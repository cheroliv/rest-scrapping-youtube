package webapp.accounts.exceptions

import jakarta.validation.ConstraintViolationException
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.badRequest
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler
import webapp.Logging.i

@RestControllerAdvice
class AccountAdvice : ResponseEntityExceptionHandler() {
    @ExceptionHandler(ConstraintViolationException::class)
    suspend fun handleConstraintViolationException(
        cve: ConstraintViolationException,
        req: WebRequest
    ): ResponseEntity<ProblemDetail> = badRequest().build<ProblemDetail?>().apply {
        i("passé par ici: ${cve.message}")
    }

//    @ExceptionHandler(UsernameAlreadyUsedException::class)
//    suspend fun handleUsernameAlreadyUsedException(
//        ex: UsernameAlreadyUsedException,
//        request: ServerWebExchange
//    ): ResponseEntity<ProblemDetail> {
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
}





