@file:Suppress("unused")

package backend.canaries

import org.springframework.context.ApplicationContext
import kotlin.test.assertTrue

//@org.springframework.boot.test.context.SpringBootTest
//@org.springframework.test.context.ActiveProfiles("test")
internal class CanaryIntegrationTest {

    //    @org.springframework.beans.factory.annotation.Autowired
    private lateinit var context: ApplicationContext

//    @kotlin.test.Test
//    @kotlin.test.Ignore
    fun contextLoads() = assertTrue(context.beanDefinitionCount > 0)
}