package webapp.accounts.mail

import org.springframework.context.MessageSource
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.thymeleaf.spring6.SpringTemplateEngine
import webapp.Properties
import webapp.Bootstrap.log
import webapp.Constants.GMAIL


@Suppress("unused")
@Async
@Service
@Profile(GMAIL)
class MailServiceGmail(
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
    ) = log.info(MailServiceGmail::class.java.name)
}