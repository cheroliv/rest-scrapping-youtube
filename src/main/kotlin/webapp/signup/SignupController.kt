package webapp.signup

import jakarta.validation.Valid
import jakarta.validation.Validator
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
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
import webapp.accounts.exceptions.EmailAlreadyUsedException
import webapp.accounts.exceptions.InvalidPasswordException
import webapp.accounts.exceptions.UsernameAlreadyUsedException
import webapp.accounts.models.AccountCredentials
import webapp.accounts.models.AccountUtils.generateActivationKey
import webapp.accounts.repository.AccountRepository
import webapp.mail.MailService
import java.time.Instant.now

@RestController
@RequestMapping(ACCOUNT_API)
class SignupController(
    private val accountRepository: AccountRepository,
    private val mailService: MailService,
    private val passwordEncoder: PasswordEncoder,
    private val validator: Validator,

    ) {
    internal class SignupException(message: String) : RuntimeException(message)

    /**
     * {@code POST  /signup} : register the user.
     *
     * @param account the managed user View Model.
     * @throws webapp.InvalidPasswordException {@code 400 (Bad Request)} if the password is incorrect.
     * @throws webapp.EmailAlreadyUsedProblem {@code 400 (Bad Request)} if the email is already used.
     * @throws webapp.LoginAlreadyUsedProblem {@code 400 (Bad Request)} if the login is already used.
     */
    @PostMapping(SIGNUP_API, produces = [APPLICATION_PROBLEM_JSON_VALUE])
    @Throws(
        UsernameAlreadyUsedException::class,
        EmailAlreadyUsedException::class
    )
    @ResponseStatus(CREATED)
    @Transactional
    suspend fun signup(
        @Valid @RequestBody
        account: AccountCredentials
    ): ResponseEntity<ProblemDetail> = account.run {
        isLoginAvailable(this)
        isEmailAvailable(this)
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
        ResponseEntity(CREATED)
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