package webapp.accounts.password

import jakarta.validation.constraints.Email
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import webapp.Bootstrap
import webapp.Constants
import webapp.accounts.mail.MailService
import webapp.accounts.models.KeyAndPassword
import webapp.accounts.models.exceptions.InvalidPasswordException

@Suppress("unused")
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
    suspend fun requestPasswordReset(@RequestBody @Email mail: String) =
        with(resetPasswordService.requestPasswordReset(mail)) {
            if (this == null) Bootstrap.log.warn("Password reset requested for non existing mail")
            else mailService.sendPasswordResetMail(this)
        }

    /**
     * {@code POST   /account/reset-password/finish} : Finish to reset the password of the user.
     *
     * @param keyAndPassword the generated key and the new password.
     * @throws InvalidPasswordProblem {@code 400 (Bad Request)} if the password is incorrect.
     * @throws RuntimeException         {@code 500 (Internal WebApplication Error)} if the password could not be reset.
     */
    @PostMapping(Constants.RESET_PASSWORD_API_FINISH)
    suspend fun finishPasswordReset(@RequestBody keyAndPassword: KeyAndPassword): Unit =
        with(InvalidPasswordException()) {
            if (isPasswordLengthInvalid(keyAndPassword.newPassword)) throw this
            else if (keyAndPassword.newPassword != null
                && keyAndPassword.key != null
                && resetPasswordService.completePasswordReset(
                    keyAndPassword.newPassword,
                    keyAndPassword.key
                ) == null
            ) throw ResetPasswordException("No user was found for this reset key")
        }
}