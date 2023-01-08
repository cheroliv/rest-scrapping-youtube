package webapp.accounts.password

import jakarta.validation.constraints.Email
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import webapp.Constants
import webapp.Constants.CHANGE_PASSWORD_API
import webapp.Constants.RESET_PASSWORD_API_FINISH
import webapp.Constants.RESET_PASSWORD_API_INIT
import webapp.accounts.mail.MailService
import webapp.accounts.models.KeyAndPassword
import webapp.accounts.models.PasswordChange
import webapp.accounts.models.exceptions.InvalidPasswordException

/*=================================================================================*/
@Suppress("unused")
@RestController
@RequestMapping(Constants.ACCOUNT_API)
class PasswordController(
    private val passwordService: PasswordService,
    private val mailService: MailService
) {
    internal class PasswordException(message: String) : RuntimeException(message)

    /**
     * {@code POST  /account/change-password} : changes the current user's password.
     *
     * @param passwordChange current and new password.
     * @throws InvalidPasswordProblem {@code 400 (Bad Request)} if the new password is incorrect.
     */
    @PostMapping(CHANGE_PASSWORD_API)
    suspend fun changePassword(@RequestBody passwordChange: PasswordChange): Unit =
        InvalidPasswordException().run {
            if (isPasswordLengthInvalid(passwordChange.newPassword)) throw this
            else if (passwordChange.currentPassword != null
                && passwordChange.newPassword != null
            ) passwordService.changePassword(
                passwordChange.currentPassword,
                passwordChange.newPassword
            )

        }

    /**
     * {@code POST   /account/reset-password/init} : Send an email to reset the password of the user.
     *
     * @param mail the mail of the user.
     */
    @PostMapping(RESET_PASSWORD_API_INIT)
    suspend fun requestPasswordReset(@RequestBody @Email mail: String) =
        with(passwordService.requestPasswordReset(mail)) {
            if (this == null) webapp.Bootstrap.log.warn("Password reset requested for non existing mail")
            else mailService.sendPasswordResetMail(this)
        }

    /**
     * {@code POST   /account/reset-password/finish} : Finish to reset the password of the user.
     *
     * @param keyAndPassword the generated key and the new password.
     * @throws InvalidPasswordProblem {@code 400 (Bad Request)} if the password is incorrect.
     * @throws RuntimeException         {@code 500 (Internal WebApplication Error)} if the password could not be reset.
     */
    @PostMapping(RESET_PASSWORD_API_FINISH)
    suspend fun finishPasswordReset(@RequestBody keyAndPassword: KeyAndPassword): Unit =
        with(InvalidPasswordException()) {
            if (isPasswordLengthInvalid(keyAndPassword.newPassword)) throw this
            else if (keyAndPassword.newPassword != null
                && keyAndPassword.key != null
                && passwordService.completePasswordReset(
                    keyAndPassword.newPassword,
                    keyAndPassword.key
                ) == null
            ) throw PasswordException("No user was found for this reset key")
        }

}