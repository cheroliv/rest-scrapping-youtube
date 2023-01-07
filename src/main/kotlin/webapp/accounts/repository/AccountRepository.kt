package webapp.accounts.repository

import webapp.accounts.models.Account
import webapp.accounts.models.AccountCredentials
import webapp.accounts.models.entities.AccountRecord

interface AccountRepository {
    suspend fun save(model: AccountCredentials): Account?
    suspend fun findOne(emailOrLogin: String): AccountCredentials?
    suspend fun findOneWithAuthorities(emailOrLogin: String): AccountCredentials?
    suspend fun signup(model: AccountCredentials)

    suspend fun findActivationKeyByLogin(login: String): String?
    suspend fun findOneByActivationKey(key: String): AccountCredentials?
    suspend fun findOneByResetKey(key: String): AccountRecord<*>?//<AuthorityRecord>?
    suspend fun delete(account: Account)

}