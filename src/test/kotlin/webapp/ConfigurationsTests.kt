package webapp

import webapp.Constants.DEVELOPMENT
import webapp.Constants.PRODUCTION
import webapp.Constants.STARTUP_LOG_MSG_KEY
import webapp.Bootstrap.log
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.springframework.beans.factory.getBean
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.MessageSource
import java.util.*
import java.util.Locale.FRENCH
import kotlin.test.Test
import kotlin.test.assertEquals

class ConfigurationsTests {

    private lateinit var context: ConfigurableApplicationContext
    private val messageSource: MessageSource by lazy { context.getBean() }
private val properties:ApplicationProperties by lazy { context.getBean() }
    @BeforeAll
    fun `lance le server en profile test`() {
        context = launcher()
    }

    @Suppress("NonAsciiCharacters")
    @AfterAll
    fun `arrête le serveur`() = context.close()

    @Test
    fun `MessageSource test email_activation_greeting message fr`() {
        "Oliv".apply {
            assertEquals(
                expected = "Cher $this",
                actual = messageSource.getMessage(
                    "email.activation.greeting",
                    arrayOf(this),
                    FRENCH
                )
            )
        }
    }

    @Test
    fun `MessageSource test message startupLog`() {
        val msg = "You have misconfigured your application!\n" +
                "It should not run with both the $DEVELOPMENT\n" +
                "and $PRODUCTION profiles at the same time."
        val i18nMsg = messageSource.getMessage(
            STARTUP_LOG_MSG_KEY,
            arrayOf(
                DEVELOPMENT,
                PRODUCTION
            ),
            Locale.getDefault()
        )
        assertEquals(msg, i18nMsg)
        log.info(i18nMsg)
    }

    @Test
    fun `test go visit message`(){
        assertEquals(
            "https://github.com/cheroliv/kotlin-springboot",
            properties.goVisitMessage
        )

    }
}