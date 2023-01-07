package webapp.accounts.repository

import kotlinx.coroutines.reactive.collect
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.dao.DataAccessException
import org.springframework.data.r2dbc.core.*
import org.springframework.data.relational.core.query.Criteria.where
import org.springframework.data.relational.core.query.Query.query
import org.springframework.stereotype.Repository
import webapp.accounts.models.Account
import webapp.accounts.models.AccountCredentials
import webapp.accounts.models.AccountCredentials.Companion.isValidEmail
import webapp.accounts.models.entities.AccountAuthorityEntity
import webapp.accounts.models.entities.AccountEntity
import webapp.accounts.models.entities.AuthorityEntity
import webapp.accounts.models.entities.AuthorityRecord


@Repository
class AccountRepositoryR2dbc(
    private val dao: R2dbcEntityTemplate
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


    override suspend fun findOne(emailOrLogin: String): AccountCredentials? = dao
        .select<AccountEntity>()
        .matching(
            query(
                where(if (isValidEmail(emailOrLogin)) "email" else "login")
                    .`is`(emailOrLogin)
                    .ignoreCase(true)
            )
        ).awaitOneOrNull()
        ?.toCredentialsModel

    private suspend fun withAuthorities(ac: AccountCredentials?): AccountCredentials? = if (ac == null) null
    else if (ac.id == null) null
    else ac.copy(authorities = mutableSetOf<String>().apply {
        dao.select<AccountAuthorityEntity>()
            .matching(query(where("userId").`is`(ac.id)))
            .all()
            .collect { add(it.role) }
    })

    override suspend fun findOneWithAuthorities(emailOrLogin: String): AccountCredentials? =
        findOne(emailOrLogin).run { return@run withAuthorities(this) }

    override suspend fun signup(model: AccountCredentials) {
        AccountEntity(model).run {
            dao.insert(this).awaitSingleOrNull()?.id.run id@{
                if (this@id != null) model.authorities!!.map { modelRole ->
                    dao.selectOne(
                        query(where(AuthorityRecord.ROLE_COLUMN).`is`(modelRole)),
                        AuthorityEntity::class.java
                    )
                        .awaitSingleOrNull().run auth@{
                            if (this@auth != null) if (!role.isNullOrBlank())
                                dao.insert(AccountAuthorityEntity(userId = this@id, role = role))
                                    .awaitSingleOrNull()
                        }
                }
            }
        }
    }

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

    override suspend fun delete(account: Account) {
        if (account.login != null || account.email != null && account.id == null)
            (if (account.login != null) findOne(account.login)
            else if (account.email != null) findOne(account.email)
            else null).run {
                if (this != null) {
                    dao.delete<AccountAuthorityEntity>()
                        .matching(query(where("userId").`is`(id!!)))
                        .allAndAwait()
                    dao.delete(AccountEntity(this)).awaitSingle()
                }
            }
    }
}