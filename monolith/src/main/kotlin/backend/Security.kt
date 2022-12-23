@file:Suppress("unused")

package backend


import backend.Constants.AUTHORITIES_KEY
import backend.Constants.INVALID_TOKEN
import backend.Constants.VALID_TOKEN
import backend.Log.log
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.Jwts.parserBuilder
import io.jsonwebtoken.SignatureAlgorithm.HS512
import io.jsonwebtoken.io.Decoders.BASE64
import io.jsonwebtoken.jackson.io.JacksonSerializer
import io.jsonwebtoken.lang.Strings.hasLength
import io.jsonwebtoken.security.Keys.hmacShaKeyFor
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.hibernate.validator.internal.constraintvalidators.hv.EmailValidator
import org.springframework.beans.factory.InitializingBean
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.security.Key
import java.util.*
import kotlin.text.Charsets.UTF_8
import org.springframework.security.core.userdetails.User as UserSecurity



@Component("userDetailsService")
class DomainUserDetailsService(
    private val accountRepository: AccountRepository
) : ReactiveUserDetailsService {

    @Transactional
    override fun findByUsername(login: String): Mono<UserDetails> = Log.log
        .debug("Authenticating $login").run {
            return if (EmailValidator().isValid(login, null)) mono {
                accountRepository.findOneByEmailWithAuthorities(login).apply {
                    if (this == null) throw UsernameNotFoundException(
                        "User with email $login was not found in the database"
                    )
                }
            }.map { createSpringSecurityUser(login, it) }
            else mono {
                accountRepository.findOneByLoginWithAuthorities(login).apply {
                    if (this == null) throw UsernameNotFoundException(
                        "User $login was not found in the database"
                    )
                }
            }.map { createSpringSecurityUser(login, it) }
        }


    private fun createSpringSecurityUser(
        lowercaseLogin: String,
        account: AccountCredentials
    ): UserSecurity = if (!account.activated) {
        throw UserNotActivatedException(
            "User $lowercaseLogin was not activated"
        )
    } else UserSecurity(
        account.login!!,
        account.password!!,
        account.authorities!!.map {
            SimpleGrantedAuthority(it)
        }
    )
}

@Component
class TokenProvider(
    private val properties: ApplicationProperties
) : InitializingBean {

    private var key: Key? = null
    private var tokenValidityInMilliseconds: Long = 0
    private var tokenValidityInMillisecondsForRememberMe: Long = 0

    @Throws(Exception::class)
    override fun afterPropertiesSet() {
        properties
            .security
            .authentication
            .jwt
            .secret!!
            .apply {
                key = hmacShaKeyFor(
                    if (!hasLength(this))
                        log.warn(
                            "Warning: the Jwt key used is not Base64-encoded. " +
                                    "We recommend using the `backend.security.authentication.jwt.base64-secret`" +
                                    " key for optimum security."
                        ).run { toByteArray(UTF_8) }
                    else log.debug("Using a Base64-encoded Jwt secret key").run {
                        BASE64.decode(
                            properties
                                .security
                                .authentication
                                .jwt
                                .base64Secret
                        )
                    }
                )
            }
        tokenValidityInMilliseconds = properties
            .security
            .authentication
            .jwt
            .tokenValidityInSeconds * 1000
        tokenValidityInMillisecondsForRememberMe = properties
            .security
            .authentication
            .jwt
            .tokenValidityInSecondsForRememberMe * 1000
    }

    suspend fun createToken(
        authentication: Authentication,
        rememberMe: Boolean
    ): String {
        Date().time.apply {
            return@createToken Jwts.builder()
                .setSubject(authentication.name)
                .claim(
                    AUTHORITIES_KEY,
                    authentication.authorities
                        .asSequence()
                        .map { it.authority }
                        .joinToString(separator = ","))
                .signWith(key, HS512)
                .setExpiration(
                    if (rememberMe) Date(this + tokenValidityInMillisecondsForRememberMe)
                    else Date(this + tokenValidityInMilliseconds)
                )
                .serializeToJsonWith(JacksonSerializer())
                .compact()
        }
    }

    fun getAuthentication(token: String): Authentication {
        parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
            .apply {
                this[AUTHORITIES_KEY]
                    .toString()
                    .splitToSequence(",")
                    .mapTo(mutableListOf()) { SimpleGrantedAuthority(it) }
                    .apply authorities@{
                        return@getAuthentication UsernamePasswordAuthenticationToken(
                            UserSecurity(subject, "", this@authorities),
                            token,
                            this@authorities
                        )
                    }
            }
    }

    fun validateToken(token: String): Boolean {
        try {
            parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
            return VALID_TOKEN
        } catch (e: JwtException) {
            log.info("Invalid Jwt token.")
            log.trace("Invalid Jwt token trace. $e")
        } catch (e: IllegalArgumentException) {
            log.info("Invalid Jwt token.")
            log.trace("Invalid Jwt token trace. $e")
        }
        return INVALID_TOKEN
    }
}
/*=================================================================================*/

@Component("jwtFilter")
class JwtFilter(private val tokenProvider: TokenProvider) : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        resolveToken(exchange.request).apply token@{
            chain.apply {
                return if (!isNullOrBlank() &&
                    tokenProvider.validateToken(token = this@token)
                ) filter(exchange)
                    .contextWrite(
                        ReactiveSecurityContextHolder.withAuthentication(
                            tokenProvider.getAuthentication(token = this@token)
                        )
                    )
                else filter(exchange)
            }
        }
    }

    private fun resolveToken(request: ServerHttpRequest): String? = request
        .headers
        .getFirst(Constants.AUTHORIZATION_HEADER)
        .apply {
            return if (
                !isNullOrBlank() &&
                startsWith(Constants.BEARER_START_WITH)
            ) substring(startIndex = 7)
            else null
        }

}


/*=================================================================================*/

object SecurityUtils {

    suspend fun getCurrentUserLogin(): String =
        extractPrincipal(
            ReactiveSecurityContextHolder.getContext()
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
        ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .filter { it.credentials is String }
            .map { it.credentials as String }
            .awaitSingle()

    suspend fun isAuthenticated(): Boolean =
        ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .map(Authentication::getAuthorities)
            .map { roles: Collection<GrantedAuthority> ->
                roles.map(transform = GrantedAuthority::getAuthority)
                    .none { it == Constants.ROLE_ANONYMOUS }
            }.awaitSingle()


    suspend fun isCurrentUserInRole(authority: String): Boolean =
        ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .map(Authentication::getAuthorities)
            .map { roles: Collection<GrantedAuthority> ->
                roles.map(transform = GrantedAuthority::getAuthority)
                    .any { it == authority }
            }.awaitSingle()
}

/*=================================================================================*/

