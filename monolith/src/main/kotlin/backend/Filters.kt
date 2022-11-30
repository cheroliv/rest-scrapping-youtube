package backend

import backend.Constants.AUTHORIZATION_HEADER
import backend.Constants.BEARER_START_WITH
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.security.core.context.ReactiveSecurityContextHolder.withAuthentication
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/*=================================================================================*/

@Component("jwtFilter")
class JwtFilter(private val tokenProvider: TokenProvider) : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        resolveToken(exchange.request).apply token@{
            chain.apply {
                return if (!isNullOrBlank() &&
                    tokenProvider.validateToken(token = this@token)
                ) filter(exchange)
                    .contextWrite(
                        withAuthentication(
                            tokenProvider.getAuthentication(token = this@token)
                        )
                    )
                else filter(exchange)
            }
        }
    }

    private fun resolveToken(request: ServerHttpRequest): String? = request
        .headers
        .getFirst(AUTHORIZATION_HEADER)
        .apply {
            return if (
                !isNullOrBlank() &&
                startsWith(BEARER_START_WITH)
            ) substring(startIndex = 7)
            else null
        }

}

/*=================================================================================*/



@Component
class SpaWebFilter : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        exchange.request.uri.path.apply {
            return if (
                !this.startsWith("/api") &&
                !this.startsWith("/management") &&
                !this.startsWith("/services") &&
                !this.startsWith("/swagger") &&
                !this.startsWith("/v2/api-docs") &&
                this.matches(Regex("[^\\\\.]*"))
            ) chain.filter(
                exchange.mutate().request(
                    exchange.request
                        .mutate()
                        .path("/index.html")
                        .build()
                ).build()
            ) else chain.filter(exchange)
        }
    }
}