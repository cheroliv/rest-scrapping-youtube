@file:Suppress("NonAsciiCharacters")

package webapp.accounts

import webapp.accounts.*
import webapp.*
import webapp.Constants.DEFAULT_LANGUAGE
import webapp.Constants.ROLE_ADMIN
import webapp.Constants.ROLE_ANONYMOUS
import webapp.Constants.ROLE_USER
import webapp.Constants.SYSTEM_USER
import webapp.Constants.USER
import webapp.accounts.AccountUtils.generateActivationKey
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.springframework.beans.factory.getBean
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import webapp.DataTests.accounts
import webapp.DataTests.defaultAccount
import webapp.accounts.models.*
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class AuthorityRepositoryR2dbcTest {
    private lateinit var context: ConfigurableApplicationContext

    private val authorityRepository: AuthorityRepository by lazy { context.getBean<AuthorityRepositoryR2dbc>() }


    @BeforeAll
    fun `lance le server en profile test`() {
        context = launcher()
    }

    @AfterAll
    fun `arrête le serveur`() = context.close()

    @Test
    fun test_findOne(): Unit = runBlocking {
        mapOf(
            ROLE_ADMIN to ROLE_ADMIN,
            ROLE_USER to ROLE_USER,
            ROLE_ANONYMOUS to ROLE_ANONYMOUS,
            "" to null,
            "foo" to null
        ).map { assertEquals(it.value, authorityRepository.findOne(it.key)) }
    }
}

internal class AccountRepositoryR2dbcTest {
    private lateinit var context: ConfigurableApplicationContext

    private val dao: R2dbcEntityTemplate by lazy { context.getBean() }
    private val accountRepository: AccountRepository by lazy { context.getBean<AccountRepositoryR2dbc>() }


    //    @BeforeAll
//    fun `lance le server en profile test`() = runApplication<WebApplication> {
//        testLoader(this)
//    }.run { context = this }
    @BeforeAll
    fun `lance le server en profile test`() {
        context = launcher()
    }

    @AfterAll
    fun `arrête le serveur`() = context.close()


    @AfterEach
    fun tearDown() = deleteAllAccounts(dao)


    @Test
    fun test_save() {
        mono {
            val countBefore = countAccount(dao)
            assertEquals(0, countBefore)
            accountRepository.save(defaultAccount)
            assertEquals(countBefore + 1, countAccount(dao))
        }
    }

    @Test
    fun test_delete() = runBlocking {
        assertEquals(0, countAccount(dao))
        createDataAccounts(accounts, dao)
        assertEquals(accounts.size, countAccount(dao))
        accountRepository.delete(defaultAccount.toAccount())
        assertEquals(accounts.size - 1, countAccount(dao))
    }

    @Test
    fun test_findOneByEmail() = runBlocking {
        assertEquals(0, countAccount(dao))
        createDataAccounts(accounts, dao)
        assertEquals(accounts.size, countAccount(dao))
        assertEquals(
            defaultAccount.login,
            accountRepository.findOneByEmail(defaultAccount.email!!)!!.login
        )
    }

    @Test
    fun test_findOneByLogin() = runBlocking {
        assertEquals(0, countAccount(dao))
        createDataAccounts(accounts, dao)
        assertEquals(accounts.size, countAccount(dao))
        assertEquals(
            defaultAccount.email,
            accountRepository.findOneByLogin(defaultAccount.login!!)!!.email
        )
    }

    @Test
    fun test_suppress() {
        assertEquals(0, countAccount(dao))
        createDataAccounts(accounts, dao)
        assertEquals(accounts.size, countAccount(dao))
        assertEquals(accounts.size + 1, countAccountAuthority(dao))
        runBlocking {
            accountRepository.delete(findOneByLogin(defaultAccount.login!!, dao)!!.toAccount())
        }
        assertEquals(accounts.size - 1, countAccount(dao))
        assertEquals(accounts.size, countAccountAuthority(dao))
    }

    @Test
    fun test_signup() {
        assertEquals(0, countAccount(dao))
        assertEquals(0, countAccountAuthority(dao))
        runBlocking {
            accountRepository.signup(
                defaultAccount.copy(
                    activationKey = generateActivationKey,
                    langKey = DEFAULT_LANGUAGE,
                    createdBy = SYSTEM_USER,
                    createdDate = Instant.now(),
                    lastModifiedBy = SYSTEM_USER,
                    lastModifiedDate = Instant.now(),
                    authorities = mutableSetOf(ROLE_USER)
                )
            )
        }
        assertEquals(1, countAccount(dao))
        assertEquals(1, countAccountAuthority(dao))
    }

