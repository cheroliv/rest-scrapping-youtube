package webapp.accounts.repository

import webapp.accounts.models.Account
import webapp.accounts.models.AccountCredentials
import webapp.accounts.models.entities.AccountRecord

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