package webapp.mail

import org.springframework.context.MessageSource
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine
import webapp.Properties
import webapp.Constants.BASE_URL
import webapp.Constants.TEMPLATE_NAME_CREATION
import webapp.Constants.TEMPLATE_NAME_PASSWORD
import webapp.Constants.TEMPLATE_NAME_SIGNUP
import webapp.Constants.TITLE_KEY_PASSWORD
import webapp.Constants.TITLE_KEY_SIGNUP
import webapp.Constants.USER
import webapp.Logging.d
import webapp.accounts.models.AccountCredentials
import java.util.Locale.forLanguageTag

abstract class AbstractMailService(
    private val properties: Properties,
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
                d("Email doesn't exist for user '${account.login}'")
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
    ).run { d("Sending activation email to '${account.email}'") }

    override fun sendCreationEmail(account: AccountCredentials) = sendEmailFromTemplate(
        account, TEMPLATE_NAME_CREATION, TITLE_KEY_SIGNUP
    ).run { d("Sending creation email to '${account.email}'") }

    override fun sendPasswordResetMail(account: AccountCredentials) = sendEmailFromTemplate(
        account, TEMPLATE_NAME_PASSWORD, TITLE_KEY_PASSWORD
    ).run { d("Sending password reset email to '${account.email}'") }
}