package webapp.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import webapp.Application
import webapp.Constants
import webapp.Logging
import webapp.Properties

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
                    ServerWebExchangeMatchers.pathMatchers(
                        "/app/**",
                        "/i18n/**",
                        "/content/**",
                        "/swagger-ui/**",
                        "/test/**",
                        "/webjars/**"
                    ), ServerWebExchangeMatchers.pathMatchers(HttpMethod.OPTIONS, "/**")
                )
            )
        ).csrf()
            .disable()
            .addFilterAt(Application.SpaWebFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
            .addFilterAt(JwtFilter(security), SecurityWebFiltersOrder.HTTP_BASIC)
            .authenticationManager(reactiveAuthenticationManager())
            .exceptionHandling()
//        .accessDeniedHandler(problemSupport)
//        .authenticationEntryPoint(problemSupport)
            .and()
            .headers()
            .contentSecurityPolicy(Constants.CONTENT_SECURITY_POLICY)
            .and()
            .referrerPolicy(ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
            .and()
//                .permissionsPolicy(FEATURE_POLICY)
            .featurePolicy(Constants.FEATURE_POLICY)
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
            .pathMatchers("/management/**").hasAuthority(Constants.ROLE_ADMIN)
            .pathMatchers("/api/admin/**").hasAuthority(Constants.ROLE_ADMIN)
            .and()
            .build()

    @Bean
    fun corsFilter(): CorsWebFilter = CorsWebFilter(UrlBasedCorsConfigurationSource().apply source@{
        properties.cors.apply config@{
            if (allowedOrigins != null && allowedOrigins!!.isNotEmpty()) {
                Logging.d("Registering CORS filter").run {
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


}