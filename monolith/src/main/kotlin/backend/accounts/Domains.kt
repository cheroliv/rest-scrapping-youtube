@file:Suppress("unused")

package backend.accounts

import backend.Constants.IMAGE_URL_DEFAULT
import backend.Constants.LOGIN_REGEX
import backend.Constants.PASSWORD_MAX_LENGTH
import backend.Constants.PASSWORD_MIN_LENGTH
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.apache.commons.lang3.RandomStringUtils
import java.security.SecureRandom
import java.time.Instant
import java.util.*
import jakarta.validation.constraints.Email as EmailConstraints

/*=================================================================================*/
/**
 * Représente l'account domain model sans le password
 */
//TODO: add field enabled=false
data class Account(
    val id: UUID? = null,
    @field:NotBlank
    @field:Pattern(regexp = LOGIN_REGEX)
    @field:Size(min = 1, max = 50)
    val login: String? = null,
    @field:Size(max = 50)
    val firstName: String? = null,
    @field:Size(max = 50)
    val lastName: String? = null,
    @field:EmailConstraints
    @field:Size(min = 5, max = 254)
    val email: String? = null,
    @field:Size(max = 256)
    val imageUrl: String? = IMAGE_URL_DEFAULT,
    val activated: Boolean = false,
    @field:Size(min = 2, max = 10)
    val langKey: String? = null,
    val createdBy: String? = null,
    val createdDate: Instant? = null,
    val lastModifiedBy: String? = null,
    val lastModifiedDate: Instant? = null,
    val authorities: Set<String>? = null
) {
    fun isActivated(): Boolean = activated
}
/*=================================================================================*/
/**
 * Représente l'account domain model avec le password et l'activationKey
 * pour la vue
 */
data class AccountCredentials(
    @field:NotNull
    @field:Size(
        min = PASSWORD_MIN_LENGTH,
        max = PASSWORD_MAX_LENGTH
    )
    val password: String? = null,
    val activationKey: String? = null,
    val resetKey: String? = null,
    val id: UUID? = null,
    @field:NotBlank
    @field:Pattern(regexp = LOGIN_REGEX)
    @field:Size(min = 1, max = 50)
    val login: String? = null,
    @field:Size(max = 50)
    val firstName: String? = null,
    @field:Size(max = 50)
    val lastName: String? = null,
    @field:EmailConstraints
    @field:Size(min = 5, max = 254)
    val email: String? = null,
    @field:Size(max = 256)
    val imageUrl: String? = IMAGE_URL_DEFAULT,
    val activated: Boolean = false,
    @field:Size(min = 2, max = 10)
    val langKey: String? = null,
    val createdBy: String? = null,
    val createdDate: Instant? = null,
    val lastModifiedBy: String? = null,
    val lastModifiedDate: Instant? = null,
    val authorities: Set<String>? = null
) {

    constructor(account: Account) : this(
        id = account.id,
        login = account.login,
        email = account.email,
        firstName = account.firstName,
        lastName = account.lastName,
        langKey = account.langKey,
        activated = account.activated,
        createdBy = account.createdBy,
        createdDate = account.createdDate,
        lastModifiedBy = account.lastModifiedBy,
        lastModifiedDate = account.lastModifiedDate,
        imageUrl = account.imageUrl,
        authorities = account.authorities?.map { it }?.toMutableSet()
    )


    fun toAccount(): Account = Account(
        id = id,
        login = login,
        firstName = firstName,
        lastName = lastName,
        email = email,
        activated = activated,
        langKey = langKey,
        createdBy = createdBy,
        createdDate = createdDate,
        lastModifiedBy = lastModifiedBy,
        lastModifiedDate = lastModifiedDate,
        authorities = authorities
    )
}
/*=================================================================================*/
/**
 * Représente l'account domain model minimaliste pour la view
 */
data class Avatar(
    val id: UUID? = null,
    val login: String? = null
)
/*=================================================================================*/

data class KeyAndPassword(
    val key: String? = null,
    val newPassword: String? = null
)

/*=================================================================================*/
data class Login(
    @field:NotNull
    val username:
    @Size(min = 1, max = 50)
    String? = null,
    @field:NotNull
    @field:Size(min = 4, max = 100)
    val password:
    String? = null,
    val rememberMe: Boolean? = null
)

/*=================================================================================*/
data class PasswordChange(
    val currentPassword: String? = null,
    val newPassword: String? = null
)

/*=================================================================================*/
object RandomUtils {
    private const val DEF_COUNT = 20
    private val SECURE_RANDOM: SecureRandom by lazy {
        SecureRandom().apply { nextBytes(ByteArray(size = 64)) }
    }

    private val generateRandomAlphanumericString: String
        get() = RandomStringUtils.random(
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
