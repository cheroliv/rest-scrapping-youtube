@file:Suppress("unused")

package backend.accounts

import backend.Constants.INVALID_PASSWORD_TYPE
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail.forStatusAndDetail
import org.springframework.web.ErrorResponseException

//import org.zalando.problem.Problem.DEFAULT_TYPE as PROBLEM_DEFAULT_TYPE
/*

https://www.sivalabs.in/spring-boot-3-error-reporting-using-problem-details/

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import java.net.URI;
import java.time.Instant;

public class BookmarkNotFoundException extends ErrorResponseException {

    public BookmarkNotFoundException(Long bookmarkId) {
        super(HttpStatus.NOT_FOUND, asProblemDetail("Bookmark with id "+ bookmarkId+" not found"), null);
    }

    private static ProblemDetail asProblemDetail(String message) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, message);
        problemDetail.setTitle("Bookmark Not Found");
        problemDetail.setType(URI.create("https://api.bookmarks.com/errors/not-found"));
        problemDetail.setProperty("errorCategory", "Generic");
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }
}
 */
/*=================================================================================*/

//class InvalidPasswordProblem : AbstractThrowableProblem(
//    INVALID_PASSWORD_TYPE,
//    "Incorrect password",
//    BAD_REQUEST
//) {
//    override fun getCause(): Exceptional? = super.cause
//
//    companion object {
//        private const val serialVersionUID = 1L
//    }
//}
/*{
  "type": "https://www.cheroliv.com/problem/constraint-violation",
  "title": "Data binding and validation failure",
  "status": 400,
  "path": "/api/register",
  "message": "error.validation",
  "fieldErrors": [
    {
      "objectName": "managedUserVM",
      "field": "password",
      "message": "la taille doit être comprise entre 4 et 100"
    }
  ]
}*/
/*
{
  "type": "https://www.cheroliv.com/problem/constraint-violation",
  "title": "Data binding and validation failure",
  "status": 400,
  "path": "/api/register",
  "message": "error.validation",
  "fieldErrors": [
    {
      "objectName": "managedUserVM",
      "field": "email",
      "message": "doit être une adresse électronique syntaxiquement correcte"
    },
    {
      "objectName": "managedUserVM",
      "field": "password",
      "message": "la taille doit être comprise entre 4 et 100"
    },
    {
      "objectName": "managedUserVM",
      "field": "login",
      "message": "doit correspondre à \"^(?>[a-zA-Z0-9!$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*)|(?>[_.@A-Za-z0-9-]+)$\""
    },
    {
      "objectName": "managedUserVM",
      "field": "login",
      "message": "la taille doit être comprise entre 1 et 50"
    },
    {
      "objectName": "managedUserVM",
      "field": "login",
      "message": "ne doit pas être vide"
    }
  ]
}
*/
class InvalidPasswordProblem(
    exception: InvalidPasswordException
) : ErrorResponseException(
    BAD_REQUEST,
    forStatusAndDetail(BAD_REQUEST, exception.message!!).apply {
        type = INVALID_PASSWORD_TYPE
        title = "Incorrect password"
    },
    exception
)

/*=================================================================================*/
//class Foo(
//    errorAttributes: ErrorAttributes?,
//    resources: WebProperties.Resources?,
//    errorProperties: ErrorProperties?,
//    applicationContext: ApplicationContext?
//) : AbstractErrorWebExceptionHandler(
//    errorAttributes,
//    resources,
////    errorProperties,
//    applicationContext
//) {
//    override fun getRoutingFunction(errorAttributes: ErrorAttributes?): RouterFunction<ServerResponse> {
//        TODO("Not yet implemented")
//    }
//}

//class LoginAlreadyUsedProblem :
//    AlertProblem(
//        type = LOGIN_ALREADY_USED_TYPE,
//        defaultMessage = "Login name already used!",
//        entityName = "userManagement",
//        errorKey = "userexists"
//    ) {
//    override fun getCause(): Exceptional? = super.cause
//
//    companion object {
//
//        private const val serialVersionUID = 1L
//    }
//}

