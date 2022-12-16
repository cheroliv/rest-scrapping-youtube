package backend

import backend.Constants.BASE_URL
import backend.Constants.GMAIL
import backend.Constants.MAILSLURP
import backend.Constants.USER
import backend.Log.log
import com.mailslurp.apis.InboxControllerApi
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Profile
import org.springframework.mail.MailException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import java.util.Locale.forLanguageTag
import javax.mail.MessagingException
import kotlin.text.Charsets.UTF_8

/*=================================================================================*/

@Async
@Service
@Profile("!$GMAIL or !$MAILSLURP")
class MailService(
    private val properties: ApplicationProperties,
    private val mailSender: JavaMailSender,
    private val messageSource: MessageSource,
    private val templateEngine: SpringTemplateEngine
) {
    fun sendEmail(
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
                UTF_8.name()
            ).apply {
                setTo(to)
                setFrom(properties.mail.from)
                setSubject(subject)
                setText(content, isHtml)
            }
            mailSender.send(this)
            log.debug("Sent email to User '$to'")
        } catch (e: MailException) {
            log.warn("Email could not be sent to user '$to'", e)
        } catch (e: MessagingException) {
            log.warn("Email could not be sent to user '$to'", e)
        }
    }

    fun sendEmailFromTemplate(
        account: AccountCredentials,
        templateName: String,
        titleKey: String
    ) {
        when (account.email) {
            null -> {
                log.debug("Email doesn't exist for user '${account.login}'")
                return
            }

            else -> forLanguageTag(account.langKey).apply {
                sendEmail(
                    account.email,
                    messageSource.getMessage(titleKey, null, this),
                    templateEngine.process(templateName, Context(this).apply {
                        setVariable(USER, account)
                        setVariable(BASE_URL, properties.mail.baseUrl)
                    }),
                    isMultipart = false,
                    isHtml = true
                )
            }
        }
    }

    fun sendActivationEmail(account: AccountCredentials): Unit = log.debug(
        "Sending activation email to '{}'", account.email
    ).run {
        sendEmailFromTemplate(
            account, "mail/activationEmail", "email.activation.title"
        )
    }

    fun sendCreationEmail(account: AccountCredentials): Unit =
        log.debug("Sending creation email to '${account.email}'").run {
            sendEmailFromTemplate(
                account, "mail/creationEmail", "email.activation.title"
            )
        }

    fun sendPasswordResetMail(account: AccountCredentials): Unit =
        log.debug("Sending password reset email to '${account.email}'").run {
            sendEmailFromTemplate(
                account, "mail/passwordResetEmail", "email.reset.title"
            )
        }
}
/*=================================================================================*/

@Service
@Profile(MAILSLURP)
class MailSlurpService(
    private val properties: ApplicationProperties,
    private val mailSender: JavaMailSender,
    private val messageSource: MessageSource,
    private val templateEngine: SpringTemplateEngine
) : MailService(properties, mailSender, messageSource, templateEngine) {
    private val inboxController by lazy { InboxControllerApi(properties.mailslurp.token) }

    override fun sendEmail(
        to: String,
        subject: String,
        content: String,
        isMultipart: Boolean,
        isHtml: Boolean
    ) {

        TODO("Not yet implemented")
    }

}

/*=================================================================================*/
@Service
@Profile(GMAIL)
class GmailService(
    private val properties: ApplicationProperties,
    private val mailSender: JavaMailSender,
    private val messageSource: MessageSource,
    private val templateEngine: SpringTemplateEngine
) : MailService(properties, mailSender, messageSource, templateEngine) {

    override fun sendEmail(
        to: String,
        subject: String,
        content: String,
        isMultipart: Boolean,
        isHtml: Boolean
    ) {

        TODO("Not yet implemented")
    }

}
/*=================================================================================*/
