@file:Suppress(
    "unused",
    "FunctionName",
    "RedundantUnitReturnType"
)

package webapp.accounts


import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.dao.DataAccessException
import org.springframework.data.r2dbc.core.*
import org.springframework.data.relational.core.query.Criteria.where
import org.springframework.data.relational.core.query.Query.query
import org.springframework.stereotype.Repository
import webapp.accounts.models.Account
import webapp.accounts.models.AccountCredentials
import webapp.accounts.models.entities.AccountAuthorityEntity
import webapp.accounts.models.entities.AccountEntity
import webapp.accounts.models.entities.AccountRecord
import webapp.accounts.models.entities.AuthorityEntity
import webapp.accounts.models.entities.AuthorityRecord.Companion.ROLE_COLUMN
import java.util.*


/*=================================================================================*/

interface AuthorityRepository {
    suspend fun findOne(role: String): String?
    suspend fun findAll(): List<String>
}

/*=================================================================================*/

interface AccountRepository {
    suspend fun findOneByLogin(login: String): AccountCredentials?

    suspend fun findOneByEmail(email: String): AccountCredentials?

    suspend fun save(model: AccountCredentials): Account?

    suspend fun delete(account: Account)

    suspend fun findActivationKeyByLogin(login: String): String?
    suspend fun findOneByActivationKey(key: String): AccountCredentials?
    suspend fun findOneByResetKey(key: String): AccountRecord<*>?//<AuthorityRecord>?

    suspend fun findOneByEmailWithAuthorities(email: String): AccountCredentials?

    suspend fun findOneByLoginWithAuthorities(login: String): AccountCredentials?
    suspend fun signup(model: AccountCredentials)
}
/*=================================================================================*/

interface AccountAuthorityRepository {
    suspend fun save(id: UUID, authority: String): Unit

    suspend fun delete(id: UUID, authority: String): Unit

    suspend fun deleteAllByAccountId(id: UUID): Unit
}

/*=================================================================================*/


@Repository
class AccountAuthorityRepositoryR2dbc(
    private val dao: R2dbcEntityTemplate
) : AccountAuthorityRepository {
    override suspend fun save(id: UUID, authority: String) {
        dao.insert(AccountAuthorityEntity(userId = id, role = authority))
            .awaitSingle()
    }

    override suspend fun delete(id: UUID, authority: String) {
        dao.selectOne(
            query(where("userId").`is`(id).and(where("role").`is`(authority).ignoreCase(true))),
            AccountAuthorityEntity::class.java
        ).awaitSingleOrNull().run {
            if (this != null && this.id != null)
                dao.delete(this)
                    .awaitSingle()
        }
    }

    override suspend fun deleteAllByAccountId(id: UUID) {
        dao.delete<AccountAuthorityEntity>().matching(query(where("userId").`is`(id)))
            .allAndAwait()
    }
}


/*=================================================================================*/

@Repository
class AccountRepositoryR2dbc(
    val dao: R2dbcEntityTemplate
) : AccountRepository {
    override suspend fun save(model: AccountCredentials): Account? =
        try {
            if (model.id != null) dao.update(
                AccountEntity(model).copy(
                    version = dao.selectOne(
                        query(
                            where("login").`is`(model.login!!).ignoreCase(true)
                                .or(where("email").`is`(model.email!!).ignoreCase(true))
                        ), AccountEntity::class.java
                    ).awaitSingle()!!.version
                )
            ).awaitSingle()?.toModel
            else dao.insert(AccountEntity(model)).awaitSingle()?.toModel
        } catch (_: DataAccessException) {
            null
        } catch (_: NoSuchElementException) {
            null
        }


    override suspend fun delete(account: Account) {
        if (account.login != null || account.email != null && account.id == null)
            (if (account.login != null) findOneByLogin(account.login)
            else if (account.email != null) findOneByEmail(account.email)
            else null).run {
                if (this != null) {
                    dao.delete<AccountAuthorityEntity>()
                        .matching(query(where("userId").`is`(id!!)))
                        .allAndAwait()
                    dao.delete(AccountEntity(this)).awaitSingle()
                }
            }
    }

    override suspend fun findOneByLogin(login: String): AccountCredentials? =
        dao.select<AccountEntity>().matching(query(where("login").`is`(login).ignoreCase(true)))
            .awaitOneOrNull()
            ?.toCredentialsModel


    override suspend fun findOneByEmail(email: String): AccountCredentials? =
        dao.select<AccountEntity>().matching(query(where("email").`is`(email).ignoreCase(true)))
            .awaitOneOrNull()
            ?.toCredentialsModel


    override suspend fun findActivationKeyByLogin(login: String): String? =
        dao.select<AccountEntity>().matching(query(where("login").`is`(login).ignoreCase(true)))
            .awaitOneOrNull()
            ?.activationKey


    override suspend fun findOneByActivationKey(key: String): AccountCredentials? =
        dao.selectOne(
            query(where("activationKey").`is`(key)),
            AccountEntity::class.java
        ).awaitSingleOrNull()
            ?.toCredentialsModel

    override suspend fun findOneByResetKey(key: String) = dao.selectOne(
        query(where("resetKey").`is`(key)),
        AccountEntity::class.java
    ).awaitSingleOrNull() //as  AccountRecord<*>?

    override suspend fun findOneByEmailWithAuthorities(email: String): AccountCredentials? {
        TODO("Not yet implemented")
    }

    override suspend fun findOneByLoginWithAuthorities(login: String): AccountCredentials? {
        TODO("Not yet implemented")
    }

    override suspend fun signup(model: AccountCredentials) {
            AccountEntity(model).run {
                dao.insert(this).awaitSingleOrNull()?.id.run userId@{
                    if (this != null) model.authorities!!.map { modelRole ->
                        dao.selectOne(query(where("role").`is`(modelRole)), AuthorityEntity::class.java)
                            .awaitSingleOrNull().run {
                                if (this != null) if (!role.isNullOrBlank())
                                    dao.insert(AccountAuthorityEntity(userId = this@userId, role = role))
                                        .awaitSingleOrNull()
                            }
                    }
                }
            }
    }
}

/*=================================================================================*/
@Repository
class AuthorityRepositoryR2dbc(
    private val dao: R2dbcEntityTemplate
) : AuthorityRepository {
    override suspend fun findOne(role: String): String? =
        dao.select(AuthorityEntity::class.java)
            .matching(query(where(ROLE_COLUMN).`is`(role)))
            .awaitOneOrNull()?.role

    override suspend fun findAll(): List<String> =
        dao.select(AuthorityEntity::class.java)
            .flow()
            .toList()
            .map(AuthorityEntity::role)
}
/*=================================================================================*/