/*=================================================================================*/

//class EmailAlreadyUsedProblem : AlertProblem(
//    type = EMAIL_ALREADY_USED_TYPE,
//    defaultMessage = "Email is already in use!",
//    entityName = "userManagement",
//    errorKey = "emailexists"
//) {
//    companion object {
//        private const val serialVersionUID = 1L
//    }
//}

/*=================================================================================*/

//open class AlertProblem(
//    type: URI,
//    defaultMessage: String,
//    val entityName: String,
//    val errorKey: String
//) : AbstractThrowableProblem(
//    type,
//    defaultMessage,
//    BAD_REQUEST,
//    null,
//    null,
//    null,
//    getAlertParameters(entityName, errorKey)
//) {
//    constructor(
//        defaultMessage: String,
//        entityName: String,
//        errorKey: String
//    ) : this(DEFAULT_TYPE, defaultMessage, entityName, errorKey)
//
//    override fun getCause(): Exceptional? = super.cause
//
//    companion object {
//
//        private const val serialVersionUID = 1L
//
//        private fun getAlertParameters(
//            entityName: String,
//            errorKey: String
//        ): MutableMap<String, Any> =
//            mutableMapOf(
//                "message" to "error.$errorKey",
//                "params" to entityName
//            )
//    }
//}

/*=================================================================================*/
/*Translators*/
/*=================================================================================*/

/**
 * Controller advice to translate the backend side problems to client-friendly json structures.
 * The error response follows RFC7807 - Problem Details for HTTP APIs (https://tools.ietf.org/html/rfc7807).
 */
