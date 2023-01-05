package webapp.accounts

import webapp.Constants.PASSWORD_MAX_LENGTH
import webapp.Constants.PASSWORD_MIN_LENGTH
import org.springframework.security.core.AuthenticationException

/*=================================================================================*/

class UsernameAlreadyUsedException :
    RuntimeException("Login name already used!") {
    companion object {
        private const val serialVersionUID = 1L
    }
}

/*=================================================================================*/

class InvalidPasswordException :
    RuntimeException("Incorrect password") {
    companion object {
        private const val serialVersionUID = 1L
    }

    fun isPasswordLengthInvalid(password: String?) = when {
        password == null -> false
        password.isEmpty() -> false
        else -> (password.length < PASSWORD_MIN_LENGTH) ||
                (password.length > PASSWORD_MAX_LENGTH)
    }
}

/*=================================================================================*/

class EmailAlreadyUsedException :
    RuntimeException("Email is already in use!") {
    companion object {
        private const val serialVersionUID = 1L
    }
}

/*=================================================================================*/


class UserNotActivatedException(
    message: String,
    t: Throwable? = null
) : AuthenticationException(message, t) {
    companion object {
        private const val serialVersionUID = 1L
    }
}

/*=================================================================================*/
