@file:Suppress("unused")

package webapp

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.*
import org.springframework.data.web.ReactivePageableHandlerMethodArgumentResolver
import org.springframework.data.web.ReactiveSortHandlerMethodArgumentResolver
import org.springframework.format.FormatterRegistry
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar
import org.springframework.http.HttpMethod.OPTIONS
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder.AUTHENTICATION
import org.springframework.security.config.web.server.SecurityWebFiltersOrder.HTTP_BASIC
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers
import org.springframework.validation.Validator
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.WebFluxConfigurer
import reactor.core.publisher.Hooks
import webapp.Constants.FEATURE_POLICY
import webapp.Constants.GMAIL
import webapp.Constants.MAILSLURP
import webapp.Constants.MAIL_DEBUG
import webapp.Constants.MAIL_SMTP_AUTH
import webapp.Constants.MAIL_TRANSPORT_PROTOCOL
import webapp.Constants.MAIL_TRANSPORT_STARTTLS_ENABLE
import webapp.Logging.d


/*=================================================================================*/
@EnableWebFlux
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@EnableConfigurationProperties(Properties::class)
@SpringBootApplication
class Application(
    private val properties: Properties,
    private val userDetailsService: ReactiveUserDetailsService,
    private val tokenProvider: TokenProvider,
) : WebFluxConfigurer {

    override fun addFormatters(registry: FormatterRegistry) {
        DateTimeFormatterRegistrar().apply {
            setUseIsoFormat(true)
            registerFormatters(registry)
        }
    }

    @Bean
    fun validator(): Validator = LocalValidatorFactoryBean()

    @Bean
    fun javaTimeModule(): JavaTimeModule = JavaTimeModule()

    @Bean
    fun jdk8TimeModule(): Jdk8Module = Jdk8Module()

    @Bean
    @Profile("!$MAILSLURP & !$GMAIL")
    fun javaMailSender(): JavaMailSender = JavaMailSenderImpl().apply {
        host = properties.mail.host
        port = properties.mail.port
        username = properties.mail.from
        password = properties.mail.password
        mapOf(
            MAIL_TRANSPORT_PROTOCOL to properties.mail.property.transport.protocol,
            MAIL_SMTP_AUTH to properties.mail.property.smtp.auth,
            MAIL_TRANSPORT_STARTTLS_ENABLE to properties.mail.property.smtp.starttls.enable,
            MAIL_DEBUG to properties.mail.property.debug,
            "spring.mail.test-connection" to true,
            "mail.smtp.ssl.trust" to true,
            "mail.connect_timeout" to 60000,
            "mail.auth_api_key" to "",
        ).forEach { javaMailProperties[it.key] = it.value }
    }


//    /**
//     * The handler must have precedence over
//     * WebFluxResponseStatusExceptionHandler
//     * and Spring Boot's ErrorWebExceptionHandler
//     */
//    @Bean
//    @Order(-2)
//    fun problemHandler(
//        mapper: ObjectMapper, problemHandling: ProblemHandling
//    ): WebExceptionHandler = ProblemExceptionHandler(mapper, problemHandling)
//
//    @Bean
//    fun problemModule(): ProblemModule = ProblemModule()
//
//    @Bean
//    fun constraintViolationProblemModule() = ConstraintViolationProblemModule()

    @Profile("!${Constants.PRODUCTION}")
    fun reactorConfiguration() = Hooks.onOperatorDebug()

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

    // TODO: remove when this is supported in spring-data / spring-boot
    @Bean
    fun reactivePageableHandlerMethodArgumentResolver() = ReactivePageableHandlerMethodArgumentResolver()

    // TODO: remove when this is supported in spring-boot
    @Bean
    fun reactiveSortHandlerMethodArgumentResolver() = ReactiveSortHandlerMethodArgumentResolver()

    /*
        @Bean
        fun registrationCustomizer(): ResourceHandlerRegistrationCustomizer {
            // Disable built-in cache control to use our custom filter instead
            return registration -> registration.setCacheControl(null);
        }

        @Bean
        @Profile(Constants.SPRING_PROFILE_PRODUCTION)
        fun cachingHttpHeadersFilter(): CachingHttpHeadersFilter {
            // Use a cache filter that only match selected paths
            return CachingHttpHeadersFilter(
                TimeUnit.DAYS.toMillis(
                    Properties.getHttp().getCache().getTimeToLiveInDays()
                )
            )
        }
    */
    @Bean("passwordEncoder")
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun reactiveAuthenticationManager(): ReactiveAuthenticationManager =
        UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService).apply {
            setPasswordEncoder(passwordEncoder())
        }

    @Bean
    fun springSecurityFilterChain(
        http: ServerHttpSecurity
    ): SecurityWebFilterChain = @Suppress("DEPRECATION") http.securityMatcher(
        NegatedServerWebExchangeMatcher(
            OrServerWebExchangeMatcher(
                pathMatchers(
                    "/app/**", "/i18n/**", "/content/**", "/swagger-ui/**", "/test/**", "/webjars/**"
                ), pathMatchers(OPTIONS, "/**")
            )
        )
    ).csrf().disable().addFilterAt(SpaWebFilter(), AUTHENTICATION).addFilterAt(JwtFilter(tokenProvider), HTTP_BASIC)
        .authenticationManager(reactiveAuthenticationManager())
        .exceptionHandling()
//        .accessDeniedHandler(problemSupport)
//        .authenticationEntryPoint(problemSupport)
        .and().headers()
        .contentSecurityPolicy(Constants.CONTENT_SECURITY_POLICY).and().referrerPolicy(STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
        .and().featurePolicy(FEATURE_POLICY).and().frameOptions().disable().and().authorizeExchange().pathMatchers("/")
        .permitAll().pathMatchers("/**").permitAll().pathMatchers("/*.*").permitAll()
        .pathMatchers("/api/account/signup").permitAll().pathMatchers("/api/activate").permitAll()
        .pathMatchers("/api/authenticate").permitAll().pathMatchers("/api/account/reset-password/init").permitAll()
        .pathMatchers("/api/account/reset-password/finish").permitAll().pathMatchers("/api/auth-info").permitAll()
        .pathMatchers("/api/user/**").permitAll().pathMatchers("/management/health").permitAll()
        .pathMatchers("/management/health/**").permitAll().pathMatchers("/management/info").permitAll()
        .pathMatchers("/management/prometheus").permitAll().pathMatchers("/api/**").permitAll()
        .pathMatchers("/services/**").authenticated().pathMatchers("/swagger-resources/**").authenticated()
        .pathMatchers("/v2/api-docs").authenticated().pathMatchers("/management/**").hasAuthority(Constants.ROLE_ADMIN)
        .pathMatchers("/api/admin/**").hasAuthority(Constants.ROLE_ADMIN).and().build()
}

/*=================================================================================*/


