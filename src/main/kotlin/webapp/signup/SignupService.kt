package webapp.signup

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import webapp.Constants
import webapp.Logging.i
import webapp.mail.MailService
import webapp.models.AccountCredentials
import webapp.models.AccountUtils
import webapp.models.exceptions.EmailAlreadyUsedException
import webapp.models.exceptions.InvalidPasswordException
import webapp.models.exceptions.UsernameAlreadyUsedException
import webapp.repository.AccountRepository
import java.time.Instant

@Service
@Transactional
class SignupService(
    private val accountRepository: AccountRepository,
    private val mailService: MailService,
    private val passwordEncoder: PasswordEncoder,
) {

    @Throws(
        InvalidPasswordException::class,
        UsernameAlreadyUsedException::class,
        UsernameAlreadyUsedException::class
    )

    suspend fun signup(account: AccountCredentials) {
        //TODO: account.run
        i("on entre dans le service")

        InvalidPasswordException().run { if (isPasswordLengthInvalid(account.password)) throw this }
        loginValidation(account)
        emailValidation(account)
        val createdDate = Instant.now()
        account.copy(
            password = passwordEncoder.encode(account.password),
            activationKey = AccountUtils.generateActivationKey,
            authorities = setOf(Constants.ROLE_USER),
            langKey = if (account.langKey.isNullOrBlank()) Constants.DEFAULT_LANGUAGE
            else account.langKey,
            createdBy = Constants.SYSTEM_USER,
            createdDate = createdDate,
            lastModifiedBy = Constants.SYSTEM_USER,
            lastModifiedDate = createdDate,
            activated = false
        ).run {
            accountRepository.signup(this)
            mailService.sendActivationEmail(this)
        }
    }

    @Throws(UsernameAlreadyUsedException::class)
    private suspend fun loginValidation(model: AccountCredentials) {
        accountRepository.findOne(model.login!!).run {
            if (this != null) if (!activated) accountRepository.delete(this.toAccount())
            else throw UsernameAlreadyUsedException()
        }
    }

    @Throws(UsernameAlreadyUsedException::class)
    private suspend fun emailValidation(model: AccountCredentials) {
        accountRepository.findOne(model.email!!).run {
            if (this != null) {
                if (!activated) accountRepository.delete(toAccount())
                else throw EmailAlreadyUsedException()
            }
        }
    }

    suspend fun activate(key: String): Boolean {
        with(accountRepository.findOneByActivationKey(key)) {
            return if (this == null) false
            else {
                accountRepository.save(
                    copy(
                        activated = true,
                        activationKey = null
                    )
                ).run { if (id != null) i("activation: $login") }
                true
            }
        }
    }
}