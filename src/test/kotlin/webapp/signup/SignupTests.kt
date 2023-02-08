package webapp.signup

import jakarta.validation.Validator
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.springframework.beans.factory.getBean
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.bindToServer
import org.springframework.test.web.reactive.server.returnResult
import webapp.*
import webapp.Constants.BASE_URL_DEV
import webapp.Constants.SIGNUP_API_PATH
import webapp.DataTests.defaultAccount
import webapp.accounts.entities.AccountRecord.Companion.EMAIL_FIELD
import webapp.accounts.entities.AccountRecord.Companion.FIRST_NAME_FIELD
import webapp.accounts.entities.AccountRecord.Companion.LAST_NAME_FIELD
import webapp.accounts.entities.AccountRecord.Companion.LOGIN_FIELD
import webapp.accounts.entities.AccountRecord.Companion.PASSWORD_FIELD
import webapp.accounts.models.AccountCredentials
import webapp.accounts.models.AccountUtils
import java.net.URI
import kotlin.test.*

internal class SignupTests {

    private lateinit var context: ConfigurableApplicationContext
    private val dao: R2dbcEntityTemplate by lazy { context.getBean() }
    private val validator: Validator by lazy { context.getBean() }
    private val client: WebTestClient by lazy {
        bindToServer().baseUrl(BASE_URL_DEV).build()
    }

    @BeforeAll
    fun `lance le server en profile test`() {
        context = launcher()
    }

    @AfterAll
    fun `arrête le serveur`() = context.close()

    @AfterEach
    fun tearDown() = deleteAllAccounts(dao)

    @Test
    fun `vérifie que la requête contient bien des données cohérentes`() {
        client
            .post()
            .uri("")
            .contentType(APPLICATION_JSON)
            .bodyValue(defaultAccount)
            .exchange()
            .returnResult<Unit>()
            .requestBodyContent!!
            .map { it.toInt().toChar().toString() }
            .reduce { acc: String, s: String -> acc + s }
            .run {
                defaultAccount.run {
                    setOf(
                        "\"$LOGIN_FIELD\":\"${login}\"",
                        "\"$PASSWORD_FIELD\":\"${password}\"",
                        "\"$FIRST_NAME_FIELD\":\"${firstName}\"",
                        "\"$LAST_NAME_FIELD\":\"${lastName}\"",
                        "\"$EMAIL_FIELD\":\"${email}\"",
                    ).map { assertTrue(contains(it)) }
                }
            }
    }

    @Test //TODO: mock sendmail
    fun `test signup avec un account valide`() {
        val countUserBefore = countAccount(dao)
        val countUserAuthBefore = countAccountAuthority(dao)
        assertEquals(0, countUserBefore)
        assertEquals(0, countUserAuthBefore)
        client.post().uri(SIGNUP_API_PATH).contentType(APPLICATION_JSON).bodyValue(defaultAccount).exchange()
            .expectStatus().isCreated.returnResult<Unit>().responseBodyContent!!.isEmpty().run { assertTrue(this) }
        assertEquals(countUserBefore + 1, countAccount(dao))
        assertEquals(countUserAuthBefore + 1, countAccountAuthority(dao))
        findOneByEmail(defaultAccount.email!!, dao).run {
            assertNotNull(this)
            assertFalse(activated)
            assertNotNull(activationKey)
        }
    }


    @Test
    fun `test signup account avec login invalid`() {
        assertEquals(0, countAccount(dao))
        client
            .post()
            .uri(SIGNUP_API_PATH)
            .contentType(APPLICATION_JSON)
            .bodyValue(defaultAccount.copy(login = "funky-log(n"))
            .exchange()
            .expectStatus()
            .isBadRequest
            .returnResult<Unit>()
            .responseBodyContent!!.isNotEmpty()
            .run { assertTrue(this) }
        assertEquals(0, countAccount(dao))
    }


    @Test
    fun `test signup account avec un email invalid`() {
        val countBefore = countAccount(dao)
        assertEquals(0, countBefore)
        client
            .post()
            .uri(SIGNUP_API_PATH)
            .contentType(APPLICATION_JSON)
            .bodyValue(defaultAccount.copy(password = "inv"))
            .exchange()
            .expectStatus()
            .isBadRequest
            .returnResult<Unit>()
            .responseBodyContent!!
            .isNotEmpty()
            .run { assertTrue(this) }
        assertEquals(0, countBefore)
    }


