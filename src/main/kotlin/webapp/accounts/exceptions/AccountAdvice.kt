package webapp.accounts.exceptions

import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler

@RestControllerAdvice
class AccountAdvice : ResponseEntityExceptionHandler() {

}