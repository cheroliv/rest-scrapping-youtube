package backend

import org.springframework.security.core.AuthenticationException
import backend.Constants.PASSWORD_MAX_LENGTH
import backend.Constants.PASSWORD_MIN_LENGTH
import org.apache.commons.lang3.StringUtils

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

    fun isPasswordLengthInvalid(password: String?): Boolean =
        if (StringUtils.isEmpty(password)) false
        else (password?.length!! < PASSWORD_MIN_LENGTH) ||
                (password.length > PASSWORD_MAX_LENGTH)
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
