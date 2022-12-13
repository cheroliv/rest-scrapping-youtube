package backend

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.time.Instant
import javax.validation.Valid

/*=================================================================================*/
@RestController
@RequestMapping(Constants.ACCOUNT_API)
class SignupController(
    private val signupService: SignupService
) {
    internal class SignupException(message: String) : RuntimeException(message)

    /**
     * {@code POST  /signup} : register the user.
     *
     * @param accountCredentials the managed user View Model.
     * @throws backend.InvalidPasswordException {@code 400 (Bad Request)} if the password is incorrect.
     * @throws backend.EmailAlreadyUsedProblem {@code 400 (Bad Request)} if the email is already used.
     * @throws backend.LoginAlreadyUsedProblem {@code 400 (Bad Request)} if the login is already used.
     */
    @PostMapping(Constants.SIGNUP_API)
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun signup(
        @RequestBody @Valid accountCredentials: AccountCredentials
    ) = signupService.signup(accountCredentials)

    /**
     * `GET  /activate` : activate the signed-up user.
     *
     * @param key the activation key.
     * @throws RuntimeException `500 (Internal BackendApplication Error)` if the user couldn't be activated.
     */
    @GetMapping(Constants.ACTIVATE_API)
    suspend fun activateAccount(@RequestParam(value = Constants.ACTIVATE_API_KEY) key: String) {
        when {
            !signupService.activate(key) -> throw SignupException("No user was found for this activation key")
        }
    }
}
/*=================================================================================*/
@Service
@Transactional
class SignupService(
    private val accountRepository: AccountRepository,
    private val mailService: MailService
) {

    @Throws(
        InvalidPasswordException::class,
        UsernameAlreadyUsedException::class,
        UsernameAlreadyUsedException::class
    )
    suspend fun signup(account: AccountCredentials) {
        InvalidPasswordException().run {
            if (isPasswordLengthInvalid(account.password)) throw this
        }
        loginValidation(account)
        emailValidation(account)
        val createdDate = Instant.now()
        account.copy(
            //TODO: hash password
            activationKey = RandomUtils.generateActivationKey,
            authorities = setOf(Constants.ROLE_USER),
            langKey = when {
                account.langKey.isNullOrBlank() -> Constants.DEFAULT_LANGUAGE
                else -> account.langKey
            },
            createdBy = Constants.SYSTEM_USER,
            createdDate = createdDate,
            lastModifiedBy = Constants.SYSTEM_USER,
            lastModifiedDate = createdDate,
            activated = false
        ).run {
            accountRepository.signup(this)
            mailService.sendActivationEmail(this)
        }
    }

    @Throws(UsernameAlreadyUsedException::class)
    private suspend fun loginValidation(model: AccountCredentials) {
        accountRepository.findOneByLogin(model.login!!).run {
            if (this != null) when {
                !activated -> accountRepository.suppress(this.toAccount())
                else -> throw UsernameAlreadyUsedException()
            }
        }
    }

    @Throws(UsernameAlreadyUsedException::class)
    private suspend fun emailValidation(model: AccountCredentials) {
        accountRepository.findOneByEmail(model.email!!).run {
            if (this != null) {
                when {
                    !activated -> accountRepository.suppress(toAccount())
                    else -> throw EmailAlreadyUsedException()
                }
            }
        }
    }

    suspend fun activate(key: String): Boolean {
        accountRepository.run {
            with(findOneByActivationKey(key)) {
                return when {
                    this == null -> false
                    else -> {
                        save(
                            copy(
                                activated = true, activationKey = null
                            )
                        ).apply {
                            if (id != null) Log.log.info("activation: $login")
                        }
                        true
                    }
                }
            }
        }
    }
}
/*=================================================================================*/
