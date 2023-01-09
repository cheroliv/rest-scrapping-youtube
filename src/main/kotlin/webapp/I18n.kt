package webapp

import org.springframework.context.annotation.Configuration
import org.springframework.context.i18n.LocaleContext
import org.springframework.context.i18n.SimpleLocaleContext
import org.springframework.web.reactive.config.DelegatingWebFluxConfiguration
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.i18n.LocaleContextResolver
import webapp.Constants.REQUEST_PARAM_LANG
import java.util.*
import java.util.Locale.forLanguageTag

//object I18n {
//}

/*=================================================================================*/
@Suppress("unused")
@Configuration
class I18n : DelegatingWebFluxConfiguration() {

    override fun createLocaleContextResolver(): LocaleContextResolver = RequestParamLocaleContextResolver()

    class RequestParamLocaleContextResolver : LocaleContextResolver {
        override fun resolveLocaleContext(exchange: ServerWebExchange): LocaleContext {
            var targetLocale = Locale.getDefault()
            val referLang = exchange.request.queryParams[REQUEST_PARAM_LANG]
            if (!referLang.isNullOrEmpty()) targetLocale = forLanguageTag(referLang[0])
            return SimpleLocaleContext(targetLocale)
        }

        @Throws(UnsupportedOperationException::class)
        override fun setLocaleContext(
            exchange: ServerWebExchange, localeContext: LocaleContext?
        ): Unit = throw UnsupportedOperationException("Not Supported")
    }
}