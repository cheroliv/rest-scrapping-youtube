@file:Suppress("unused")

package backend

import backend.Constants.EMAIL_ALREADY_USED_TYPE
import backend.Constants.INVALID_PASSWORD_TYPE
import backend.Constants.LOGIN_ALREADY_USED_TYPE
import org.zalando.problem.AbstractThrowableProblem
import org.zalando.problem.Exceptional
import org.zalando.problem.Status.BAD_REQUEST
import java.net.URI

/*=================================================================================*/

class LoginAlreadyUsedProblem :
    AlertProblem(
        type = LOGIN_ALREADY_USED_TYPE,
        defaultMessage = "Login name already used!",
        entityName = "userManagement",
        errorKey = "userexists"
    ) {
    override fun getCause(): Exceptional? = super.cause

    companion object {
        private const val serialVersionUID = 1L
    }
}

/*=================================================================================*/

class InvalidPasswordProblem : AbstractThrowableProblem(
    INVALID_PASSWORD_TYPE,
    "Incorrect password",
    BAD_REQUEST
) {
    override fun getCause(): Exceptional? = super.cause

    companion object {
        private const val serialVersionUID = 1L
    }
}

/*=================================================================================*/

class EmailAlreadyUsedProblem : AlertProblem(
    type = EMAIL_ALREADY_USED_TYPE,
    defaultMessage = "Email is already in use!",
    entityName = "userManagement",
    errorKey = "emailexists"
) {
    companion object {
        private const val serialVersionUID = 1L
    }
}

/*=================================================================================*/

open class AlertProblem(
    type: URI,
    defaultMessage: String,
    val entityName: String,
    val errorKey: String
) : AbstractThrowableProblem(
    type,
    defaultMessage,
    BAD_REQUEST,
    null,
    null,
    null,
    getAlertParameters(entityName, errorKey)
) {
    constructor(
        defaultMessage: String,
        entityName: String,
        errorKey: String
    ) : this(DEFAULT_TYPE, defaultMessage, entityName, errorKey)

    override fun getCause(): Exceptional? = super.cause

    companion object {

        private const val serialVersionUID = 1L

        private fun getAlertParameters(
            entityName: String,
            errorKey: String
        ): MutableMap<String, Any> =
            mutableMapOf(
                "message" to "error.$errorKey",
                "params" to entityName
            )
    }
}

/*=================================================================================*/

