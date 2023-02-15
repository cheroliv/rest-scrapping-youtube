package webapp.signup

import jakarta.validation.Validation.byProvider
import org.hibernate.validator.HibernateValidator
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE
import org.springframework.http.ProblemDetail
import org.springframework.http.ProblemDetail.forStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.badRequest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange
import webapp.Constants.ACCOUNT_API
import webapp.Constants.ACTIVATE_API
import webapp.Constants.ACTIVATE_API_KEY
import webapp.Constants.MSG_WRONG_ACTIVATION_KEY
import webapp.Constants.ROLE_USER
import webapp.Constants.SIGNUP_API
import webapp.Constants.SYSTEM_USER
import webapp.Logging.i
import webapp.ProblemsModel
import webapp.accounts.entities.AccountRecord.Companion.EMAIL_FIELD
import webapp.accounts.entities.AccountRecord.Companion.FIRST_NAME_FIELD
import webapp.accounts.entities.AccountRecord.Companion.LAST_NAME_FIELD
import webapp.accounts.entities.AccountRecord.Companion.LOGIN_FIELD
import webapp.accounts.entities.AccountRecord.Companion.PASSWORD_FIELD
import webapp.accounts.exceptions.EmailAlreadyUsedException
import webapp.accounts.exceptions.UsernameAlreadyUsedException
import webapp.accounts.models.AccountCredentials
import webapp.accounts.models.AccountCredentials.Companion.objectName
import webapp.accounts.models.AccountUtils.generateActivationKey
import webapp.accounts.repository.AccountRepository
import webapp.mail.MailService
import java.net.URI
import java.time.Instant.now
import java.util.*
import java.util.Locale.*

@RestController
@RequestMapping(ACCOUNT_API)
class SignupController(
    private val accountRepository: AccountRepository,
    private val mailService: MailService,
    private val passwordEncoder: PasswordEncoder,
) {
    companion object {
        val signupFields
            get() = setOf(
                PASSWORD_FIELD,
                EMAIL_FIELD,
                LOGIN_FIELD,
                FIRST_NAME_FIELD,
                LAST_NAME_FIELD
            )
    }

    internal class SignupException(message: String) : RuntimeException(message)

    /**
     * {@code POST  /signup} : register the user.
     *
     * @param account the managed user View Model.
     */
    @PostMapping(SIGNUP_API, produces = [APPLICATION_PROBLEM_JSON_VALUE])
    @ResponseStatus(CREATED)
    @Transactional
    suspend fun signup(@RequestBody account: AccountCredentials, exchange: ServerWebExchange) = account.run acc@{
        ProblemsModel(
            type = "https://www.cheroliv.com/problem/constraint-violation",
            title = "Data binding and validation failure",
            path = "$ACCOUNT_API$SIGNUP_API",
            message = "error.validation",
            status = BAD_REQUEST.value(),
        ).run pm@{
            byProvider(HibernateValidator::class.java)
                .configure()
                .localeResolver {
                    try {
                        of(
                            exchange
                                .request
                                .headers
                                .acceptLanguage
                                .first()
                                .range
                        )
                    } catch (e: Exception) {
                        ENGLISH
                    }
                }
                .buildValidatorFactory()
                .validator.run {
                    signupFields.map { field ->
                        field to validateProperty(this@acc, field)
                    }.forEach { violatedField ->
                        violatedField.second.forEach {
                            fieldErrors.add(
                                mapOf(
                                    "objectName" to objectName,
                                    "field" to violatedField.first,
                                    "message" to it.message
                                )
                            )
                        }
                    }
                }

            when {
                fieldErrors.isNotEmpty() -> {
                    return badRequest().body<ProblemDetail>(
                        forStatus(BAD_REQUEST).apply {
                            type = URI(this@pm.type)
                            title = this@pm.title
                            status = BAD_REQUEST.value()
                            setProperty("path", this@pm.path)
                            setProperty("message", this@pm.message)
                            setProperty("fieldErrors", this@pm.fieldErrors)
                        }
                    )
                }
            }
            try {
                isLoginAvailable(this@acc)
                isEmailAvailable(this@acc)
            } catch (e: UsernameAlreadyUsedException) {
                return badRequest().body<ProblemDetail>(
                    forStatus(BAD_REQUEST).apply {
                        type = URI(this@pm.type)
                        title = this@pm.title
                        status = BAD_REQUEST.value()
                        setProperty("path", this@pm.path)
                        setProperty("message", this@pm.message)
                        setProperty("fieldErrors", this@pm.fieldErrors.apply {
                            add(
                                mapOf(
                                    "objectName" to objectName,
                                    "field" to LOGIN_FIELD,
                                    "message" to e.message!!
                                )
                            )
                        })
                    }
                )
            } catch (e: EmailAlreadyUsedException) {
                return badRequest().body<ProblemDetail>(
                    forStatus(BAD_REQUEST).apply {
                        type = URI(this@pm.type)
                        title = this@pm.title
                        status = BAD_REQUEST.value()
                        setProperty("path", path)
                        setProperty("message", this@pm.message)
                        setProperty("fieldErrors", this@pm.fieldErrors.apply {
                            add(
                                mapOf(
                                    "objectName" to objectName,
                                    "field" to EMAIL_FIELD,
                                    "message" to e.message!!
                                )
                            )
                        })
                    }
                )
            }
        }
        now().run {
            copy(
                password = passwordEncoder.encode(password),
                activationKey = generateActivationKey,
                authorities = setOf(ROLE_USER),
                langKey = when {
                    langKey.isNullOrBlank() -> ENGLISH.language
                    else -> langKey
                },
                activated = false,
                createdBy = SYSTEM_USER,
                createdDate = this,
                lastModifiedBy = SYSTEM_USER,
                lastModifiedDate = this
            ).run {
                accountRepository.signup(this)
                mailService.sendActivationEmail(this)
            }
        }

        ResponseEntity<ProblemDetail>(CREATED)
    }

    /**
     * `GET  /activate` : activate the signed-up user.
     *
     * @param key the activation key.
     * @throws RuntimeException `500 (Internal Application Error)` if the user couldn't be activated.
     */
    @GetMapping(ACTIVATE_API)
    @Throws(SignupException::class)
    suspend fun activateAccount(@RequestParam(ACTIVATE_API_KEY) key: String) {
        if (!accountRepository.findOneByActivationKey(key).run no@{
                return@no when {
                    this == null -> false.apply { i("no activation for key: $key") }
                    else -> accountRepository
                        .save(copy(activated = true, activationKey = null))
                        .run yes@{
                            return@yes when {
                                this != null -> true.apply { i("activation: $login") }
                                else -> false
                            }
                        }
                }
            }) throw SignupException(MSG_WRONG_ACTIVATION_KEY)
    }


    @Throws(UsernameAlreadyUsedException::class)
    private suspend fun isLoginAvailable(model: AccountCredentials) {
        accountRepository.findOne(model.login!!).run {
            when {
                this != null -> if (!activated) accountRepository.delete(toAccount())
                else throw UsernameAlreadyUsedException()
            }
        }
    }

    @Throws(EmailAlreadyUsedException::class)
    private suspend fun isEmailAvailable(model: AccountCredentials) {
        accountRepository.findOne(model.email!!).run {
            when {
                this != null -> if (!activated) accountRepository.delete(toAccount())
                else throw EmailAlreadyUsedException()
            }
        }
    }
}