    @Test
    fun `test signup account avec un password invalid`() {
        val wrongPassword = "123"
        validator
            .validateProperty(AccountCredentials(password = wrongPassword), PASSWORD_FIELD)
            .run {
                assertTrue(isNotEmpty())
                first().run {
                    assertEquals(
                        "{jakarta.validation.constraints.Size.message}",
                        messageTemplate
                    )
                }
            }

        assertEquals(0, countAccount(dao))
        client.post()
            .uri(SIGNUP_API_PATH)
            .contentType(APPLICATION_JSON)
            .bodyValue(defaultAccount.copy(password = wrongPassword))
            .exchange()
            .expectStatus()
            .isBadRequest
            .returnResult<Unit>()
            .responseBodyContent!!
            .logBody()
            .isNotEmpty()
            .run { assertTrue(this) }
        assertEquals(0, countAccount(dao))
    }

    @Test
    fun `test signup account avec un password null`() {
        assertEquals(0, countAccount(dao))
        client.post().uri(SIGNUP_API_PATH).contentType(APPLICATION_JSON).bodyValue(defaultAccount.copy(password = null))
            .exchange().expectStatus().isBadRequest.returnResult<Unit>().responseBodyContent!!.isNotEmpty()
            .run { assertTrue(this) }
        assertEquals(0, countAccount(dao))
    }

    @Test
    fun `test signup account activé avec un email existant`() {
        assertEquals(0, countAccount(dao))
        assertEquals(0, countAccountAuthority(dao))
        //activation de l'account
        createActivatedDataAccounts(setOf(defaultAccount), dao)
        assertEquals(1, countAccount(dao))
        assertEquals(1, countAccountAuthority(dao))
        findOneByEmail(defaultAccount.email!!, dao).run {
            assertNotNull(this)
            assertTrue(activated)
            assertNull(activationKey)
        }

        client
            .post()
            .uri(SIGNUP_API_PATH)
            .contentType(APPLICATION_JSON)
            .bodyValue(defaultAccount.copy(login = "foo"))
            .exchange()
            .expectStatus()
            .isBadRequest
            .returnResult<Unit>()
            .responseBodyContent!!
            .isNotEmpty()
            .run { assertTrue(this) }
    }


    @Test
    fun `test signup account activé avec un login existant`() {
        assertEquals(0, countAccount(dao))
        assertEquals(0, countAccountAuthority(dao))
        //activation de l'account
        createActivatedDataAccounts(setOf(defaultAccount), dao)
        findOneByEmail(defaultAccount.email!!, dao).run {
            assertNotNull(this)
            assertTrue(activated)
            assertNull(activationKey)
        }
        assertEquals(1, countAccount(dao))
        assertEquals(1, countAccountAuthority(dao))

        client
            .post()
            .uri(SIGNUP_API_PATH)
            .contentType(APPLICATION_JSON)
            .bodyValue(defaultAccount.copy(email = "foo@localhost"))
            .exchange()
            .expectStatus()
            .isBadRequest
            .returnResult<Unit>()
            .responseBodyContent!!
            .isNotEmpty()
            .run { assertTrue(this) }
    }

    @Test//TODO: mock sendmail
    fun `test signup account avec un email dupliqué`() {

        assertEquals(0, countAccount(dao))
        assertEquals(0, countAccountAuthority(dao))
        // premier user
        // sign up premier user
        client.post().uri(SIGNUP_API_PATH).contentType(APPLICATION_JSON).bodyValue(defaultAccount).exchange()
            .expectStatus().isCreated.returnResult<Unit>().responseBodyContent!!.isEmpty().run { assertTrue(this) }
        assertEquals(1, countAccount(dao))
        assertEquals(1, countAccountAuthority(dao))
        assertFalse(findOneByEmail(defaultAccount.email!!, dao)!!.activated)

        // email dupliqué, login different
        // sign up un second user (non activé)
        val secondLogin = "foo"
        client.post().uri(SIGNUP_API_PATH).contentType(APPLICATION_JSON)
            .bodyValue(defaultAccount.copy(login = secondLogin))
            .exchange().expectStatus().isCreated.returnResult<Unit>().responseBodyContent!!.isEmpty()
            .run { assertTrue(this) }
        assertEquals(1, countAccount(dao))
        assertEquals(1, countAccountAuthority(dao))
        assertNull(findOneByLogin(defaultAccount.login!!, dao))
        findOneByLogin(secondLogin, dao).run {
            assertNotNull(this)
            assertEquals(defaultAccount.email!!, email)
            assertFalse(activated)
        }

        // email dupliqué - avec un email en majuscule, login différent
        // sign up un troisieme user (non activé)
        val thirdLogin = "bar"
        client.post().uri(SIGNUP_API_PATH).contentType(APPLICATION_JSON).bodyValue(
            defaultAccount.copy(
                login = thirdLogin, email = defaultAccount.email!!.uppercase()
            )
        ).exchange().expectStatus().isCreated.returnResult<Unit>().responseBodyContent!!.isEmpty()
            .run { assertTrue(this) }
        assertEquals(1, countAccount(dao))
        assertEquals(1, countAccountAuthority(dao))
        findOneByLogin(thirdLogin, dao).run {
            assertNotNull(this)
            assertEquals(defaultAccount.email!!, email!!.lowercase())
            assertFalse(activated)
            //activation du troisieme user
            saveAccount(copy(activated = true, activationKey = null), dao)
        }
        //validation que le troisieme est actif et activationKey est null
        findOneByLogin(thirdLogin, dao).run {
            assertNotNull(this)
            assertTrue(activated)
            assertNull(activationKey)
        }
        val fourthLogin = "baz"
        // sign up un quatrieme user avec login different et meme email
        // le user existant au meme mail est deja activé
        client.post().uri(SIGNUP_API_PATH).contentType(APPLICATION_JSON)
            .bodyValue(defaultAccount.copy(login = fourthLogin))
            .exchange().expectStatus().isBadRequest.returnResult<Unit>().responseBodyContent!!.isNotEmpty()
            .run { assertTrue(this) }
        assertEquals(1, countAccount(dao))
        assertEquals(1, countAccountAuthority(dao))
        assertNull(findOneByLogin(fourthLogin, dao))
        //meme id
        assertEquals(findOneByLogin(thirdLogin, dao).apply {
            assertNotNull(this)
            assertTrue(activated)
            assertNull(activationKey)
            assertTrue(defaultAccount.email!!.equals(email!!, true))
        }!!.id, findOneByEmail(defaultAccount.email!!, dao).apply {
            assertNotNull(this)
            assertTrue(activated)
            assertNull(activationKey)
            assertTrue(thirdLogin.equals(login, true))
        }!!.id
        )
    }

