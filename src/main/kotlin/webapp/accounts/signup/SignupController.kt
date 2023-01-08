package webapp.accounts.signup

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import webapp.Constants
import webapp.accounts.models.AccountCredentials
import webapp.accounts.models.exceptions.InvalidPasswordException
import webapp.accounts.models.exceptions.http.InvalidPasswordProblem

@Suppress("unused")
@RestController
@RequestMapping(Constants.ACCOUNT_API)
class SignupController(private val signupService: SignupService) {
    internal class SignupException(message: String) : RuntimeException(message)

    /**
     * {@code POST  /signup} : register the user.
     *
     * @param accountCredentials the managed user View Model.
     * @throws webapp.InvalidPasswordException {@code 400 (Bad Request)} if the password is incorrect.
     * @throws webapp.EmailAlreadyUsedProblem {@code 400 (Bad Request)} if the email is already used.
     * @throws webapp.LoginAlreadyUsedProblem {@code 400 (Bad Request)} if the login is already used.
     */
    @Throws(InvalidPasswordProblem::class)
    @PostMapping(Constants.SIGNUP_API)
    @ResponseStatus(HttpStatus.CREATED)
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
     * @throws RuntimeException `500 (Internal WebApplication Error)` if the user couldn't be activated.
     */
    @GetMapping(Constants.ACTIVATE_API)
    suspend fun activateAccount(@RequestParam(value = Constants.ACTIVATE_API_KEY) key: String) {
        if (!signupService.activate(key)) throw SignupException(Constants.MSG_WRONG_ACTIVATION_KEY)
    }
}