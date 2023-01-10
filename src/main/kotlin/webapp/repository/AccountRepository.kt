package webapp.repository

import webapp.models.Account
import webapp.models.AccountCredentials
import webapp.models.entities.AccountRecord

interface AccountRepository {
    suspend fun save(model: AccountCredentials): Account?
    suspend fun findOne(emailOrLogin: String): AccountCredentials?
    suspend fun findOneWithAuthorities(emailOrLogin: String): AccountCredentials?
    suspend fun findOneByActivationKey(key: String): AccountCredentials?
    suspend fun findOneByResetKey(key: String): AccountRecord<*>?//<AuthorityRecord>?
    suspend fun findActivationKeyByLogin(login: String): String?
    suspend fun signup(model: AccountCredentials)
    suspend fun delete(account: Account)
}