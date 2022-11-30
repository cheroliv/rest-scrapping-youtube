@file:Suppress("unused")

package backend

import backend.Constants.ROLE_ANONYMOUS
import backend.Log.log
import org.apache.commons.lang3.RandomStringUtils.random
import org.springframework.data.domain.Page
import org.springframework.http.HttpHeaders
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.util.UriComponentsBuilder
import java.io.UnsupportedEncodingException
import java.net.URLEncoder.encode
import java.security.SecureRandom
import java.text.MessageFormat
import kotlin.text.Charsets.UTF_8
import org.springframework.security.core.context.ReactiveSecurityContextHolder.getContext
import kotlinx.coroutines.reactive.awaitSingle



/*=================================================================================*/
object RandomUtils {
    private const val DEF_COUNT = 20
    private val SECURE_RANDOM: SecureRandom by lazy {
        SecureRandom().apply { nextBytes(ByteArray(size = 64)) }
    }

    private val generateRandomAlphanumericString: String
        get() = random(
            DEF_COUNT,
            0,
            0,
            true,
            true,
            null,
            SECURE_RANDOM
        )

    val generatePassword: String
        get() = generateRandomAlphanumericString

    val generateActivationKey: String
        get() = generateRandomAlphanumericString

    val generateResetKey: String
        get() = generateRandomAlphanumericString
}
/*=================================================================================*/

/**
 * Utility class for HTTP headers creation.
 */
object HttpHeaderUtil {

    /**
     *
     * createAlert.
     *
     * @param applicationName a [java.lang.String] object.
     * @param message a [java.lang.String] object.
     * @param param a [java.lang.String] object.
     * @return a [org.springframework.http.HttpHeaders] object.
     */
    private fun createAlert(
        applicationName: String,
        message: String?,
        param: String?
    ): HttpHeaders = HttpHeaders().apply {
        add("X-$applicationName-alert", message)
        try {
            add("X-$applicationName-params", encode(param, UTF_8))
        } catch (_: UnsupportedEncodingException) {
            // StandardCharsets are supported by every Java implementation so this exceptions will never happen
        }
    }

    /**
     *
     * createEntityCreationAlert.
     *
     * @param applicationName a [java.lang.String] object.
     * @param enableTranslation a boolean.
     * @param entityName a [java.lang.String] object.
     * @param param a [java.lang.String] object.
     * @return a [org.springframework.http.HttpHeaders] object.
     */
    fun createEntityCreationAlert(
        applicationName: String,
        enableTranslation: Boolean,
        entityName: String,
        param: String
    ): HttpHeaders = createAlert(
        applicationName,
        when {
            enableTranslation -> "$applicationName.$entityName.created"
            else -> "A new $entityName is created with identifier $param"
        },
        param
    )

    /**
     *
     * createEntityUpdateAlert.
     *
     * @param applicationName a [java.lang.String] object.
     * @param enableTranslation a boolean.
     * @param entityName a [java.lang.String] object.
     * @param param a [java.lang.String] object.
     * @return a [org.springframework.http.HttpHeaders] object.
     */
    fun createEntityUpdateAlert(
        applicationName: String,
        enableTranslation: Boolean,
        entityName: String,
        param: String
    ): HttpHeaders = createAlert(
        applicationName,
        when {
            enableTranslation -> "$applicationName.$entityName.updated"
            else -> "A $entityName is updated with identifier $param"
        },
        param
    )

    /**
     *
     * createEntityDeletionAlert.
     *
     * @param applicationName a [java.lang.String] object.
     * @param enableTranslation a boolean.
     * @param entityName a [java.lang.String] object.
     * @param param a [java.lang.String] object.
     * @return a [org.springframework.http.HttpHeaders] object.
     */
    fun createEntityDeletionAlert(
        applicationName: String,
        enableTranslation: Boolean,
        entityName: String,
        param: String
    ): HttpHeaders = createAlert(
        applicationName,
        when {
            enableTranslation -> "$applicationName.$entityName.deleted"
            else -> "A $entityName is deleted with identifier $param"
        },
        param
    )

