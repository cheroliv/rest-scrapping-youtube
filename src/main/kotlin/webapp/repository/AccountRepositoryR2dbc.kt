package webapp.repository

import kotlinx.coroutines.reactive.collect
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.dao.DataAccessException
import org.springframework.data.r2dbc.core.*
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query
import org.springframework.stereotype.Repository
import webapp.Constants
import webapp.models.Account
import webapp.models.AccountCredentials
import webapp.models.AccountCredentials.Companion.isValidEmail
import webapp.models.entities.AccountAuthorityEntity
import webapp.models.entities.AccountEntity
import webapp.models.entities.AccountRecord

@Repository
class AccountRepositoryR2dbc(
    private val dao: R2dbcEntityTemplate,
) : AccountRepository {
    override suspend fun save(model: AccountCredentials): Account? =
        try {
            if (model.id != null) dao.update(
                AccountEntity(model).copy(
                    version = dao.selectOne(
                        Query.query(
                            Criteria.where(AccountRecord.LOGIN_FIELD).`is`(model.login!!).ignoreCase(true)
                                .or(Criteria.where(AccountRecord.EMAIL_FIELD).`is`(model.email!!).ignoreCase(true))
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

    override suspend fun signup(accountCredentials: AccountCredentials) {
        dao.insert(AccountEntity(accountCredentials))
            .awaitSingleOrNull()
            ?.id
            .run {
                if (this != null) dao.insert(
                    AccountAuthorityEntity(
                        userId = this,
                        role = Constants.ROLE_USER
                    )
                ).awaitSingleOrNull()
            }
    }

    override suspend fun findOne(emailOrLogin: String): AccountCredentials? = dao
        .select<AccountEntity>()
        .matching(
            Query.query(
                Criteria.where(if (emailOrLogin.isValidEmail()) AccountRecord.EMAIL_FIELD else AccountRecord.LOGIN_FIELD)
                    .`is`(emailOrLogin)
                    .ignoreCase(true)
            )
        ).awaitOneOrNull()
        ?.toCredentialsModel

    private suspend fun withAuthorities(ac: AccountCredentials?): AccountCredentials? =
        when {
            ac == null -> null
            ac.id == null -> null
            else -> ac.copy(authorities = mutableSetOf<String>().apply {
                dao.select<AccountAuthorityEntity>()
                    .matching(Query.query(Criteria.where(AccountRecord.ACCOUNT_AUTH_USER_ID_FIELD).`is`(ac.id)))
                    .all()
                    .collect { add(it.role) }
            })
        }

    override suspend fun findOneWithAuthorities(emailOrLogin: String): AccountCredentials? =
        findOne(emailOrLogin).run { return@run withAuthorities(this) }




    override suspend fun findActivationKeyByLogin(login: String): String? =
        dao.select<AccountEntity>().matching(
            Query.query(
                Criteria.where(AccountRecord.LOGIN_FIELD).`is`(login).ignoreCase(true)
            )
        )
            .awaitOneOrNull()
            ?.activationKey


    override suspend fun findOneByActivationKey(key: String): AccountCredentials? =
        dao.selectOne(
            Query.query(Criteria.where(AccountRecord.ACTIVATION_KEY_FIELD).`is`(key)),
            AccountEntity::class.java
        ).awaitSingleOrNull()
            ?.toCredentialsModel

    override suspend fun findOneByResetKey(key: String) = dao.selectOne(
        Query.query(Criteria.where(AccountRecord.RESET_KEY_FIELD).`is`(key)),
        AccountEntity::class.java
    ).awaitSingleOrNull() //as  AccountRecord<*>?

    override suspend fun delete(account: Account) {
        if (account.login != null || account.email != null && account.id == null)
            (if (account.login != null) findOne(account.login)
            else if (account.email != null) findOne(account.email)
            else null).run {
                if (this != null) {
                    dao.delete<AccountAuthorityEntity>()
                        .matching(Query.query(Criteria.where(AccountRecord.ACCOUNT_AUTH_USER_ID_FIELD).`is`(id!!)))
                        .allAndAwait()
                    dao.delete(AccountEntity(this)).awaitSingle()
                }
            }
    }
}