package webapp.accounts.mail

import jakarta.mail.MessagingException
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Profile
import org.springframework.mail.MailException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.thymeleaf.spring6.SpringTemplateEngine
import webapp.Constants.GMAIL
import webapp.Constants.MAILSLURP
import webapp.Logging
import webapp.Properties

/*=================================================================================*/
@Async
@Service
@Profile("!$MAILSLURP & !$GMAIL")
class MailServiceSmtp(
    private val properties: Properties,
    private val mailSender: JavaMailSender,
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
    ) = mailSender.createMimeMessage().run {
        try {
            MimeMessageHelper(
                this,
                isMultipart,
                Charsets.UTF_8.name()
            ).apply {
                setTo(to)
                setFrom(properties.mail.from)
                setSubject(subject)
                setText(content, isHtml)
            }
            mailSender.send(this)
            Logging.log.debug("Sent email to User '$to'")
        } catch (e: MailException) {
            Logging.log.warn("Email could not be sent to user '$to'", e)
        } catch (e: MessagingException) {
            Logging.log.warn("Email could not be sent to user '$to'", e)
        }
    }
}