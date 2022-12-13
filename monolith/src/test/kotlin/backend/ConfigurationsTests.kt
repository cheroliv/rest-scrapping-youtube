package backend

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.springframework.beans.factory.getBean
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.MessageSource
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ConfigurationsTests {
    private lateinit var context: ConfigurableApplicationContext
    private val messageSource: MessageSource by lazy { context.getBean() }

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
                    Locale.FRENCH
                )
            )
        }
    }

    @Test
    fun `MessageSource test message startupLog`() {
        val msg = "You have misconfigured your application!\n" +
                "It should not run with both the ${Constants.SPRING_PROFILE_DEVELOPMENT}\n" +
                "and ${Constants.SPRING_PROFILE_PRODUCTION} profiles at the same time."
        val i18nMsg = messageSource.getMessage(
            Constants.STARTUP_LOG_MSG_KEY,
            arrayOf(
                Constants.SPRING_PROFILE_DEVELOPMENT,
                Constants.SPRING_PROFILE_PRODUCTION
            ),
            Locale.getDefault()
        )
        assertEquals(msg, i18nMsg)
        Log.log.info(i18nMsg)
    }
}