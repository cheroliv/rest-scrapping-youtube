package webapp.mail

import org.springframework.context.MessageSource
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.thymeleaf.spring6.SpringTemplateEngine
import webapp.Constants
import webapp.Logging
import webapp.ApplicationProperties

@Suppress("unused")
@Async
@Service
@Profile(Constants.GMAIL)
class MailServiceGmail(
    private val properties: ApplicationProperties,
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
    ) = Logging.i(MailServiceGmail::class.java.name)
}