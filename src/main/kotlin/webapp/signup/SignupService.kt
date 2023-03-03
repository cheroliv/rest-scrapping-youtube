package webapp.signup

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import webapp.accounts.models.Account
import webapp.accounts.models.AccountCredentials
import webapp.accounts.repository.AccountRepository
import webapp.mail.MailService

@Service
class SignupService(
    private val accountRepository: AccountRepository,
    private val mailService: MailService,
    private val passwordEncoder: PasswordEncoder
) {
    fun encode(password: String?): String? = passwordEncoder.encode(password)
    @Transactional
    suspend fun signup(account: AccountCredentials) = account.run {
        accountRepository.signup(this)
        /*
        copy(
                    password = signupService.encode(password),
                    activationKey = generateActivationKey,
                    authorities = setOf(ROLE_USER),
                    langKey = when {
                        langKey.isNullOrBlank() -> ENGLISH.language
                        else -> langKey
                    },
                    activated = false,
                    createdBy = SYSTEM_USER,
                    createdDate = this@run,
                    lastModifiedBy = SYSTEM_USER,
                    lastModifiedDate = this@run
                )
         */
        mailService.sendActivationEmail(this)
    }

    @Transactional(readOnly=true)
    suspend fun findOneByActivationKey(key: String): AccountCredentials? = accountRepository.findOneByActivationKey(key)

    @Transactional(readOnly=true)
    suspend fun findOne(emailOrLogin: String): AccountCredentials? = accountRepository.findOne(emailOrLogin)

    @Transactional
    suspend fun save(account: AccountCredentials): Account? = accountRepository.save(account)

    @Transactional
    suspend fun delete(account: Account) = accountRepository.delete(account)
}