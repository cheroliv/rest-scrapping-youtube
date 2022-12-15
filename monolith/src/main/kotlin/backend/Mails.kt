package backend

import backend.Constants.BASE_URL
import backend.Constants.GMAIL
import backend.Constants.MAILSLURP
import backend.Constants.USER
import backend.Log.log
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Profile
import org.springframework.mail.MailException
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.mail.javamail.MimeMessagePreparator
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import java.io.InputStream
import java.util.Locale.forLanguageTag
import javax.mail.MessagingException
import javax.mail.internet.MimeMessage
import kotlin.text.Charsets.UTF_8

/*=================================================================================*/
@Service
@Profile(MAILSLURP)
class SenderMailSlurp : JavaMailSender {
    override fun send(mimeMessage: MimeMessage) {
        TODO("Not yet implemented")
    }

    override fun send(vararg mimeMessages: MimeMessage?) {
        TODO("Not yet implemented")
    }

    override fun send(mimeMessagePreparator: MimeMessagePreparator) {
        TODO("Not yet implemented")
    }

    override fun send(vararg mimeMessagePreparators: MimeMessagePreparator?) {
        TODO("Not yet implemented")
    }

    override fun send(simpleMessage: SimpleMailMessage) {
        TODO("Not yet implemented")
    }

    override fun send(vararg simpleMessages: SimpleMailMessage?) {
        TODO("Not yet implemented")
    }

    override fun createMimeMessage(): MimeMessage {
        TODO("Not yet implemented")
    }

    override fun createMimeMessage(contentStream: InputStream): MimeMessage {
        TODO("Not yet implemented")
    }

}

/*=================================================================================*/
@Service
@Profile(GMAIL)
class SenderGmail : JavaMailSender {
    override fun send(mimeMessage: MimeMessage) {
        TODO("Not yet implemented")
    }

    override fun send(vararg mimeMessages: MimeMessage?) {
        TODO("Not yet implemented")
    }

    override fun send(mimeMessagePreparator: MimeMessagePreparator) {
        TODO("Not yet implemented")
    }

    override fun send(vararg mimeMessagePreparators: MimeMessagePreparator?) {
        TODO("Not yet implemented")
    }

    override fun send(simpleMessage: SimpleMailMessage) {
        TODO("Not yet implemented")
    }

    override fun send(vararg simpleMessages: SimpleMailMessage?) {
        TODO("Not yet implemented")
    }

    override fun createMimeMessage(): MimeMessage {
        TODO("Not yet implemented")
    }

    override fun createMimeMessage(contentStream: InputStream): MimeMessage {
        TODO("Not yet implemented")
    }

}

/*=================================================================================*/
//TODO: AbstractMailService

@Service
@Profile("!$GMAIL or !$MAILSLURP")
class MailService(
    private val properties: ApplicationProperties,
    private val mailSender: JavaMailSender,
    private val messageSource: MessageSource,
    private val templateEngine: SpringTemplateEngine
) {
    @Async
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

    @Async
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

    @Async
    fun sendActivationEmail(account: AccountCredentials): Unit = log.debug(
        "Sending activation email to '{}'", account.email
    ).run {
        sendEmailFromTemplate(
            account, "mail/activationEmail", "email.activation.title"
        )
    }

    @Async
    fun sendCreationEmail(account: AccountCredentials): Unit =
        log.debug("Sending creation email to '${account.email}'").run {
            sendEmailFromTemplate(
                account, "mail/creationEmail", "email.activation.title"
            )
        }

    @Async
    fun sendPasswordResetMail(account: AccountCredentials): Unit =
        log.debug("Sending password reset email to '${account.email}'").run {
            sendEmailFromTemplate(
                account, "mail/passwordResetEmail", "email.reset.title"
            )
        }
}

/*=================================================================================*/
