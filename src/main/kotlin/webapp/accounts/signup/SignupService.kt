package webapp.accounts.signup

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import webapp.*
import webapp.Constants.DEFAULT_LANGUAGE
import webapp.Constants.ROLE_USER
import webapp.Constants.SYSTEM_USER
import webapp.Logging.i
import webapp.accounts.*
import webapp.accounts.mail.MailService
import webapp.accounts.models.AccountCredentials
import webapp.accounts.models.AccountUtils.generateActivationKey
import webapp.accounts.models.exceptions.EmailAlreadyUsedException
import webapp.accounts.models.exceptions.InvalidPasswordException
import webapp.accounts.models.exceptions.UsernameAlreadyUsedException
import webapp.accounts.repository.AccountRepository
import java.time.Instant.now


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
        InvalidPasswordException().run { if (isPasswordLengthInvalid(account.password)) throw this }
        loginValidation(account)
        emailValidation(account)
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