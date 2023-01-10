package webapp.security

import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.userdetails.UserDetails
import webapp.Constants

object SecurityUtils {

    suspend fun getCurrentUserLogin(): String =
        extractPrincipal(
            ReactiveSecurityContextHolder.getContext()
                .awaitSingle()
                .authentication
        )

    private fun extractPrincipal(authentication: Authentication?): String =
        if (authentication == null) ""
        else when (val principal = authentication.principal) {
            is UserDetails -> principal.username
            is String -> principal
            else -> ""
        }

    suspend fun getCurrentUserJwt(): String =
        ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .filter { it.credentials is String }
            .map { it.credentials as String }
            .awaitSingle()

    suspend fun isAuthenticated(): Boolean = ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .map(Authentication::getAuthorities)
        .map { roles: Collection<GrantedAuthority> ->
            roles.map(GrantedAuthority::getAuthority)
                .none { it == Constants.ROLE_ANONYMOUS }
        }.awaitSingle()


    suspend fun isCurrentUserInRole(authority: String): Boolean = ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .map(Authentication::getAuthorities)
        .map { roles: Collection<GrantedAuthority> ->
            roles.map(GrantedAuthority::getAuthority)
                .any { it == authority }
        }.awaitSingle()
}