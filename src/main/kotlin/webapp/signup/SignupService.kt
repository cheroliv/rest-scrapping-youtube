package webapp.signup

import jakarta.validation.Valid
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import webapp.Constants.DEFAULT_LANGUAGE
import webapp.Constants.ROLE_USER
import webapp.Constants.SYSTEM_USER
import webapp.Logging.i
import webapp.mail.MailService
import webapp.models.AccountCredentials
import webapp.models.AccountUtils.generateActivationKey
import webapp.models.exceptions.EmailAlreadyUsedException
import webapp.models.exceptions.UsernameAlreadyUsedException
import webapp.repository.AccountRepository
import java.time.Instant.now

@Service
@Transactional
class SignupService(
    private val accountRepository: AccountRepository,
    private val mailService: MailService,
    private val passwordEncoder: PasswordEncoder,
) {

    @Throws(
        UsernameAlreadyUsedException::class,
        EmailAlreadyUsedException::class
    )
    suspend fun signup(@Valid account: AccountCredentials) {
        i("on entre dans le service")
        isLoginAvailable(account)
        isEmailAvailable(account)
        val createdDate = now()
        account.copy(
            password = passwordEncoder.encode(account.password),
            activationKey = generateActivationKey,
            authorities = setOf(ROLE_USER),
            langKey = if (account.langKey.isNullOrBlank()) DEFAULT_LANGUAGE
            else account.langKey,
            createdBy = SYSTEM_USER,
            createdDate = createdDate,
            lastModifiedBy = SYSTEM_USER,
            lastModifiedDate = createdDate,
            activated = false
        ).run {
            accountRepository.signup(this)
            mailService.sendActivationEmail(this)
        }
    }

    @Throws(UsernameAlreadyUsedException::class)
    private suspend fun isLoginAvailable(model: AccountCredentials) {
        accountRepository.findOne(model.login!!).run {
            if (this != null) if (!activated) accountRepository.delete(this.toAccount())
            else throw UsernameAlreadyUsedException()
        }
    }

    @Throws(EmailAlreadyUsedException::class)
    private suspend fun isEmailAvailable(model: AccountCredentials) {
        accountRepository.findOne(model.email!!).run {
            if (this != null) if (!activated) accountRepository.delete(toAccount())
            else throw EmailAlreadyUsedException()
        }
    }

    suspend fun activate(key: String): Boolean {
        with(accountRepository.findOneByActivationKey(key)) {
            return if (this == null) false.apply { i("no activation for key: $key") }
            else accountRepository.save(copy(activated = true, activationKey = null)).run {
                return if (this != null) true.apply { i("activation: $login") } else false
            }
        }
    }
}