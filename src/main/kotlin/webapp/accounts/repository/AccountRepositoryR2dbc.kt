package webapp.accounts.repository

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.dao.DataAccessException
import org.springframework.data.r2dbc.core.*
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query
import org.springframework.stereotype.Repository
import webapp.accounts.models.Account
import webapp.accounts.models.AccountCredentials
import webapp.accounts.models.entities.AccountAuthorityEntity
import webapp.accounts.models.entities.AccountEntity
import webapp.accounts.models.entities.AuthorityEntity
import webapp.accounts.models.entities.AuthorityRecord

@Repository
class AccountRepositoryR2dbc(
    val dao: R2dbcEntityTemplate
) : AccountRepository {
    override suspend fun save(model: AccountCredentials): Account? =
        try {
            if (model.id != null) dao.update(
                AccountEntity(model).copy(
                    version = dao.selectOne(
                        Query.query(
                            Criteria.where("login").`is`(model.login!!).ignoreCase(true)
                                .or(Criteria.where("email").`is`(model.email!!).ignoreCase(true))
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
                        .matching(Query.query(Criteria.where("userId").`is`(id!!)))
                        .allAndAwait()
                    dao.delete(AccountEntity(this)).awaitSingle()
                }
            }
    }

    override suspend fun findOneByLogin(login: String): AccountCredentials? =
        dao.select<AccountEntity>().matching(Query.query(Criteria.where("login").`is`(login).ignoreCase(true)))
            .awaitOneOrNull()
            ?.toCredentialsModel


    override suspend fun findOneByEmail(email: String): AccountCredentials? =
        dao.select<AccountEntity>().matching(Query.query(Criteria.where("email").`is`(email).ignoreCase(true)))
            .awaitOneOrNull()
            ?.toCredentialsModel


    override suspend fun findActivationKeyByLogin(login: String): String? =
        dao.select<AccountEntity>().matching(Query.query(Criteria.where("login").`is`(login).ignoreCase(true)))
            .awaitOneOrNull()
            ?.activationKey


    override suspend fun findOneByActivationKey(key: String): AccountCredentials? =
        dao.selectOne(
            Query.query(Criteria.where("activationKey").`is`(key)),
            AccountEntity::class.java
        ).awaitSingleOrNull()
            ?.toCredentialsModel

    override suspend fun findOneByResetKey(key: String) = dao.selectOne(
        Query.query(Criteria.where("resetKey").`is`(key)),
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
                dao.insert(this).awaitSingleOrNull()?.id.run id@{
                    if (this@id != null) model.authorities!!.map { modelRole ->
                        dao.selectOne(Query.query(Criteria.where(AuthorityRecord.ROLE_COLUMN).`is`(modelRole)), AuthorityEntity::class.java)
                            .awaitSingleOrNull().run auth@{
                                if (this@auth != null) if (!role.isNullOrBlank())
                                    dao.insert(AccountAuthorityEntity(userId = this@id, role = role))
                                        .awaitSingleOrNull()
                            }
                    }
                }
            }
    }
}