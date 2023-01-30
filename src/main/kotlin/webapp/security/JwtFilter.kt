package webapp.security

import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.security.core.context.ReactiveSecurityContextHolder.withAuthentication
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import webapp.Constants

@Component("jwtFilter")
class JwtFilter(private val security: Security) : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        resolveToken(exchange.request).apply token@{
            chain.apply {
                return if (!isNullOrBlank() &&
                    security.validateToken(this@token)
                ) filter(exchange)
                    .contextWrite(withAuthentication(security.getAuthentication(this@token)))
                else filter(exchange)
            }
        }
    }

    private fun resolveToken(request: ServerHttpRequest): String? = request
        .headers
        .getFirst(Constants.AUTHORIZATION_HEADER)
        .apply {
            return if (
                !isNullOrBlank() &&
                startsWith(Constants.BEARER_START_WITH)
            ) substring(startIndex = 7)
            else null
        }
}