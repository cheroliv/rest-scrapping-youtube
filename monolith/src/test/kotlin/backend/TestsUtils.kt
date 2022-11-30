@file:Suppress("unused")

package backend

import backend.Constants.ADMIN
import backend.Constants.DEFAULT_LANGUAGE
import backend.Constants.ROLE_ADMIN
import backend.Constants.ROLE_USER
import backend.Constants.SPRING_PROFILE_CONF_DEFAULT_KEY
import backend.Constants.SPRING_PROFILE_TEST
import backend.Constants.SYSTEM_USER
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Description
import org.hamcrest.TypeSafeDiagnosingMatcher
import org.springframework.boot.SpringApplication
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.r2dbc.core.select
import org.springframework.data.relational.core.query.Criteria.where
import org.springframework.data.relational.core.query.Query.query
import java.io.IOException
import java.lang.Byte.parseByte
import java.time.Instant
import java.time.ZonedDateTime
import java.time.ZonedDateTime.parse
import java.time.format.DateTimeParseException
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.test.assertEquals
import kotlin.test.assertTrue

const val BASE_URL_DEV = "http://localhost:8080"

fun testLoader(app: SpringApplication) = with(app) {
    setDefaultProperties(
        hashMapOf<String, Any>().apply {
            set(
                SPRING_PROFILE_CONF_DEFAULT_KEY,
                SPRING_PROFILE_TEST
            )
        })
    setAdditionalProfiles(SPRING_PROFILE_TEST)
}

fun createDataAccounts(accounts: Set<AccountCredentials>, dao: R2dbcEntityTemplate) {
    assertEquals(0, countAccount(dao))
    assertEquals(0, countAccountAuthority(dao))
    accounts.map { acc ->
        AccountEntity(acc.copy(
            activationKey = RandomUtils.generateActivationKey,
            langKey = DEFAULT_LANGUAGE,
            createdBy = SYSTEM_USER,
            createdDate = Instant.now(),
            lastModifiedBy = SYSTEM_USER,
            lastModifiedDate = Instant.now(),
            authorities = mutableSetOf(ROLE_USER).apply {
                if (acc.login == ADMIN) add(ROLE_ADMIN)
            }
        )).run {
            dao.insert(this).block()!!.id!!.let { uuid ->
                authorities!!.map { authority ->
                    dao.insert(AccountAuthorityEntity(userId = uuid, role = authority.role)).block()
                }
            }
        }
    }
    assertEquals(accounts.size, countAccount(dao))
    assertTrue(accounts.size <= countAccountAuthority(dao))
}


fun createActivatedDataAccounts(accounts: Set<AccountCredentials>, dao: R2dbcEntityTemplate) {
    assertEquals(0, countAccount(dao))
    assertEquals(0, countAccountAuthority(dao))
    accounts.map { acc ->
        AccountEntity(acc.copy(
            activated = true,
            langKey = DEFAULT_LANGUAGE,
            createdBy = SYSTEM_USER,
            createdDate = Instant.now(),
            lastModifiedBy = SYSTEM_USER,
            lastModifiedDate = Instant.now(),
            authorities = mutableSetOf(ROLE_USER).apply {
                if (acc.login == ADMIN) add(ROLE_ADMIN)
            }.toSet()
        )).run {
            dao.insert(this).block()!!.id!!.let { uuid ->
                authorities!!.map { authority ->
                    dao.insert(AccountAuthorityEntity(userId = uuid, role = authority.role)).block()
                }
            }
        }
    }
    assertEquals(accounts.size, countAccount(dao))
    assertTrue(accounts.size <= countAccountAuthority(dao))
}

fun deleteAllAccounts(dao: R2dbcEntityTemplate) {
    deleteAllAccountAuthority(dao)
    deleteAccounts(dao)
    assertEquals(0, countAccount(dao))
    assertEquals(0, countAccountAuthority(dao))
}

fun deleteAccounts(repository: R2dbcEntityTemplate) {
    repository.delete(AccountEntity::class.java).all().block()
}

fun deleteAllAccountAuthority(dao: R2dbcEntityTemplate) {
    dao.delete(AccountAuthorityEntity::class.java).all().block()
}

