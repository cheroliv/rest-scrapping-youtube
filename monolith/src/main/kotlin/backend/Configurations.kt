@file:Suppress("unused")

package backend

//import reactor.core.publisher.Hooks.onOperatorDebug

import backend.Constants.FEATURE_POLICY
import backend.Constants.REQUEST_PARAM_LANG
import backend.Log.log
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.apache.commons.mail.EmailConstants.*
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties
import org.springframework.boot.autoconfigure.web.reactive.ResourceHandlerRegistrationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.context.i18n.LocaleContext
import org.springframework.context.i18n.SimpleLocaleContext
import org.springframework.core.annotation.Order
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.data.web.ReactivePageableHandlerMethodArgumentResolver
import org.springframework.data.web.ReactiveSortHandlerMethodArgumentResolver
import org.springframework.format.FormatterRegistry
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar
import org.springframework.http.HttpMethod.OPTIONS
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
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
import org.springframework.web.reactive.config.DelegatingWebFluxConfiguration
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebExceptionHandler
import org.springframework.web.server.i18n.LocaleContextResolver
import org.zalando.problem.jackson.ProblemModule
import org.zalando.problem.spring.webflux.advice.ProblemExceptionHandler
import org.zalando.problem.spring.webflux.advice.ProblemHandling
import org.zalando.problem.spring.webflux.advice.security.SecurityProblemSupport
import org.zalando.problem.violations.ConstraintViolationProblemModule
import reactor.core.publisher.Hooks
import java.util.Locale.forLanguageTag
import java.util.Locale.getDefault
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.Future

//import org.springframework.boot.autoconfigure.web.reactive.ResourceHandlerRegistrationCustomizer
//import org.springframework.context.annotation.Import
//import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
//import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
//import org.springframework.security.core.userdetails.ReactiveUserDetailsService
//import org.zalando.problem.spring.webflux.advice.security.SecurityProblemSupport

/*=================================================================================*/
@Configuration
class LocaleSupportConfiguration : DelegatingWebFluxConfiguration() {

    override fun createLocaleContextResolver(): LocaleContextResolver =
        RequestParamLocaleContextResolver()

    class RequestParamLocaleContextResolver : LocaleContextResolver {
        override fun resolveLocaleContext(exchange: ServerWebExchange): LocaleContext {
            var targetLocale = getDefault()
            val referLang = exchange.request.queryParams[REQUEST_PARAM_LANG]
            if (referLang != null && referLang.isNotEmpty())
                targetLocale = forLanguageTag(referLang[0])
            return SimpleLocaleContext(targetLocale)
        }

        @Throws(UnsupportedOperationException::class)
        override fun setLocaleContext(
            exchange: ServerWebExchange,
            localeContext: LocaleContext?
        ): Unit = throw UnsupportedOperationException("Not Supported")
    }
}


