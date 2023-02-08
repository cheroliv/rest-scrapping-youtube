package webapp.signup

import jakarta.validation.Validator
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE
import org.springframework.http.ProblemDetail
import org.springframework.http.ProblemDetail.forStatusAndDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.badRequest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import webapp.Constants.ACCOUNT_API
import webapp.Constants.ACTIVATE_API
import webapp.Constants.ACTIVATE_API_KEY
import webapp.Constants.DEFAULT_LANGUAGE
import webapp.Constants.MSG_WRONG_ACTIVATION_KEY
import webapp.Constants.ROLE_USER
import webapp.Constants.SIGNUP_API
import webapp.Constants.SYSTEM_USER
import webapp.Logging.i
import webapp.accounts.entities.AccountRecord.Companion.EMAIL_FIELD
import webapp.accounts.entities.AccountRecord.Companion.FIRST_NAME_FIELD
import webapp.accounts.entities.AccountRecord.Companion.LAST_NAME_FIELD
import webapp.accounts.entities.AccountRecord.Companion.LOGIN_FIELD
import webapp.accounts.entities.AccountRecord.Companion.PASSWORD_FIELD
import webapp.accounts.exceptions.EmailAlreadyUsedException
import webapp.accounts.exceptions.UsernameAlreadyUsedException
import webapp.accounts.models.AccountCredentials
import webapp.accounts.models.AccountUtils.generateActivationKey
import webapp.accounts.repository.AccountRepository
import webapp.mail.MailService
import java.time.Instant.now
import java.util.*

@RestController
@RequestMapping(ACCOUNT_API)
class SignupController(
    private val accountRepository: AccountRepository,
    private val mailService: MailService,
    private val passwordEncoder: PasswordEncoder,
    private val validator: Validator,
//    private val request: LocaleContextResolver//to get accepted locales
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
    data class ProblemsModel(
        val type: String,
        val title: String,
        val status: Int,
        val path: String,
        val message: String,
        val fieldErrors: MutableSet<Map<String, String>> = mutableSetOf()
    ) {
        @Suppress("MemberVisibilityCanBePrivate")
        companion object {
            const val PROBLEM_OBJECT_NAME = "objectName"
            const val PROBLEM_FIELD = "field"
            const val PROBLEM_MESSAGE = "message"
            val detailsKeys = setOf(
                PROBLEM_OBJECT_NAME,
                PROBLEM_FIELD,
                PROBLEM_MESSAGE
            )
        }
    }
    /**
     * {@code POST  /signup} : register the user.
     *
     * @param account the managed user View Model.
     */
    @PostMapping(SIGNUP_API, produces = [APPLICATION_PROBLEM_JSON_VALUE])
    @ResponseStatus(CREATED)
    @Transactional
    suspend fun signup(@RequestBody account: AccountCredentials) = account.run {
        val problem=ProblemsModel(
            type = "https://www.cheroliv.com/problem/constraint-violation",
            title = "Data binding and validation failure",
            path = "$ACCOUNT_API/$SIGNUP_API",
            message = "error.validation",
            status = 400,
        )
        signupFields.forEach {
            validator.validateProperty(this, it).run {
//                problem.fieldErrors.add(
//                    mapOf(
//                        "objectName" to objectName,
//                        "field" to field,
//                        "message" to first().message
//                    )
//                )
                when {
                    isNotEmpty() -> return badRequest().body<ProblemDetail>(
                        forStatusAndDetail(
                            BAD_REQUEST,
                            first().message
                        )
                    )
                }
            }
        }
        try {
            isLoginAvailable(this)
            isEmailAvailable(this)
        } catch (e: UsernameAlreadyUsedException) {
            return badRequest().body<ProblemDetail>(
                forStatusAndDetail(
                    BAD_REQUEST,
                    e.message!!
                )
            )
        } catch (e: EmailAlreadyUsedException) {
            return badRequest().body<ProblemDetail>(
                forStatusAndDetail(
                    BAD_REQUEST,
                    e.message!!
                )
            )
        }
        now().run {
            copy(
                password = passwordEncoder.encode(password),
                activationKey = generateActivationKey,
                authorities = setOf(ROLE_USER),
                langKey = when {
                    langKey.isNullOrBlank() -> DEFAULT_LANGUAGE
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
/*
    suspend fun signup(@RequestBody account: AccountCredentials) = account.run acc@{
        ProblemsModel(
            type = "https://www.cheroliv.com/problem/constraint-violation",
            title = "Data binding and validation failure",
            path = "$ACCOUNT_API/$SIGNUP_API",
            message = "error.validation",
            status = 400,
        ).run {
            signupFields.forEach { field ->
                validator.validateProperty(this, field).run {
                    fieldErrors.add(
                        mapOf(
                            "objectName" to objectName,
                            "field" to field,
                            "message" to first().message
                        )
                    )


                    when {
                        isNotEmpty() -> return ResponseEntity.badRequest().body<ProblemDetail>(
                            forStatusAndDetail(
                                BAD_REQUEST,
                                first().message
                            )
                        )
                    }
                }
            }
            try {
                isLoginAvailable(this@acc)
                isEmailAvailable(this@acc)
            } catch (e: UsernameAlreadyUsedException) {

                fieldErrors.add(
                    mapOf(
                        "objectName" to objectName,
                        "field" to LOGIN_FIELD,
                        "message" to e.message!!
                    )
                )

                return ResponseEntity.badRequest().body<ProblemDetail>(
                    forStatusAndDetail(
                        BAD_REQUEST,
                        e.message!!
                    )
                )
            } catch (e: EmailAlreadyUsedException) {
                fieldErrors.add(
                    mapOf(
                        "objectName" to objectName,
                        "field" to EMAIL_FIELD,
                        "message" to e.message!!
                    )
                )
                return ResponseEntity.badRequest().body<ProblemDetail>(
                    forStatusAndDetail(
                        BAD_REQUEST,
                        e.message!!
                    )
                )
            }
        }
        now().run {
            copy(
                password = passwordEncoder.encode(password),
                activationKey = generateActivationKey,
                authorities = setOf(ROLE_USER),
                langKey = when {
                    langKey.isNullOrBlank() -> DEFAULT_LANGUAGE
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

 */
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
                return@no if (this == null) false.apply { i("no activation for key: $key") }
                else accountRepository.save(copy(activated = true, activationKey = null)).run yes@{
                    return@yes if (this != null) true.apply { i("activation: $login") } else false
                }
            }) throw SignupException(MSG_WRONG_ACTIVATION_KEY)
    }


    @Throws(UsernameAlreadyUsedException::class)
    private suspend fun isLoginAvailable(model: AccountCredentials) {
        accountRepository.findOne(model.login!!).run {
            if (this != null) if (!activated) accountRepository.delete(toAccount())
            else throw UsernameAlreadyUsedException()
        }
    }

    @Throws(EmailAlreadyUsedException::class)
    private suspend fun isEmailAvailable(model: AccountCredentials) {
        accountRepository.findOne(model.email!!).run {
            if (this != null) if (!activated) accountRepository.delete(toAccount())
            else throw EmailAlreadyUsedException()
        }
    }
}