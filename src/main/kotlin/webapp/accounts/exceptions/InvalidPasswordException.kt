package webapp.accounts.exceptions

import webapp.Constants

class InvalidPasswordException :
    RuntimeException("Incorrect password") {
    companion object {
        private const val serialVersionUID = 1L
        fun isPasswordLengthInvalid(password: String?) = when {
            password == null -> false
            password.isEmpty() -> false
            else -> (password.length < Constants.PASSWORD_MIN_LENGTH) ||
                    (password.length > Constants.PASSWORD_MAX_LENGTH)
        }
    }

}