    /**
     *
     * createFailureAlert.
     *
     * @param applicationName a [java.lang.String] object.
     * @param enableTranslation a boolean.
     * @param entityName a [java.lang.String] object.
     * @param errorKey a [java.lang.String] object.
     * @param defaultMessage a [java.lang.String] object.
     * @return a [org.springframework.http.HttpHeaders] object.
     */
    fun createFailureAlert(
        applicationName: String,
        enableTranslation: Boolean,
        entityName: String?,
        errorKey: String,
        defaultMessage: String?
    ): HttpHeaders = log.error(
        "Entity processing failed, {}",
        defaultMessage
    ).run {
        return@run HttpHeaders().apply {
            add(
                "X-$applicationName-error",
                if (enableTranslation) "error.$errorKey"
                else defaultMessage!!
            )
            add("X-$applicationName-params", entityName)
        }
    }
}
/*=================================================================================*/
/**
 * Utility class for handling pagination.
 *
 *
 *
 * Pagination uses the same principles as the [GitHub API](https://developer.github.com/v3/#pagination),
 * and follow [RFC 5988 (Link header)](http://tools.ietf.org/html/rfc5988).
 */
object PaginationUtil {
    private const val HEADER_X_TOTAL_COUNT = "X-Total-Count"
    private const val HEADER_LINK_FORMAT = "<{0}>; rel=\"{1}\""

    /**
     * Generate pagination headers for a Spring Data [org.springframework.data.domain.Page] object.
     *
     * @param uriBuilder The URI builder.
     * @param page The page.
     * @param <T> The type of object.
     * @return http header.
    </T> */
    fun <T> generatePaginationHttpHeaders(uriBuilder: UriComponentsBuilder, page: Page<T>): HttpHeaders {
        val headers = HttpHeaders()
        headers.add(HEADER_X_TOTAL_COUNT, page.totalElements.toString())
        val pageNumber = page.number
        val pageSize = page.size
        val link = StringBuilder()
        if (pageNumber < page.totalPages - 1) {
            link.append(
                prepareLink(
                    uriBuilder = uriBuilder,
                    pageNumber = pageNumber + 1,
                    pageSize = pageSize,
                    relType = "next"
                )
            ).append(",")
        }
        if (pageNumber > 0) {
            link.append(prepareLink(uriBuilder, pageNumber - 1, pageSize, "prev"))
                .append(",")
        }
        link.append(prepareLink(uriBuilder, page.totalPages - 1, pageSize, "last"))
            .append(",")
            .append(prepareLink(uriBuilder, 0, pageSize, "first"))
        headers.add(HttpHeaders.LINK, link.toString())
        return headers
    }

    private fun prepareLink(
        uriBuilder: UriComponentsBuilder,
        pageNumber: Int,
        pageSize: Int,
        relType: String
    ): String = MessageFormat.format(
        HEADER_LINK_FORMAT,
        preparePageUri(
            uriBuilder = uriBuilder,
            pageNumber = pageNumber,
            pageSize = pageSize
        ),
        relType
    )

    private fun preparePageUri(
        uriBuilder: UriComponentsBuilder,
        pageNumber: Int,
        pageSize: Int
    ): String = uriBuilder.replaceQueryParam(
        "page",
        pageNumber.toString()
    ).replaceQueryParam(
        "size",
        pageSize.toString()
    ).toUriString()
        .replace(oldValue = ",", newValue = "%2C")
        .replace(oldValue = ";", newValue = "%3B")
}
/*=================================================================================*/
object SecurityUtils {

    suspend fun getCurrentUserLogin(): String =
        extractPrincipal(
            getContext()
                .awaitSingle()
                .authentication
        )

    private fun extractPrincipal(authentication: Authentication?): String =
        if (authentication == null) ""
        else when (val principal = authentication.principal) {
            is UserDetails -> principal.username
            is String -> principal
            else -> ""
        }

    suspend fun getCurrentUserJwt(): String =
        getContext()
            .map(SecurityContext::getAuthentication)
            .filter { it.credentials is String }
            .map { it.credentials as String }
            .awaitSingle()

    suspend fun isAuthenticated(): Boolean =
        getContext()
            .map(SecurityContext::getAuthentication)
            .map(Authentication::getAuthorities)
            .map { roles: Collection<GrantedAuthority> ->
                roles.map(transform = GrantedAuthority::getAuthority)
                    .none { it == ROLE_ANONYMOUS }
            }.awaitSingle()


    suspend fun isCurrentUserInRole(authority: String): Boolean =
        getContext()
            .map(SecurityContext::getAuthentication)
            .map(Authentication::getAuthorities)
            .map { roles: Collection<GrantedAuthority> ->
                roles.map(transform = GrantedAuthority::getAuthority)
                    .any { it == authority }
            }.awaitSingle()
}

/*=================================================================================*/