    @Test
    fun test_findActivationKeyByLogin() {
        assertEquals(0, countAccount(dao))
        createDataAccounts(accounts, dao)
        assertEquals(accounts.size, countAccount(dao))
        assertEquals(accounts.size + 1, countAccountAuthority(dao))
        runBlocking {
            assertEquals(
                findOneByEmail(defaultAccount.email!!, dao)!!.activationKey,
                accountRepository.findActivationKeyByLogin(defaultAccount.login!!)
            )
        }
    }

    @Test
    fun test_findOneByActivationKey() {
        assertEquals(0, countAccount(dao))
        createDataAccounts(accounts, dao)
        assertEquals(accounts.size, countAccount(dao))
        assertEquals(accounts.size + 1, countAccountAuthority(dao))
        findOneByLogin(defaultAccount.login!!, dao).run findOneByLogin@{
            assertNotNull(this@findOneByLogin)
            assertNotNull(this@findOneByLogin.activationKey)
            runBlocking {
                accountRepository.findOneByActivationKey(this@findOneByLogin.activationKey!!)
                    .run findOneByActivationKey@{
                        assertNotNull(this@findOneByActivationKey)
                        assertNotNull(this@findOneByActivationKey.id)
                        assertEquals(
                            this@findOneByLogin.id,
                            this@findOneByActivationKey.id
                        )
                    }
            }
        }
    }
}

internal class AccountAuthorityRepositoryR2dbcTest {

    private lateinit var context: ConfigurableApplicationContext

    private val dao: R2dbcEntityTemplate by lazy { context.getBean() }
    private val accountAuthorityRepository: AccountAuthorityRepository by lazy { context.getBean<AccountAuthorityRepositoryR2dbc>() }

    //    @BeforeAll
//    fun `lance le server en profile test`() = runApplication<WebApplication> {
//        testLoader(this)
//    }.run { context = this }
    @BeforeAll
    fun `lance le server en profile test`() {
        context = launcher()
    }

    @AfterAll
    fun `arrête le serveur`() = context.close()


    @AfterEach
    fun tearDown() = deleteAllAccounts(dao)

    @Test
    fun test_save() {
        assertEquals(0, countAccount(dao))
        createDataAccounts(accounts, dao)
        assertEquals(accounts.size, countAccount(dao))
        assertEquals(accounts.size + 1, countAccountAuthority(dao))
        runBlocking {
            accountAuthorityRepository.save(findOneByLogin(USER, dao)!!.id!!, ROLE_ADMIN)
        }
        assertEquals(accounts.size + 2, countAccountAuthority(dao))
    }


    @Test
    fun test_delete() {
        assertEquals(0, countAccount(dao))
        createDataAccounts(accounts, dao)
        assertEquals(accounts.size, countAccount(dao))
        assertEquals(accounts.size + 1, countAccountAuthority(dao))
        runBlocking {
            accountAuthorityRepository.delete(findOneByLogin(USER, dao)!!.id!!, ROLE_USER)
        }
        assertEquals(accounts.size, countAccountAuthority(dao))
    }

    @Test
    fun test_deleteAllByAccountId() {
        assertEquals(0, countAccount(dao))
        createDataAccounts(accounts, dao)
        assertEquals(accounts.size, countAccount(dao))
        assertEquals(accounts.size + 1, countAccountAuthority(dao))
        runBlocking {
            accountAuthorityRepository.deleteAllByAccountId(findOneByLogin(USER, dao)!!.id!!)
        }
        assertEquals(accounts.size, countAccount(dao))
        assertEquals(accounts.size, countAccountAuthority(dao))
    }

//    @Test
//    fun test_deleteAll() {
//        assertEquals(0, countAccount(dao))
//        createDataAccounts(DataTests.accounts, dao)
//        assertEquals(DataTests.accounts.size, countAccount(dao))
//        assertEquals(DataTests.accounts.size + 1, countAccountAuthority(dao))
//        runBlocking {
//            accountAuthorityRepository.deleteAll()
//        }
//        assertEquals(DataTests.accounts.size, countAccount(dao))
//        assertEquals(0, countAccountAuthority(dao))
//    }
}