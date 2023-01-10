@file:Suppress("unused")

package webapp


import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.Jwts.parserBuilder
import io.jsonwebtoken.SignatureAlgorithm.HS512
import io.jsonwebtoken.io.Decoders.BASE64
import io.jsonwebtoken.jackson.io.JacksonSerializer
import io.jsonwebtoken.lang.Strings.hasLength
import io.jsonwebtoken.security.Keys.hmacShaKeyFor
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod.*
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder.*
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.*
import org.springframework.stereotype.Component
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import webapp.Application.SpaWebFilter
import webapp.Constants.AUTHORITIES_KEY
import webapp.Constants.FEATURE_POLICY
import webapp.Constants.INVALID_TOKEN
import webapp.Constants.ROLE_ADMIN
import webapp.Constants.VALID_TOKEN
import webapp.Logging.d
import webapp.Logging.i
import webapp.Logging.t
import webapp.Logging.w
import java.security.Key
import java.util.*
import kotlin.text.Charsets.UTF_8
import org.springframework.security.core.userdetails.User as UserSecurity


/*=================================================================================*/

@Component
class Security(
    private val properties: Properties,
    private var key: Key? = null,
    private var tokenValidityInMilliseconds: Long = 0,
    private var tokenValidityInMillisecondsForRememberMe: Long = 0
) : InitializingBean {
    @Configuration
    @EnableWebFluxSecurity
    @EnableReactiveMethodSecurity
    class SecurityConfiguration(
        private val properties: Properties,
        private val security: Security,
        private val userDetailsService: ReactiveUserDetailsService,
    ) {
        @Bean
        fun springSecurityFilterChain(
            http: ServerHttpSecurity
        ): SecurityWebFilterChain =
            http.securityMatcher(
                NegatedServerWebExchangeMatcher(
                    OrServerWebExchangeMatcher(
                        pathMatchers(
                            "/app/**",
                            "/i18n/**",
                            "/content/**",
                            "/swagger-ui/**",
                            "/test/**",
                            "/webjars/**"
                        ), pathMatchers(OPTIONS, "/**")
                    )
                )
            ).csrf()
                .disable()
                .addFilterAt(SpaWebFilter(), AUTHENTICATION)
                .addFilterAt(JwtFilter(security), HTTP_BASIC)
                .authenticationManager(reactiveAuthenticationManager())
                .exceptionHandling()
//        .accessDeniedHandler(problemSupport)
//        .authenticationEntryPoint(problemSupport)
                .and()
                .headers()
                .contentSecurityPolicy(Constants.CONTENT_SECURITY_POLICY)
                .and()
                .referrerPolicy(STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                .and()
//                .permissionsPolicy(FEATURE_POLICY)
                .featurePolicy(FEATURE_POLICY)
                .and()
                .frameOptions().disable()
                .and()
                .authorizeExchange()
                .pathMatchers("/").permitAll()
                .pathMatchers("/**").permitAll()
                .pathMatchers("/*.*").permitAll()
                .pathMatchers("/api/account/signup").permitAll()
                .pathMatchers("/api/activate").permitAll()
                .pathMatchers("/api/authenticate").permitAll()
                .pathMatchers("/api/account/reset-password/init").permitAll()
                .pathMatchers("/api/account/reset-password/finish").permitAll()
                .pathMatchers("/api/auth-info").permitAll()
                .pathMatchers("/api/user/**").permitAll()
                .pathMatchers("/management/health").permitAll()
                .pathMatchers("/management/health/**").permitAll()
                .pathMatchers("/management/info").permitAll()
                .pathMatchers("/management/prometheus").permitAll()
                .pathMatchers("/api/**").permitAll()
                .pathMatchers("/services/**").authenticated()
                .pathMatchers("/swagger-resources/**").authenticated()
                .pathMatchers("/v2/api-docs").authenticated()
                .pathMatchers("/management/**").hasAuthority(ROLE_ADMIN)
                .pathMatchers("/api/admin/**").hasAuthority(ROLE_ADMIN)
                .and()
                .build()

        @Bean
        fun corsFilter(): CorsWebFilter = CorsWebFilter(UrlBasedCorsConfigurationSource().apply source@{
            properties.cors.apply config@{
                if (allowedOrigins != null && allowedOrigins!!.isNotEmpty()) {
                    d("Registering CORS filter").run {
                        this@source.apply {
                            registerCorsConfiguration("/api/**", this@config)
                            registerCorsConfiguration("/management/**", this@config)
                            registerCorsConfiguration("/v2/api-docs", this@config)
                        }
                    }
                }
            }
        })


        @Bean("passwordEncoder")
        fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

        @Bean
        fun reactiveAuthenticationManager(): ReactiveAuthenticationManager =
            UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService).apply {
                setPasswordEncoder(passwordEncoder())
            }


        @Component("jwtFilter")
        class JwtFilter(private val security: Security) : WebFilter {

            override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
                resolveToken(exchange.request).apply token@{
                    chain.apply {
                        return if (!isNullOrBlank() &&
                            security.validateToken(this@token)
                        ) filter(exchange)
                            .contextWrite(
                                ReactiveSecurityContextHolder.withAuthentication(
                                    security.getAuthentication(this@token)
                                )
                            )
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
    }



    @Throws(Exception::class)
    override fun afterPropertiesSet() {
        properties
            .security
            .authentication
            .jwt
            .secret
            .run {
                key = hmacShaKeyFor(
                    when {
                        !hasLength(this) -> w(
                            "Warning: the Jwt key used is not Base64-encoded. " +
                                    "We recommend using the `webapp.security.authentication.jwt.base64-secret`" +
                                    " key for optimum security."
                        ).run { toByteArray(UTF_8) }

                        else -> d("Using a Base64-encoded Jwt secret key").run {
                            BASE64.decode(
                                properties
                                    .security
                                    .authentication
                                    .jwt
                                    .base64Secret
                            )
                        }
                    }
                )
            }
        tokenValidityInMilliseconds = properties
            .security
            .authentication
            .jwt
            .tokenValidityInSeconds * 1000
        tokenValidityInMillisecondsForRememberMe = properties
            .security
            .authentication
            .jwt
            .tokenValidityInSecondsForRememberMe * 1000
    }

    suspend fun createToken(
        authentication: Authentication,
        rememberMe: Boolean
    ): String {
        Date().time.apply {
            return@createToken Jwts.builder()
                .setSubject(authentication.name)
                .claim(
                    AUTHORITIES_KEY,
                    authentication.authorities
                        .asSequence()
                        .map { it.authority }
                        .joinToString(separator = ","))
                .signWith(key, HS512)
                .setExpiration(
                    if (rememberMe) Date(this + tokenValidityInMillisecondsForRememberMe)
                    else Date(this + tokenValidityInMilliseconds)
                )
                .serializeToJsonWith(JacksonSerializer())
                .compact()
        }
    }

    fun getAuthentication(token: String): Authentication {
        parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
            .apply {
                this[AUTHORITIES_KEY]
                    .toString()
                    .splitToSequence(",")
                    .mapTo(mutableListOf()) { SimpleGrantedAuthority(it) }
                    .apply authorities@{
                        return@getAuthentication UsernamePasswordAuthenticationToken(
                            UserSecurity(subject, "", this@authorities),
                            token,
                            this@authorities
                        )
                    }
            }
    }

    fun validateToken(token: String): Boolean {
        try {
            parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
            return VALID_TOKEN
        } catch (e: JwtException) {
            i("Invalid Jwt token.")
            t("Invalid Jwt token trace. $e")
        } catch (e: IllegalArgumentException) {
            i("Invalid Jwt token.")
            t("Invalid Jwt token trace. $e")
        }
        return INVALID_TOKEN
    }
}
/*=================================================================================*/