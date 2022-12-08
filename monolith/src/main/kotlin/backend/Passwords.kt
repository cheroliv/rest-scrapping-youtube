package backend

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import javax.validation.constraints.Email

/*=================================================================================*/

@RestController
@RequestMapping(Constants.ACCOUNT_API)
class ResetPasswordController(
    private val resetPasswordService: ResetPasswordService,
    private val mailService: MailService
) {
    internal class ResetPasswordException(message: String) : RuntimeException(message)

    /**
     * {@code POST   /account/reset-password/init} : Send an email to reset the password of the user.
     *
     * @param mail the mail of the user.
     */
    @PostMapping(Constants.RESET_PASSWORD_API_INIT)
    suspend fun requestPasswordReset(@RequestBody @Email mail: String): Unit =
        with(resetPasswordService.requestPasswordReset(mail)) {
            when {
                this == null -> Log.log.warn("Password reset requested for non existing mail")
                else -> mailService.sendPasswordResetMail(this)
            }
        }

    /**
     * {@code POST   /account/reset-password/finish} : Finish to reset the password of the user.
     *
     * @param keyAndPassword the generated key and the new password.
     * @throws InvalidPasswordProblem {@code 400 (Bad Request)} if the password is incorrect.
     * @throws RuntimeException         {@code 500 (Internal BackendApplication Error)} if the password could not be reset.
     */
    @PostMapping(Constants.RESET_PASSWORD_API_FINISH)
    suspend fun finishPasswordReset(@RequestBody keyAndPassword: KeyAndPassword): Unit =
        with(InvalidPasswordException()) {
            when {
                isPasswordLengthInvalid(keyAndPassword.newPassword) -> throw this
                keyAndPassword.newPassword != null
                        && keyAndPassword.key != null
                        && resetPasswordService.completePasswordReset(
                    keyAndPassword.newPassword,
                    keyAndPassword.key
                ) == null -> throw ResetPasswordException("No user was found for this reset key")
            }
        }

}

/*=================================================================================*/
@RestController
@RequestMapping(Constants.ACCOUNT_API)
class ChangePasswordController(
    private val changePasswordService: ChangePasswordService
) {
    internal class ChangePasswordException(message: String) : RuntimeException(message)

    /**
     * {@code POST  /account/change-password} : changes the current user's password.
     *
     * @param passwordChange current and new password.
     * @throws InvalidPasswordProblem {@code 400 (Bad Request)} if the new password is incorrect.
     */
    @PostMapping(Constants.CHANGE_PASSWORD_API)
    suspend fun changePassword(@RequestBody passwordChange: PasswordChange): Unit =
        InvalidPasswordException().run {
            when {
                isPasswordLengthInvalid(passwordChange.newPassword) -> throw this

                passwordChange.currentPassword != null
                        && passwordChange.newPassword != null -> changePasswordService.changePassword(
                    passwordChange.currentPassword,
                    passwordChange.newPassword
                )
            }

        }


}

/*=================================================================================*/

@Service
@Transactional
class ResetPasswordService(
    private val accountRepository: AccountRepository
) {
    suspend fun completePasswordReset(newPassword: String, key: String): AccountCredentials? =
        accountRepository.findOneByResetKey(key).run {
            when {
                this != null && resetDate?.isAfter(
                    Instant.now().minusSeconds(86400)
                ) == true -> {
                    Log.log.debug("Reset account password for reset key $key")
                    return@completePasswordReset toCredentialsModel
                    //                return saveUser(
                    //                apply {
                    ////                    password = passwordEncoder.encode(newPassword)
                    //                    resetKey = null
                    //                    resetDate = null
                    //                })
                }

                else -> {
                    Log.log.debug("$key is not a valid reset account password key")
                    return@completePasswordReset null
                }
            }
        }


    suspend fun requestPasswordReset(mail: String): AccountCredentials? = null
//        return userRepository
//            .findOneByEmail(mail)
//            .apply {
//                if (this != null && this.activated) {
//                    resetKey = generateResetKey
//                    resetDate = now()
//                    saveUser(this)
//                } else return null
//            }
//    }

}

/*=================================================================================*/


@Service
@Transactional
class ChangePasswordService {
    fun changePassword(currentPassword: String, newPassword: String) {
        TODO("Not yet implemented")
    }
    //    @Transactional
//    suspend fun changePassword(currentClearTextPassword: String, newPassword: String) {
//        SecurityUtils.getCurrentUserLogin().apply {
//            if (!isNullOrBlank()) {
//                userRepository.findOneByLogin(this).apply {
//                    if (this != null) {
//                        if (!passwordEncoder.matches(
//                                currentClearTextPassword,
//                                password
//                            )
//                        ) throw InvalidPasswordException()
//                        else saveUser(this.apply {
//                            password = passwordEncoder.encode(newPassword)
//                        }).run {
//                            log.debug("Changed password for User: {}", this)
//                        }
//                    }
//                }
//            }
//        }
//    }

}
