package backend.accounts.signup

import backend.*
import backend.Constants.ACCOUNT_API
import backend.Constants.ACTIVATE_API
import backend.Constants.ACTIVATE_API_KEY
import backend.Constants.DEFAULT_LANGUAGE
import backend.Constants.MSG_WRONG_ACTIVATION_KEY
import backend.Constants.ROLE_USER
import backend.Constants.SIGNUP_API
import backend.Constants.SYSTEM_USER
import backend.Log.log
import backend.accounts.*
import backend.accounts.RandomUtils.generateActivationKey
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatusCode
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.time.Instant.now


/*=================================================================================*/
@RestController
@RequestMapping(ACCOUNT_API)
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
    @Throws(InvalidPasswordProblem::class)
    @PostMapping(SIGNUP_API)
    @ResponseStatus(CREATED)
    suspend fun signup(
        @RequestBody @Valid accountCredentials: AccountCredentials
    ) = try {
        signupService.signup(accountCredentials)
    } catch (ipe: InvalidPasswordException) {
        throw InvalidPasswordProblem(ipe)
    }
    //        catch (eap:EmailAlreadyUsedProblem){
//
//        }


    /**
     * `GET  /activate` : activate the signed-up user.
     *
     * @param key the activation key.
     * @throws RuntimeException `500 (Internal BackendApplication Error)` if the user couldn't be activated.
     */
    @GetMapping(ACTIVATE_API)
    suspend fun activateAccount(@RequestParam(value = ACTIVATE_API_KEY) key: String) {
        when {
            !signupService.activate(key) -> throw SignupException(MSG_WRONG_ACTIVATION_KEY)
        }
    }
}

/*=================================================================================*/
@Service
@Transactional
class SignupService(
    private val accountRepository: AccountRepository,
    private val mailService: MailService,
    private val passwordEncoder: PasswordEncoder,
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
        val createdDate = now()
        account.copy(
            password = passwordEncoder.encode(account.password),
            activationKey = generateActivationKey,
            authorities = setOf(ROLE_USER),
            langKey = when {
                account.langKey.isNullOrBlank() -> DEFAULT_LANGUAGE
                else -> account.langKey
            },
            createdBy = SYSTEM_USER,
            createdDate = createdDate,
            lastModifiedBy = SYSTEM_USER,
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
                        save(copy(
                                activated = true,
                                activationKey = null
                            )).run { if (id != null) log.info("activation: $login") }
                        true
                    }
                }
            }
        }
    }
}
/*=================================================================================*/
