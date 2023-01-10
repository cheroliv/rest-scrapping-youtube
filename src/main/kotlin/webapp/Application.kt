@file:Suppress("unused")

package webapp

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.*
import org.springframework.data.web.ReactivePageableHandlerMethodArgumentResolver
import org.springframework.data.web.ReactiveSortHandlerMethodArgumentResolver
import org.springframework.stereotype.Component
import org.springframework.validation.Validator
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Hooks
import reactor.core.publisher.Mono


/*=================================================================================*/
@EnableWebFlux
@SpringBootApplication
@EnableConfigurationProperties(Properties::class)
class Application(private val properties: Properties) : WebFluxConfigurer {

    @Bean
    fun validator(): Validator = LocalValidatorFactoryBean()

    @Bean
    fun javaTimeModule(): JavaTimeModule = JavaTimeModule()

    @Bean
    fun jdk8TimeModule(): Jdk8Module = Jdk8Module()




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
}

/*=================================================================================*/


