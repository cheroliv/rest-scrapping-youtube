@file:Suppress("NonAsciiCharacters")

package backend

import backend.Constants.DEFAULT_LANGUAGE
import backend.Constants.ROLE_ADMIN
import backend.Constants.ROLE_ANONYMOUS
import backend.Constants.ROLE_USER
import backend.Constants.SYSTEM_USER
import backend.Constants.USER
import backend.RandomUtils.generateActivationKey
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.springframework.beans.factory.getBean
import org.springframework.boot.runApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class AuthorityRepositoryR2dbcTest {
    private lateinit var context: ConfigurableApplicationContext

    private val authorityRepository: AuthorityRepository by lazy { context.getBean<AuthorityRepositoryR2dbc>() }

    @BeforeAll
    fun `lance le server en profile test`() = runApplication<BackendApplication> {
        testLoader(app = this)
    }.run { context = this }

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

    @BeforeAll
    fun `lance le server en profile test`() = runApplication<BackendApplication> {
        testLoader(this)
    }.run { context = this }

    @AfterAll
    fun `arrête le serveur`() = context.close()


    @AfterEach
    fun tearDown() = deleteAllAccounts(dao)


    @Test
    fun test_save() {
        mono {
            val countBefore = countAccount(dao)
            assertEquals(0, countBefore)
            accountRepository.save(Data.defaultAccount)
            assertEquals(countBefore + 1, countAccount(dao))
        }
    }

    @Test
    fun test_count() = runBlocking {
        assertEquals(0, accountRepository.count())
        assertEquals(0, countAccount(dao))
        createDataAccounts(Data.accounts, dao)
        assertEquals(Data.accounts.size, countAccount(dao))
        assertEquals(Data.accounts.size.toLong(), accountRepository.count())
    }

    @Test
    fun test_delete() = runBlocking {
        assertEquals(0, countAccount(dao))
        createDataAccounts(Data.accounts, dao)
        assertEquals(Data.accounts.size, countAccount(dao))
        accountRepository.delete(Data.defaultAccount.toAccount())
        assertEquals(Data.accounts.size - 1, countAccount(dao))
    }

    @Test
    fun test_findOneByEmail() = runBlocking {
        assertEquals(0, countAccount(dao))
        createDataAccounts(Data.accounts, dao)
        assertEquals(Data.accounts.size, countAccount(dao))
        assertEquals(
            Data.defaultAccount.login,
            accountRepository.findOneByEmail(Data.defaultAccount.email!!)!!.login
        )
    }

    @Test
    fun test_findOneByLogin() = runBlocking {
        assertEquals(0, countAccount(dao))
        createDataAccounts(Data.accounts, dao)
        assertEquals(Data.accounts.size, countAccount(dao))
        assertEquals(
            Data.defaultAccount.email,
            accountRepository.findOneByLogin(Data.defaultAccount.login!!)!!.email
        )
    }

    @Test
    fun test_suppress() {
        assertEquals(0, countAccount(dao))
        createDataAccounts(Data.accounts, dao)
        assertEquals(Data.accounts.size, countAccount(dao))
        assertEquals(Data.accounts.size + 1, countAccountAuthority(dao))
        runBlocking {
            accountRepository.suppress(findOneByLogin(Data.defaultAccount.login!!, dao)!!.toAccount())
        }
        assertEquals(Data.accounts.size - 1, countAccount(dao))
        assertEquals(Data.accounts.size, countAccountAuthority(dao))
    }

    @Test
    fun test_signup() {
        assertEquals(0, countAccount(dao))
        assertEquals(0, countAccountAuthority(dao))
        runBlocking {
            accountRepository.signup(
                Data.defaultAccount.copy(
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
        createDataAccounts(Data.accounts, dao)
        assertEquals(Data.accounts.size, countAccount(dao))
        assertEquals(Data.accounts.size + 1, countAccountAuthority(dao))
        runBlocking {
            assertEquals(
                findOneByEmail(Data.defaultAccount.email!!, dao)!!.activationKey,
                accountRepository.findActivationKeyByLogin(Data.defaultAccount.login!!)
            )
        }
    }

    @Test
    fun test_findOneByActivationKey() {
        assertEquals(0, countAccount(dao))
        createDataAccounts(Data.accounts, dao)
        assertEquals(Data.accounts.size, countAccount(dao))
        assertEquals(Data.accounts.size + 1, countAccountAuthority(dao))
        findOneByLogin(Data.defaultAccount.login!!, dao).run findOneByLogin@{
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

    @BeforeAll
    fun `lance le server en profile test`() = runApplication<BackendApplication> {
        testLoader(this)
    }.run { context = this }

    @AfterAll
    fun `arrête le serveur`() = context.close()


    @AfterEach
    fun tearDown() = deleteAllAccounts(dao)

    @Test
    fun test_save() {
        assertEquals(0, countAccount(dao))
        createDataAccounts(Data.accounts, dao)
        assertEquals(Data.accounts.size, countAccount(dao))
        assertEquals(Data.accounts.size + 1, countAccountAuthority(dao))
        runBlocking {
            accountAuthorityRepository.save(findOneByLogin(USER, dao)!!.id!!, ROLE_ADMIN)
        }
        assertEquals(Data.accounts.size + 2, countAccountAuthority(dao))
    }

    @Test
    fun test_count() {
        runBlocking {
            assertEquals(0, accountAuthorityRepository.count())
            createDataAccounts(Data.accounts, dao)
            assertEquals(
                Data.accounts.size.toLong() + 1,
                accountAuthorityRepository.count()
            )
        }
    }

    @Test
    fun test_delete() {
        assertEquals(0, countAccount(dao))
        createDataAccounts(Data.accounts, dao)
        assertEquals(Data.accounts.size, countAccount(dao))
        assertEquals(Data.accounts.size + 1, countAccountAuthority(dao))
        runBlocking {
            accountAuthorityRepository.delete(findOneByLogin(USER, dao)!!.id!!, ROLE_USER)
        }
        assertEquals(Data.accounts.size, countAccountAuthority(dao))
    }

    @Test
    fun test_deleteAllByAccountId() {
        assertEquals(0, countAccount(dao))
        createDataAccounts(Data.accounts, dao)
        assertEquals(Data.accounts.size, countAccount(dao))
        assertEquals(Data.accounts.size + 1, countAccountAuthority(dao))
        runBlocking {
            accountAuthorityRepository.deleteAllByAccountId(findOneByLogin(USER, dao)!!.id!!)
        }
        assertEquals(Data.accounts.size, countAccount(dao))
        assertEquals(Data.accounts.size, countAccountAuthority(dao))
    }

//    @Test
//    fun test_deleteAll() {
//        assertEquals(0, countAccount(dao))
//        createDataAccounts(Data.accounts, dao)
//        assertEquals(Data.accounts.size, countAccount(dao))
//        assertEquals(Data.accounts.size + 1, countAccountAuthority(dao))
//        runBlocking {
//            accountAuthorityRepository.deleteAll()
//        }
//        assertEquals(Data.accounts.size, countAccount(dao))
//        assertEquals(0, countAccountAuthority(dao))
//    }
}