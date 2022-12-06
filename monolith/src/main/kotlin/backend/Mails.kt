package backend

import org.springframework.context.MessageSource
import org.springframework.mail.MailException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import org.thymeleaf.spring5.SpringTemplateEngine
import java.nio.charset.StandardCharsets
import java.util.*
import javax.mail.MessagingException

/*=================================================================================*/

@Service("mailService")
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
                StandardCharsets.UTF_8.name()
            ).apply {
                setTo(to)
                setFrom(properties.mail.from)
                setSubject(subject)
                setText(content, isHtml)
            }
            mailSender.send(this)
            Log.log.debug("Sent email to User '$to'")
        } catch (e: MailException) {
            Log.log.warn("Email could not be sent to user '$to'", e)
        } catch (e: MessagingException) {
            Log.log.warn("Email could not be sent to user '$to'", e)
        }
    }

    @Async
    fun sendEmailFromTemplate(
        account: AccountCredentials, templateName: String, titleKey: String
    ) {
        when (account.email) {
            null -> {
                Log.log.debug("Email doesn't exist for user '${account.login}'")
                return
            }

            else -> Locale.forLanguageTag(account.langKey).apply {
                sendEmail(
                    account.email,
                    messageSource.getMessage(titleKey, null, this),
                    templateEngine.process(templateName, Context(this).apply {
                        setVariable(Constants.USER, account)
                        setVariable(Constants.BASE_URL, properties.mail.baseUrl)
                    }),
                    isMultipart = false,
                    isHtml = true
                )
            }
        }
    }

    @Async
    fun sendActivationEmail(account: AccountCredentials): Unit = Log.log.debug(
        "Sending activation email to '{}'", account.email
    ).run {
        sendEmailFromTemplate(
            account, "mail/activationEmail", "email.activation.title"
        )
    }

    @Async
    fun sendCreationEmail(account: AccountCredentials): Unit =
        Log.log.debug("Sending creation email to '${account.email}'").run {
            sendEmailFromTemplate(
                account, "mail/creationEmail", "email.activation.title"
            )
        }

    @Async
    fun sendPasswordResetMail(account: AccountCredentials): Unit =
        Log.log.debug("Sending password reset email to '${account.email}'").run {
            sendEmailFromTemplate(
                account, "mail/passwordResetEmail", "email.reset.title"
            )
        }
}

/*=================================================================================*/