//TODO: revoir les updates avec id!=null
fun saveAccount(model: AccountCredentials, dao: R2dbcEntityTemplate): Account? =
    when {
        model.id != null -> dao.update(
            AccountEntity(model).copy(
                version = dao.selectOne(
                    query(where("login").`is`(model.login!!).ignoreCase(true)),
                    AccountEntity::class.java
                ).block()!!.version
            )
        ).block()?.toModel

        else -> dao.insert(AccountEntity(model)).block()?.toModel
    }

fun saveAccountAuthority(id: UUID, role: String, dao: R2dbcEntityTemplate): AccountAuthorityEntity? =
    dao.insert(AccountAuthorityEntity(userId = id, role = role)).block()


fun countAccount(dao: R2dbcEntityTemplate): Int =
    dao.select(AccountEntity::class.java).count().block()?.toInt()!!


fun countAccountAuthority(dao: R2dbcEntityTemplate): Int =
    dao.select(AccountAuthorityEntity::class.java).count().block()?.toInt()!!


fun findOneByLogin(login: String, dao: R2dbcEntityTemplate): AccountCredentials? =
    dao.select<AccountEntity>()
        .matching(query(where("login").`is`(login).ignoreCase(true)))
        .one().block()?.toCredentialsModel

fun findOneByEmail(email: String, dao: R2dbcEntityTemplate): AccountCredentials? = dao.select<AccountEntity>()
    .matching(query(where("email").`is`(email).ignoreCase(true)))
    .one().block()?.toCredentialsModel

fun findAllAccountAuthority(dao: R2dbcEntityTemplate): Set<AccountAuthorityEntity> =
    dao.select(AccountAuthorityEntity::class.java).all().toIterable().toHashSet()

private fun createObjectMapper() =
    ObjectMapper().apply {
        configure(WRITE_DURATIONS_AS_TIMESTAMPS, false)
        setSerializationInclusion(NON_EMPTY)
        registerModule(JavaTimeModule())
    }

/**
 * Convert an object to JSON byte array.
 *
 * @param object the object to convert.
 * @return the JSON byte array.
 * @throws IOException
 */
@Throws(IOException::class)
fun convertObjectToJsonBytes(`object`: Any): ByteArray = createObjectMapper().writeValueAsBytes(`object`)

/**
 * Create a byte array with a specific size filled with specified data.
 *
 * @param size the size of the byte array.
 * @param data the data to put in the byte array.
 * @return the JSON byte array.
 */
fun createByteArray(size: Int, data: String) = ByteArray(size) { parseByte(data, 2) }

/**
 * A matcher that tests that the examined string represents the same instant as the reference datetime.
 */
class ZonedDateTimeMatcher(private val date: ZonedDateTime) : TypeSafeDiagnosingMatcher<String>() {

    override fun matchesSafely(item: String, mismatchDescription: Description): Boolean {
        try {
            if (!date.isEqual(parse(item))) {
                mismatchDescription.appendText("was ").appendValue(item)
                return false
            }
            return true
        } catch (e: DateTimeParseException) {
            mismatchDescription.appendText("was ")
                .appendValue(item)
                .appendText(", which could not be parsed as a ZonedDateTime")
            return false
        }
    }

    override fun describeTo(description: Description) {
        description.appendText("a String representing the same Instant as ")
            .appendValue(date)
    }
}

/**
 * Creates a matcher that matches when the examined string represents the same instant as the reference datetime.
 * @param date the reference datetime against which the examined string is checked.
 */
fun sameInstant(date: ZonedDateTime) = ZonedDateTimeMatcher(date)

/**
 * Verifies the equals/hashcode contract on the domain object.
 */
fun <T : Any> equalsVerifier(clazz: KClass<T>) {
    clazz.createInstance().apply i@{
        assertThat(toString()).isNotNull
        assertThat(this).isEqualTo(this)
        assertThat(hashCode()).isEqualTo(hashCode())
        // Test with an instance of another class
        assertThat(this).isNotEqualTo(Any())
        assertThat(this).isNotEqualTo(null)
        // Test with an instance of the same class
        clazz.createInstance().apply j@{
            assertThat(this@i).isNotEqualTo(this@j)
            // HashCodes are equals because the objects are not persisted yet
            assertThat(this@i.hashCode()).isEqualTo(this@j.hashCode())
        }
    }
}

