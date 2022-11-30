@file:Suppress("unused")

package backend

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.data.annotation.*
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.*
import javax.validation.constraints.NotNull
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size
import javax.validation.constraints.Email as EmailConstraint


/*=================================================================================*/
interface AuthorityRecord : Persistable<String> {
    val role: String
    override fun getId() = role
    override fun isNew() = true

    companion object {
        const val ROLE_COLUMN = "role"
    }
}


/*=================================================================================*/
@Table("`authority`")
data class AuthorityEntity(
    @Id
    @field:NotNull
    @field:Size(max = 50)
    @Column(AuthorityRecord.ROLE_COLUMN)
    override val role: String
) : AuthorityRecord



/*=================================================================================*/
@Table("`phone`")
data class PhoneEntity(
    @Id var id: UUID? = null,
    @field:NotNull
    @field:Pattern(regexp = Constants.LOGIN_REGEX)
    @field:Size(min = 1, max = 50)
    var value: String? = null
)


/*=================================================================================*/
@Table("`country_phone_code`")
data class CountryPhoneCodeEntity(
    @Id val code: String,
    val countryCode: String
) : Persistable<String> {
    override fun getId() = code
    override fun isNew() = true
}
/*=================================================================================*/
@Table("`email`")
data class EmailEntity(@Id val value: @EmailConstraint String) : Persistable<String> {
    override fun getId() = value
    override fun isNew() = true
}
/*=================================================================================*/
@Table("`user_authority`")
data class AccountAuthorityEntity(
    @Id var id: Long? = null,
    @field:NotNull
    val userId: UUID,
    @field:NotNull
    val role: String
)
/*=================================================================================*/
interface AccountRecord<AUTH : AuthorityRecord> {
    var id: UUID?
    var login: String?
    var password: String?
    var firstName: String?
    var lastName: String?
    var email: String?
    var activated: Boolean
    var langKey: String?
    var imageUrl: String?
    var activationKey: String?
    var resetKey: String?
    var resetDate: Instant?
    var authorities: MutableSet<AUTH>?
    var createdBy: String?
    var createdDate: Instant?
    var lastModifiedBy: String?
    var lastModifiedDate: Instant?
    val toModel: Account
        get() = Account(
            id = id,
            login = login,
            firstName = firstName,
            lastName = lastName,
            email = email,
            imageUrl = imageUrl,
            activated = activated,
            langKey = langKey,
            createdBy = createdBy,
            createdDate = createdDate,
            lastModifiedBy = lastModifiedBy,
            lastModifiedDate = lastModifiedDate,
            authorities = authorities?.map { it.role }?.toSet()
        )

    val toCredentialsModel: AccountCredentials
        get() = AccountCredentials(
            id = id,
            login = login,
            firstName = firstName,
            lastName = lastName,
            email = email,
            password = password,
            activationKey = activationKey,
            imageUrl = imageUrl,
            activated = activated,
            langKey = langKey,
            createdBy = createdBy,
            createdDate = createdDate,
            lastModifiedBy = lastModifiedBy,
            lastModifiedDate = lastModifiedDate,
            authorities = authorities?.map { it.role }?.toSet()
        )
}
/*=================================================================================*/
@Table("`user`")
data class AccountEntity @JvmOverloads constructor(
    @Id override var id: UUID? = null,

    @field:NotNull
    @field:Pattern(regexp = Constants.LOGIN_REGEX)
    @field:Size(min = 1, max = 50)
    override var login: String? = null,

    @JsonIgnore @Column("password_hash")
    @field:NotNull
    @field:Size(min = 60, max = 60)
    override var password: String? = null,

    @field:Size(max = 50)
    override var firstName: String? = null,
    @field:Size(max = 50)
    override var lastName: String? = null,

    @field:EmailConstraint
    @field:Size(min = 5, max = 254)
    override var email: String? = null,

    @field:NotNull
    override var activated: Boolean = false,

    @field:Size(min = 2, max = 10)
    override var langKey: String? = null,

    @field:Size(max = 256)
    override var imageUrl: String? = null,

    @JsonIgnore
    @field:Size(max = 20)
    override var activationKey: String? = null,

    @JsonIgnore
    @field:Size(max = 20)
    override var resetKey: String? = null,

    override var resetDate: Instant? = null,

    @JsonIgnore @Transient
    override var authorities: MutableSet<AuthorityEntity>? = mutableSetOf(),

    @JsonIgnore
    override var createdBy: String? = null,

    @JsonIgnore @CreatedDate
    override var createdDate: Instant? = Instant.now(),

    @JsonIgnore
    override var lastModifiedBy: String? = null,

    @JsonIgnore @LastModifiedDate
    override var lastModifiedDate: Instant? = Instant.now(),

    @Version @JsonIgnore var version: Long? = null
) : AccountRecord<AuthorityEntity> {
    @PersistenceCreator
    constructor(
        id: UUID?,
        login: String?,
        password: String?,
        firstName: String?,
        lastName: String?,
        email: String?,
        activated: Boolean,
        langKey: String?,
        imageUrl: String?,
        activationKey: String?,
        resetKey: String?,
        resetDate: Instant?,
        createdBy: String?,
        createdDate: Instant?,
        lastModifiedBy: String?,
        lastModifiedDate: Instant?
    ) : this(
        id,
        login,
        password,
        firstName,
        lastName,
        email,
        activated,
        langKey,
        imageUrl,
        activationKey,
        resetKey,
        resetDate,
        mutableSetOf(),
        createdBy,
        createdDate,
        lastModifiedBy,
        lastModifiedDate
    )
    constructor(model: AccountCredentials) : this() {
        id = model.id
        login = model.login
        email = model.email
        firstName = model.firstName
        lastName = model.lastName
        langKey = model.langKey
        activated = model.activated
        createdBy = model.createdBy
        createdDate = model.createdDate
        lastModifiedBy = model.lastModifiedBy
        lastModifiedDate = model.lastModifiedDate
        imageUrl = model.imageUrl
        authorities = model.authorities?.map { AuthorityEntity(it) }?.toMutableSet()
        password = model.password
        activationKey = model.activationKey
    }
}

/*=================================================================================*/
