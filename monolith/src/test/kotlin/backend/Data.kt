@file:Suppress(
    "unused",
    "HttpUrlsUsage",
    "MemberVisibilityCanBePrivate",
    "NonAsciiCharacters"
)

package backend

import backend.Constants.ADMIN
import backend.Constants.DOMAIN_DEV_URL
import backend.Constants.USER
import backend.Data.accounts
import backend.Data.defaultAccount
import backend.Data.defaultAccountJson
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.lang3.StringUtils.stripAccents
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.springframework.beans.factory.getBean
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import kotlin.test.Test


internal object Data {
    val adminAccount by lazy { accountCredentialsFactory(ADMIN) }
    val defaultAccount by lazy { accountCredentialsFactory(USER) }
    val accounts = setOf(adminAccount, defaultAccount)
    const val defaultAccountJson = """{
    "login": "$USER",
    "firstName": "$USER",
    "lastName": "$USER",
    "email": "$USER@$DOMAIN_DEV_URL",
    "password": "$USER",
    "imageUrl": "http://placehold.it/50x50"
}"""
}


fun accountCredentialsFactory(login: String): AccountCredentials =
    AccountCredentials(
        password = login,
        login = login,
        firstName = login,
        lastName = login,
        email = "$login@$DOMAIN_DEV_URL",
        imageUrl = "http://placehold.it/50x50",
    )

fun nameToLogin(userList: List<String>): List<String> = userList.map { s: String ->
    stripAccents(
        s.lowercase().replace(
            ' ',
            '.'
        )
    )
}

@Suppress("unused")
val writers = listOf(
    "Karl Marx",
    "Jean-Jacques Rousseau",
    "Victor Hugo",
    "Platon",
    "René Descartes",
    "Socrate",
    "Homère",
    "Paul Verlaine",
    "Claude Roy",
    "Bernard Friot",
    "François Bégaudeau",
    "Frederic Lordon",
    "Antonio Gramsci",
    "Georg Lukacs",
    "Franz Kafka",
    "Arthur Rimbaud",
    "Gérard de Nerval",
    "Paul Verlaine",
    "Dominique Pagani",
    "Rocé",
    "Chrétien de Troyes",
    "François Rabelais",
    "Montesquieu",
    "Georg Hegel",
    "Friedrich Engels",
    "Voltaire",
    "Michel Clouscard"
)

internal class DataTests {

    private lateinit var context: ConfigurableApplicationContext

    @BeforeAll
    fun `lance le server en profile test`() =
        runApplication<BackendApplication> {
            testLoader(app = this)
        }.run { context = this }

    @AfterAll
    fun `arrête le serveur`() = context.close()


    @Test
    fun `affiche moi du json`() {
        Log.log.info(context.getBean<ObjectMapper>().writeValueAsString(accounts))
        Log.log.info(context.getBean<ObjectMapper>().writeValueAsString(defaultAccount))
        Log.log.info(defaultAccountJson)
    }
}