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
import org.springframework.web.server.i18n.LocaleContextResolver
import webapp.Constants.ACCOUNT_API
import webapp.Constants.ACTIVATE_API
import webapp.Constants.ACTIVATE_API_KEY
import webapp.Constants.DEFAULT_LANGUAGE
import webapp.Constants.MSG_WRONG_ACTIVATION_KEY
import webapp.Constants.ROLE_USER
import webapp.Constants.SIGNUP_API
import webapp.Constants.SYSTEM_USER
import webapp.Logging.i
import webapp.accounts.exceptions.EmailAlreadyUsedException
import webapp.accounts.exceptions.InvalidPasswordException
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
    private val request: LocaleContextResolver
) {
    internal class SignupException(message: String) : RuntimeException(message)
//    @ExceptionHandler(ConstraintViolationException::class)
//    fun handleConstraintViolationException(
//        cve: ConstraintViolationException,
//        req: WebRequest
//    ): ResponseEntity<ProblemDetail> = badRequest().build<ProblemDetail?>().apply {
//        i(messageSource!!.getMessage(cve.constraintViolations.first().messageTemplate, null, ENGLISH))
//        i("passé par ici: ${cve.message}")
//    }
//
//    @ExceptionHandler(UsernameAlreadyUsedException::class)
//    suspend fun handleUsernameAlreadyUsedException(
//        ex: UsernameAlreadyUsedException,
//        request: ServerWebExchange
//    ): ResponseEntity<ProblemDetail> {
//        val problem = LoginAlreadyUsedProblem()
//        return create(
//            problem,
//            request,
//            createFailureAlert(
//                applicationName = properties.clientApp.name,
//                enableTranslation = true,
//                entityName = problem.entityName,
//                errorKey = problem.errorKey,
//                defaultMessage = problem.message
//            )
//        )
//    }

    //    @ExceptionHandler
//    fun handleEmailAlreadyUsedException(
//        ex: EmailAlreadyUsedException,
//        request: ServerWebExchange
//    ): Mono<ResponseEntity<Problem>> {
//        val problem = EmailAlreadyUsedProblem()
//        return create(
//            problem,
//            request,
//            createFailureAlert(
//                applicationName = properties.clientApp.name,
//                enableTranslation = true,
//                entityName = problem.entityName,
//                errorKey = problem.errorKey,
//                defaultMessage = problem.message
//            )
//        )
//    }
    /**
     * {@code POST  /signup} : register the user.
     *
     * @param account the managed user View Model.
     * @throws webapp.InvalidPasswordException {@code 400 (Bad Request)} if the password is incorrect.
     * @throws webapp.EmailAlreadyUsedProblem {@code 400 (Bad Request)} if the email is already used.
     * @throws webapp.LoginAlreadyUsedProblem {@code 400 (Bad Request)} if the login is already used.
     */
    @PostMapping(SIGNUP_API, produces = [APPLICATION_PROBLEM_JSON_VALUE])
    @ResponseStatus(CREATED)
    @Transactional
    suspend fun signup(@RequestBody account: AccountCredentials) = account.run {
        /*
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
    @field:Email
    @field:Size(min = 5, max = 254)
    val email: String? = null,
    @field:Size(max = 256)
    val imageUrl: String? = IMAGE_URL_DEFAULT,
    val activated: Boolean = false,
    @field:Size(min = 2, max = 10)
    val langKey: String? = null,
         */

        when {
            validator.validateProperty(account, "password").isNotEmpty() -> return badRequest().body<ProblemDetail>(
                forStatusAndDetail(
                    BAD_REQUEST,
                    validator.validateProperty(account, "password").first().message
                )
            )
            else -> {
                try {
                    isLoginAvailable(this)
                    isEmailAvailable(this)
                } catch (uaue: UsernameAlreadyUsedException) {
                    return badRequest().body<ProblemDetail>(
                        forStatusAndDetail(
                            BAD_REQUEST,
                            uaue.message!!
                        )
                    )
                } catch (eaue: EmailAlreadyUsedException) {
                    return badRequest().body<ProblemDetail>(
                        forStatusAndDetail(
                            BAD_REQUEST,
                            eaue.message!!
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
        }

    }

    /**
     * `GET  /activate` : activate the signed-up user.
     *
     * @param key the activation key.
     * @throws RuntimeException `500 (Internal Application Error)` if the user couldn't be activated.
     */
    @GetMapping(ACTIVATE_API)
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