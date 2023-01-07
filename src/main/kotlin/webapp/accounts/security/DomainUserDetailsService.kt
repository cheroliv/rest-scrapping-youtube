package webapp.accounts.security

import kotlinx.coroutines.reactor.mono
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import webapp.Bootstrap.log
import webapp.accounts.models.AccountCredentials
import webapp.accounts.models.AccountCredentials.Companion.isValidEmail
import webapp.accounts.models.exceptions.UserNotActivatedException
import webapp.accounts.repository.AccountRepository

@Suppress("unused")
@Component("userDetailsService")
class DomainUserDetailsService(
    private val accountRepository: AccountRepository
) : ReactiveUserDetailsService {

    @Transactional
    override fun findByUsername(emailOrLogin: String): Mono<UserDetails> = log
        .debug("Authenticating $emailOrLogin").run {
            return if (isValidEmail(emailOrLogin)) mono {
                accountRepository.findOneWithAuthorities(emailOrLogin).apply {
                    if (this == null) throw UsernameNotFoundException("User with email $emailOrLogin was not found in the database")
                }
            }.map { createSpringSecurityUser(emailOrLogin, it) }
            else mono {
                accountRepository.findOneWithAuthorities(emailOrLogin).apply {
                    if (this == null) throw UsernameNotFoundException("User $emailOrLogin was not found in the database")
                }
            }.map { createSpringSecurityUser(emailOrLogin, it) }
        }


    private fun createSpringSecurityUser(
        lowercaseLogin: String,
        account: AccountCredentials
    ): User = if (!account.activated)
        throw UserNotActivatedException("User $lowercaseLogin was not activated")
    else User(
        account.login!!,
        account.password!!,
        account.authorities!!.map {
            SimpleGrantedAuthority(it)
        }
    )
}