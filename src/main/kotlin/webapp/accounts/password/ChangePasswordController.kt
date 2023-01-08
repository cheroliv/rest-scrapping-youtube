package webapp.accounts.password

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import webapp.Constants
import webapp.accounts.models.PasswordChange
import webapp.accounts.models.exceptions.InvalidPasswordException

/*=================================================================================*/
@Suppress("unused")
@RestController
@RequestMapping(Constants.ACCOUNT_API)
class ChangePasswordController(private val changePasswordService: ChangePasswordService) {
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
            if (isPasswordLengthInvalid(passwordChange.newPassword)) throw this
            else if (passwordChange.currentPassword != null
                && passwordChange.newPassword != null
            ) changePasswordService.changePassword(
                passwordChange.currentPassword,
                passwordChange.newPassword
            )

        }


}