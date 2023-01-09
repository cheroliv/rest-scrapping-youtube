package webapp.accounts.mail

import org.springframework.context.MessageSource
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.thymeleaf.spring6.SpringTemplateEngine
import webapp.Constants
import webapp.Logging.i
import webapp.Properties

/*=================================================================================*/
@Suppress("unused")
@Async
@Service
@Profile(Constants.MAILSLURP)
class MailServiceSlurp(
    private val properties: Properties,
    private val messageSource: MessageSource,
    private val templateEngine: SpringTemplateEngine
) : AbstractMailService(
    properties,
    messageSource,
    templateEngine
) {
    override fun sendEmail(
        to: String,
        subject: String,
        content: String,
        isMultipart: Boolean,
        isHtml: Boolean
    ) = i(MailServiceSlurp::class.java.name)
}