//@Component
//@ControllerAdvice
//class ProblemTranslator(
//    private val env: Environment,
//    private val properties: ApplicationProperties
//) : ProblemHandling, SecurityAdviceTrait {
//
//    companion object {
//        private const val FIELD_ERRORS_KEY = "fieldErrors"
//        private const val MESSAGE_KEY = "message"
//        private const val PATH_KEY = "path"
//        private const val VIOLATIONS_KEY = "violations"
//    }
//
//    class FieldErrorVM(
//        @Suppress("unused")
//        val objectName: String,
//        val field: String,
//        val message: String?
//    ) : Serializable {
//        companion object {
//            private const val serialVersionUID = 1L
//        }
//    }
//
//    /**
//     * Post-process the Problem payload to add the message key for the front-end if needed.
//     */
//    override fun process(
//        entity: ResponseEntity<Problem>,
//        request: ServerWebExchange
//    ): Mono<ResponseEntity<Problem>> {
//        @Suppress("SENSELESS_COMPARISON")
//        if (entity == null) return Mono.empty()
//        val problem = entity.body
//        if (
//            !(problem is ConstraintViolationProblem
//                    || problem is DefaultProblem)
//        ) return just(entity)
//        val builder = builder()
//            .withType(
//                if (PROBLEM_DEFAULT_TYPE == problem.type) DEFAULT_TYPE
//                else problem.type
//            )
//            .withStatus(problem.status)
//            .withTitle(problem.title)
//            .with(PATH_KEY, request.request.path.value())
//
//        if (problem is ConstraintViolationProblem) builder
//            .with(VIOLATIONS_KEY, problem.violations)
//            .with(MESSAGE_KEY, ERR_VALIDATION)
//        else {
//            builder
//                .withCause((problem as DefaultProblem).cause)
//                .withDetail(problem.detail)
//                .withInstance(problem.instance)
//            problem.parameters.forEach { (key, value) -> builder.with(key, value) }
//            if (!problem.parameters.containsKey(MESSAGE_KEY) && problem.status != null) {
//                builder.with(
//                    MESSAGE_KEY,
//                    "error.http." + problem.status!!.statusCode
//                )
//            }
//        }
//        return just(
//            ResponseEntity<Problem>(
//                builder.build(),
//                entity.headers,
//                entity.statusCode
//            )
//        )
//    }
//
//    override fun handleBindingResult(
//        ex: WebExchangeBindException, request: ServerWebExchange
//    ): Mono<ResponseEntity<Problem>> = create(
//        ex, builder()
//            .withType(CONSTRAINT_VIOLATION_TYPE)
//            .withTitle("Data binding and validation failure")
//            .withStatus(BAD_REQUEST)
//            .with(MESSAGE_KEY, ERR_VALIDATION)
//            .with(FIELD_ERRORS_KEY, ex.bindingResult.fieldErrors.map {
//                FieldErrorVM(
//                    it.objectName.replaceFirst(
//                        Regex(pattern = "DTO$"),
//                        replacement = ""
//                    ), it.field, it.code
//                )
//            }).build(), request
//    )
//
//    @ExceptionHandler
//    fun handleEmailAlreadyUsedException(
//        ex: EmailAlreadyUsedException,
//        request: ServerWebExchange
//    ): Mono<ResponseEntity<Problem>> {
//        val problem = EmailAlreadyUsedProblem()
//        return create(
//            problem,
//            request,
//            createFailureAlert(
//                applicationName = properties.clientApp.name,
//                enableTranslation = true,
//                entityName = problem.entityName,
//                errorKey = problem.errorKey,
//                defaultMessage = problem.message
//            )
//        )
//    }
//
//    @ExceptionHandler
//    fun handleUsernameAlreadyUsedException(
//        ex: UsernameAlreadyUsedException,
//        request: ServerWebExchange
//    ): Mono<ResponseEntity<Problem>> {
//        val problem = LoginAlreadyUsedProblem()
//        return create(
//            problem,
//            request,
//            createFailureAlert(
//                applicationName = properties.clientApp.name,
//                enableTranslation = true,
//                entityName = problem.entityName,
//                errorKey = problem.errorKey,
//                defaultMessage = problem.message
//            )
//        )
//    }
//
//    @ExceptionHandler
//    fun handleInvalidPasswordException(
//        ex: InvalidPasswordException,
//        request: ServerWebExchange
//    ): Mono<ResponseEntity<Problem>> = create(
//        InvalidPasswordProblem(),
//        request
//    )
//
//    @ExceptionHandler
//    fun handleBadRequestAlertException(
//        ex: AlertProblem,
//        request: ServerWebExchange
//    ): Mono<ResponseEntity<Problem>> =
//        create(
//            ex, request,
//            createFailureAlert(
//                applicationName = properties.clientApp.name,
//                enableTranslation = true,
//                entityName = ex.entityName,
//                errorKey = ex.errorKey,
//                defaultMessage = ex.message
//            )
//        )
//
//    @ExceptionHandler
//    fun handleConcurrencyFailure(
//        ex: ConcurrencyFailureException,
//        request: ServerWebExchange
//    ): Mono<ResponseEntity<Problem>> = create(
//        ex, builder()
//            .withStatus(Status.CONFLICT)
//            .with(MESSAGE_KEY, Constants.ERR_CONCURRENCY_FAILURE)
//            .build(), request
//    )
//
//    override fun prepare(
//        throwable: Throwable,
//        status: StatusType,
//        type: URI
//    ): ProblemBuilder {
//        var detail = throwable.message
//        if (env.activeProfiles.contains(Constants.PRODUCTION)) {
//            detail = when (throwable) {
//                is HttpMessageConversionException -> "Unable to convert http message"
//                is DataAccessException -> "Failure during data access"
//                else -> {
//                    if (containsPackageName(throwable.message))
//                        "Unexpected runtime exceptions"
//                    else throwable.message
//                }
//            }
//        }
//        return builder()
//            .withType(type)
//            .withTitle(status.reasonPhrase)
//            .withStatus(status)
//            .withDetail(detail)
//            .withCause(throwable.cause.takeIf { isCausalChainsEnabled }?.let { toProblem(it) })
//    }
//
//    private fun containsPackageName(message: String?) =
//        listOf(
//            "org.",
//            "java.",
//            "net.",
//            "javax.",
//            "com.",
//            "io.",
//            "de.",
//            "backend"
//        ).any { it == message }
//
//
//}

/*=================================================================================*/

