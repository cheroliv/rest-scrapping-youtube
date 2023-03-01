package webapp.accounts.repository

import jakarta.validation.Validator
import kotlinx.coroutines.reactive.collect
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.dao.DataAccessException
import org.springframework.data.r2dbc.core.*
import org.springframework.data.relational.core.query.Criteria.where
import org.springframework.data.relational.core.query.Query.query
import org.springframework.stereotype.Repository
import webapp.accounts.entities.AccountAuthorityEntity
import webapp.accounts.entities.AccountEntity
import webapp.accounts.entities.AccountRecord
import webapp.accounts.entities.AccountRecord.Companion.EMAIL_FIELD
import webapp.accounts.entities.AccountRecord.Companion.LOGIN_FIELD
import webapp.accounts.models.Account
import webapp.accounts.models.AccountCredentials

@Repository
class AccountRepositoryR2dbc(
    private val dao: R2dbcEntityTemplate,
    private val validator: Validator,
) : AccountRepository {
    override suspend fun save(model: AccountCredentials): Account? =
        try {
            if (model.id != null) dao.update(
                AccountEntity(model).copy(
                    version = dao.selectOne(
                        query(
                            where(LOGIN_FIELD).`is`(model.login!!).ignoreCase(true)
                                .or(where(EMAIL_FIELD).`is`(model.email!!).ignoreCase(true))
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
                where(
                    if (validator.validateProperty(
                            AccountCredentials(email = emailOrLogin),
                            EMAIL_FIELD
                        ).isEmpty()
                    ) EMAIL_FIELD else LOGIN_FIELD
                ).`is`(emailOrLogin).ignoreCase(true)
            )
        ).awaitOneOrNull()
        ?.toCredentialsModel

    private suspend fun withAuthorities(account: AccountCredentials?): AccountCredentials? =
        when {
            account == null -> null
            account.id == null -> null
            else -> account.copy(authorities = mutableSetOf<String>().apply {
                dao.select<AccountAuthorityEntity>()
                    .matching(query(where(AccountRecord.ACCOUNT_AUTH_USER_ID_FIELD).`is`(account.id)))
                    .all()
                    .collect { add(it.role) }
            })
        }

    override suspend fun findOneWithAuthorities(emailOrLogin: String) = withAuthorities(findOne(emailOrLogin))


    override suspend fun findActivationKeyByLogin(login: String): String? =
        dao.select<AccountEntity>()
            .matching(query(where(LOGIN_FIELD).`is`(login).ignoreCase(true)))
            .awaitOneOrNull()
            ?.activationKey


    override suspend fun findOneByActivationKey(key: String): AccountCredentials? =
        dao.selectOne(
            query(where(AccountRecord.ACTIVATION_KEY_FIELD).`is`(key)),
            AccountEntity::class.java
        ).awaitSingleOrNull()
            ?.toCredentialsModel

    override suspend fun findOneByResetKey(key: String) = dao.selectOne(
        query(where(AccountRecord.RESET_KEY_FIELD).`is`(key)),
        AccountEntity::class.java
    ).awaitSingleOrNull() //as  AccountRecord<*>?

    override suspend fun delete(account: Account) {
        if (account.login != null || account.email != null && account.id == null)
            (if (account.login != null) findOne(account.login)
            else if (account.email != null) findOne(account.email)
            else null).run {
                if (this != null) {
                    dao.delete<AccountAuthorityEntity>()
                        .matching(query(where(AccountRecord.ACCOUNT_AUTH_USER_ID_FIELD).`is`(id!!)))
                        .allAndAwait()
                    dao.delete(AccountEntity(this)).awaitSingle()
                }
            }
    }
}