/*=================================================================================*/
@Configuration
@EnableWebFlux
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@Import(SecurityProblemSupport::class)
class WebConfiguration(
    private val properties: ApplicationProperties,
    private val userDetailsService: ReactiveUserDetailsService,
    private val tokenProvider: TokenProvider,
    private val problemSupport: SecurityProblemSupport,
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
    fun javaMailSender(): JavaMailSender = JavaMailSenderImpl()
        .apply {
            host = properties.mail.host
            port = properties.mail.port
            username = properties.mail.from
            password = properties.mail.password
            javaMailProperties.apply {
                this[MAIL_TRANSPORT_PROTOCOL] = properties.mail.property.transport.protocol
                this[MAIL_SMTP_AUTH] = properties.mail.property.smtp.auth
                this[MAIL_TRANSPORT_STARTTLS_ENABLE] = properties.mail.property.smtp.starttls.enable
                this[MAIL_DEBUG] = properties.mail.property.debug
                this["spring.mail.test-connection"] = true
                this["mail.smtp.ssl.trust"] = true
            }
        }


    /**
     * The handler must have precedence over
     * WebFluxResponseStatusExceptionHandler
     * and Spring Boot's ErrorWebExceptionHandler
     */
    @Bean
    @Order(-2)
    fun problemHandler(
        mapper: ObjectMapper,
        problemHandling: ProblemHandling
    ): WebExceptionHandler = ProblemExceptionHandler(mapper, problemHandling)

    @Bean
    fun problemModule(): ProblemModule = ProblemModule()

    @Bean
    fun constraintViolationProblemModule() = ConstraintViolationProblemModule()

    @Profile("!${Constants.SPRING_PROFILE_PRODUCTION}")
    fun reactorConfiguration() = Hooks.onOperatorDebug()

    @Bean
    fun corsFilter(): CorsWebFilter = CorsWebFilter(UrlBasedCorsConfigurationSource().apply source@{
        properties.cors.apply config@{
            if (
                allowedOrigins != null &&
                allowedOrigins!!.isNotEmpty()
            ) {
                log.debug("Registering CORS filter").run {
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
                ApplicationProperties.getHttp().getCache().getTimeToLiveInDays()
            )
        )
    }
*/
    @Bean("passwordEncoder")
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun reactiveAuthenticationManager(): ReactiveAuthenticationManager =
        UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService)
            .apply { setPasswordEncoder(passwordEncoder()) }

    @Bean
    fun springSecurityFilterChain(
        http: ServerHttpSecurity
    ): SecurityWebFilterChain =
        @Suppress("DEPRECATION")
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
                    ),
                    pathMatchers(OPTIONS, "/**")
                )
            )
        ).csrf()
            .disable()
            .addFilterAt(SpaWebFilter(), AUTHENTICATION)
            .addFilterAt(JwtFilter(tokenProvider), HTTP_BASIC)
            .authenticationManager(reactiveAuthenticationManager())
            .exceptionHandling()
            .accessDeniedHandler(problemSupport)
            .authenticationEntryPoint(problemSupport)
            .and()
            .headers().contentSecurityPolicy(Constants.CONTENT_SECURITY_POLICY)
            .and()
            .referrerPolicy(STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
            .and()
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
            .pathMatchers("/management/**").hasAuthority(Constants.ROLE_ADMIN)
            .pathMatchers("/api/admin/**").hasAuthority(Constants.ROLE_ADMIN)
            .and()
            .build()
}

/*=================================================================================*/


@EnableAsync
@Configuration
@EnableScheduling
@Suppress("unused")
class AsyncTasksConfiguration(
    private val taskExecutionProperties: TaskExecutionProperties
) : AsyncConfigurer {


    @Bean(name = ["taskExecutor"])
    override fun getAsyncExecutor()
            : Executor = ExceptionHandlingAsyncTaskExecutor(
        ThreadPoolTaskExecutor().apply {
            queueCapacity = taskExecutionProperties.pool.queueCapacity
            @Suppress("UsePropertyAccessSyntax")
            setThreadNamePrefix(taskExecutionProperties.threadNamePrefix)
            corePoolSize = taskExecutionProperties.pool.coreSize
            maxPoolSize = taskExecutionProperties.pool.maxSize
        }).also { log.debug("Creating Async Task Executor") }

    override fun getAsyncUncaughtExceptionHandler()
            : AsyncUncaughtExceptionHandler = SimpleAsyncUncaughtExceptionHandler()


    class ExceptionHandlingAsyncTaskExecutor(
        private val executor: AsyncTaskExecutor
    ) : AsyncTaskExecutor,
        InitializingBean,
        DisposableBean {
        companion object {
            const val EXCEPTION_MESSAGE = "Caught async exceptions"
        }

        override fun execute(task: Runnable): Unit = executor.execute(createWrappedRunnable(task))

        @Suppress("OVERRIDE_DEPRECATION")
        override fun execute(task: Runnable, startTimeout: Long): Unit =
            executor.execute(createWrappedRunnable(task), startTimeout)

        private fun <T> createCallable(task: Callable<T>): Callable<T> = Callable {
            try {
                return@Callable task.call()
            } catch (e: Exception) {
                handle(e)
                throw e
            }
        }

        private fun createWrappedRunnable(task: Runnable): Runnable =
            Runnable {
                try {
                    task.run()
                } catch (e: Exception) {
                    handle(e)
                }
            }

        private fun handle(e: Exception?): Unit = log.error(EXCEPTION_MESSAGE, e)

        override fun submit(task: Runnable): Future<*> = executor.submit(createWrappedRunnable(task))

        override fun <T> submit(task: Callable<T>): Future<T> = executor.submit(createCallable(task))

        @Throws(Exception::class)
        override fun destroy() {
            if (executor is DisposableBean)
                (executor as DisposableBean)
                    .apply(DisposableBean::destroy)
        }

        @Throws(Exception::class)
        override fun afterPropertiesSet() {
            if (executor is InitializingBean)
                (executor as InitializingBean)
                    .apply { afterPropertiesSet() }
        }
    }
}

/*=================================================================================*/
