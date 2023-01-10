package webapp.signup

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import webapp.Constants
import webapp.models.exceptions.http.InvalidPasswordProblem
import webapp.models.AccountCredentials
import webapp.models.exceptions.InvalidPasswordException

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
    @PostMapping(
        Constants.SIGNUP_API,
        produces = [MediaType.APPLICATION_PROBLEM_JSON_VALUE]
    )
    @ResponseStatus(HttpStatus.CREATED)
    @Throws(InvalidPasswordProblem::class)
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
     * @throws RuntimeException `500 (Internal Application Error)` if the user couldn't be activated.
     */
    @GetMapping(Constants.ACTIVATE_API)
    suspend fun activateAccount(@RequestParam(Constants.ACTIVATE_API_KEY) key: String) {
        if (!signupService.activate(key)) throw SignupException(Constants.MSG_WRONG_ACTIVATION_KEY)
    }
}