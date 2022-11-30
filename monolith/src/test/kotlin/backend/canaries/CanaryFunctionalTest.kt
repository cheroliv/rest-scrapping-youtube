@file:Suppress("unused")

package backend.canaries

import backend.BackendApplication
import backend.testLoader
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import kotlin.test.assertTrue

internal class CanaryFunctionalTest {
    private lateinit var context: ConfigurableApplicationContext

    //    @org.junit.jupiter.api.BeforeAll
    fun `lance le server en profile test`() {
        context = runApplication<BackendApplication> { testLoader(app = this) }
    }

    //    @org.junit.jupiter.api.AfterAll
    fun `arrete le serveur`() = context.close()

    //    @kotlin.test.Test @kotlin.test.Ignore
    fun `canary functional test`() = assertTrue(context.beanDefinitionCount > 0)

}