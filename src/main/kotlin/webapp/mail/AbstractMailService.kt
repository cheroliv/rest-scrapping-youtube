package webapp.mail

import org.springframework.context.MessageSource
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine
import webapp.Constants
import webapp.Logging
import webapp.Properties
import webapp.models.AccountCredentials
import java.util.*

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
                Logging.d("Email doesn't exist for user '${account.login}'")
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

    override fun sendActivationEmail(account: AccountCredentials) = sendEmailFromTemplate(
        account, Constants.TEMPLATE_NAME_SIGNUP, Constants.TITLE_KEY_SIGNUP
    ).run { Logging.d("Sending activation email to '${account.email}'") }

    override fun sendCreationEmail(account: AccountCredentials) = sendEmailFromTemplate(
        account, Constants.TEMPLATE_NAME_CREATION, Constants.TITLE_KEY_SIGNUP
    ).run { Logging.d("Sending creation email to '${account.email}'") }

    override fun sendPasswordResetMail(account: AccountCredentials) = sendEmailFromTemplate(
        account, Constants.TEMPLATE_NAME_PASSWORD, Constants.TITLE_KEY_PASSWORD
    ).run { Logging.d("Sending password reset email to '${account.email}'") }
}