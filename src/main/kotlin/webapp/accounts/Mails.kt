package webapp.accounts

import webapp.ApplicationProperties
import webapp.Constants.BASE_URL
import webapp.Constants.GMAIL
import webapp.Constants.MAILSLURP
import webapp.Constants.TEMPLATE_NAME_CREATION
import webapp.Constants.TEMPLATE_NAME_PASSWORD
import webapp.Constants.TEMPLATE_NAME_SIGNUP
import webapp.Constants.TITLE_KEY_PASSWORD
import webapp.Constants.TITLE_KEY_SIGNUP
import webapp.Constants.USER
import webapp.Bootstrap.log
import jakarta.mail.MessagingException
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Profile
import org.springframework.mail.MailException
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine
import webapp.accounts.models.AccountCredentials
import java.util.Locale.forLanguageTag
import kotlin.text.Charsets.UTF_8

/*=================================================================================*/
interface MailService {
    fun sendEmail(
        to: String,
        subject: String,
        content: String,
        isMultipart: Boolean,
        isHtml: Boolean
    )

    fun sendEmailFromTemplate(
        account: AccountCredentials,
        templateName: String,
        titleKey: String
    )

    fun sendPasswordResetMail(account: AccountCredentials)
    fun sendActivationEmail(account: AccountCredentials)
    fun sendCreationEmail(account: AccountCredentials)
}
/*=================================================================================*/

abstract class AbstractMailService(
    private val properties: ApplicationProperties,
    private val messageSource: MessageSource,
    private val templateEngine: SpringTemplateEngine
) : MailService {

    abstract override fun sendEmail(
        to: String,
        subject: String,
        content: String,
        isMultipart: Boolean,
        isHtml: Boolean
    )

    override fun sendEmailFromTemplate(
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

    override fun sendActivationEmail(account: AccountCredentials) = sendEmailFromTemplate(
        account, TEMPLATE_NAME_SIGNUP, TITLE_KEY_SIGNUP
    ).run { log.debug("Sending activation email to '${account.email}'") }

    override fun sendCreationEmail(account: AccountCredentials) = sendEmailFromTemplate(
        account, TEMPLATE_NAME_CREATION, TITLE_KEY_SIGNUP
    ).run { log.debug("Sending creation email to '${account.email}'") }

    override fun sendPasswordResetMail(account: AccountCredentials) = sendEmailFromTemplate(
        account, TEMPLATE_NAME_PASSWORD, TITLE_KEY_PASSWORD
    ).run { log.debug("Sending password reset email to '${account.email}'") }
}

/*=================================================================================*/
@Async
@Service
@Profile("!$MAILSLURP & !$GMAIL")
class MailServiceSmtp(
    private val properties: ApplicationProperties,
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
}

/*=================================================================================*/
@Async
@Service
@Profile(MAILSLURP)
class MailServiceSlurp(
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
    ) = log.info(MailServiceSlurp::class.java.name)
}
/*=================================================================================*/

@Async
@Service
@Profile(GMAIL)
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
    ) = log.info(MailServiceGmail::class.java.name)
}
/*=================================================================================*/