    @Test//TODO: mock sendmail
    fun `test signup account en renseignant l'autorité admin qui sera ignoré et le champ activé qui sera mis à false`() {
        val countUserBefore = countAccount(dao)
        val countUserAuthBefore = countAccountAuthority(dao)
        assertEquals(0, countUserBefore)
        assertEquals(0, countUserAuthBefore)
        val login = "badguy"
        client.post().uri(SIGNUP_API_PATH).contentType(APPLICATION_JSON).bodyValue(
            AccountCredentials(
                login = login,
                password = "password",
                firstName = "Bad",
                lastName = "Guy",
                email = "badguy@example.com",
                activated = true,
                imageUrl = "http://placehold.it/50x50",
                langKey = Constants.DEFAULT_LANGUAGE,
                authorities = setOf(Constants.ROLE_ADMIN),
            )
        ).exchange().expectStatus().isCreated.returnResult<Unit>().responseBodyContent.run {
            assertNotNull(this)
            assertTrue(isEmpty())
        }
        assertEquals(countUserBefore + 1, countAccount(dao))
        assertEquals(countUserAuthBefore + 1, countAccountAuthority(dao))
        findOneByLogin(login, dao).run {
            assertNotNull(this)
            assertFalse(activated)
            assertFalse(activationKey.isNullOrBlank())
        }
        assertTrue(findAllAccountAuthority(dao).none {
            it.role.equals(Constants.ROLE_ADMIN, true)
        })
    }


    @Test
    fun `vérifie que la requête activate contient bien des données cohérentes`() {
        AccountUtils.generateActivationKey.run {
            client.get().uri("${Constants.ACTIVATE_API_PATH}${Constants.ACTIVATE_API_PARAM}", this).exchange()
                .returnResult<Unit>().url.let {
                    assertEquals(URI("$BASE_URL_DEV${Constants.ACTIVATE_API_PATH}$this"), it)
                }
        }
    }

    @Test
    fun `test activate avec une mauvaise clé`() {
        client.get().uri(
            "${Constants.ACTIVATE_API_PATH}${Constants.ACTIVATE_API_PARAM}", "wrongActivationKey"
        ).exchange().expectStatus().is5xxServerError.returnResult<Unit>()
    }

    @Test
    fun `test activate avec une clé valide`() {
        assertEquals(0, countAccount(dao))
        assertEquals(0, countAccountAuthority(dao))
        createDataAccounts(setOf(defaultAccount), dao)
        assertEquals(1, countAccount(dao))
        assertEquals(1, countAccountAuthority(dao))

        client.get().uri(
            "${Constants.ACTIVATE_API_PATH}${Constants.ACTIVATE_API_PARAM}",
            findOneByLogin(defaultAccount.login!!, dao)!!.apply {
                assertTrue(activationKey!!.isNotBlank())
                assertFalse(activated)
            }.activationKey
        ).exchange().expectStatus().isOk.returnResult<Unit>()

        findOneByLogin(defaultAccount.login!!, dao)!!.run {
            assertNull(activationKey)
            assertTrue(activated)
        }
    }
}