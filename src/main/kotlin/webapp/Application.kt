@file:Suppress("unused")

package webapp

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import jakarta.validation.MessageInterpolator
import jakarta.validation.Validation.byDefaultProvider
import jakarta.validation.ValidatorFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
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
import reactor.core.publisher.Hooks.onOperatorDebug
import reactor.core.publisher.Mono
import java.util.*
import java.util.regex.Pattern.compile


@EnableWebFlux
@SpringBootApplication
@EnableConfigurationProperties(Properties::class)
class Application(
    private val properties: Properties,
    private val context: ApplicationContext
) : WebFluxConfigurer {

    @Component
    class SpaWebFilter : WebFilter {
        override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
            exchange.request.uri.path.apply {
                return if (
                    !startsWith("/api") &&
                    !startsWith("/management") &&
                    !startsWith("/services") &&
                    !startsWith("/swagger") &&
                    !startsWith("/v2/api-docs") &&
                    matches(Regex("[^\\\\.]*"))
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

    class CustomInterpolator(private val interpolator: MessageInterpolator) : MessageInterpolator {
        companion object {
            private val parametersPattern = compile("\\[(.+)\\]$")
        }

        override fun interpolate(
            messageTemplate: String,
            context: MessageInterpolator.Context
        ) = replaceParameters(
            interpolator.interpolate(messageTemplate, context)
        )

        override fun interpolate(
            messageTemplate: String,
            context: MessageInterpolator.Context,
            locale: Locale
        ): String = replaceParameters(
            interpolator.interpolate(messageTemplate, context)
        )

        private fun replaceParameters(message: String): String {
            var message = message
            val matcher = parametersPattern.matcher(message)
            var values = arrayOf<String?>()
            if (matcher.find()) {
                values = matcher
                    .group(1)
                    .split("\\s*,\\s*".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                message = message.substring(0, matcher.start())
                for (i in values.indices) {
                    message = message.replace("{$i}", values[i]!!)
                }
            }
            return message
        }
    }

    @Bean
    fun validation(): ValidatorFactory = byDefaultProvider().configure().apply {
        messageInterpolator(CustomInterpolator(defaultMessageInterpolator))
    }.buildValidatorFactory()


    @Bean
    fun validator(): Validator = LocalValidatorFactoryBean()

    @Bean
    fun javaTimeModule(): JavaTimeModule = JavaTimeModule()

    @Bean
    fun jdk8TimeModule(): Jdk8Module = Jdk8Module()

    @Profile("!${Constants.PRODUCTION}")
    fun reactorConfiguration() = onOperatorDebug()

    // TODO: remove when this is supported in spring-data / spring-boot
    @Bean
    fun reactivePageableHandlerMethodArgumentResolver() = ReactivePageableHandlerMethodArgumentResolver()

    // TODO: remove when this is supported in spring-boot
    @Bean
    fun reactiveSortHandlerMethodArgumentResolver() = ReactiveSortHandlerMethodArgumentResolver()
    /*
    <bean class="org.springframework.validation.beanvalidation.MethodValidationPostProcessor">
        <property name="validatedAnnotationType" value="javax.validation.Valid" />
        <property name="validator" ref="refToYOurLocalValidatorFactoryBean" />
    </bean>
     */
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


}

