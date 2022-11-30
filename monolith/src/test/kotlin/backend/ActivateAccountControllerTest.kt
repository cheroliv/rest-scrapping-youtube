@file:Suppress(
    "NonAsciiCharacters",
    "unused",
    "SpellCheckingInspection"
)

package backend

import backend.Constants.ACTIVATE_API_PATH
import backend.Constants.ACTIVATE_API_PARAM
import backend.Data.defaultAccount
import backend.RandomUtils.generateActivationKey
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.springframework.beans.factory.getBean
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult
import java.net.URI
import kotlin.test.*


internal class ActivateAccountControllerTest {

    private val client: WebTestClient by lazy {
        WebTestClient
            .bindToServer()
            .baseUrl(BASE_URL_DEV)
            .build()
    }
    private lateinit var context: ConfigurableApplicationContext
    private val dao: R2dbcEntityTemplate by lazy { context.getBean() }

    @BeforeAll
    fun `lance le server en profile test`() =
        runApplication<BackendApplication> { testLoader(app = this) }
            .run { context = this }

    @AfterAll
    fun `arrête le serveur`() = context.close()

    @AfterEach
    fun tearDown() = deleteAllAccounts(dao)

    @Test
    fun `vérifie que la requête contient bien des données cohérentes`() {
        generateActivationKey.run {
            client
                .get()
                .uri("$ACTIVATE_API_PATH$ACTIVATE_API_PARAM", this)
                .exchange()
                .returnResult<Unit>().url.let {
                    assertEquals(URI("$BASE_URL_DEV$ACTIVATE_API_PATH$this"), it)
                }
        }
    }

    @Test
    fun `test activate avec une mauvaise clé`() {
        client
            .get()
            .uri("$ACTIVATE_API_PATH$ACTIVATE_API_PARAM", "wrongActivationKey")
            .exchange()
            .expectStatus()
            .is5xxServerError
            .returnResult<Unit>()
    }

    @Test
    fun `test activate avec une clé valide`() {
        assertEquals(0, countAccount(dao))
        assertEquals(0, countAccountAuthority(dao))
        createDataAccounts(setOf(defaultAccount), dao)
        assertEquals(1, countAccount(dao))
        assertEquals(1, countAccountAuthority(dao))

        client
            .get()
            .uri(
                "$ACTIVATE_API_PATH$ACTIVATE_API_PARAM",
                findOneByLogin(defaultAccount.login!!, dao)!!.apply {
                    assertTrue(activationKey!!.isNotBlank())
                    assertFalse(activated)
                }.activationKey
            )
            .exchange()
            .expectStatus().isOk
            .returnResult<Unit>()

        findOneByLogin(defaultAccount.login!!, dao)!!.run {
            assertNull(activationKey)
            assertTrue(activated)
        }
    }
}