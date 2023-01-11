package webapp


import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.context.MessageSource
import webapp.Constants.CLOUD
import webapp.Constants.DEVELOPMENT
import webapp.Constants.DEV_HOST
import webapp.Constants.EMPTY_CONTEXT_PATH
import webapp.Constants.EMPTY_STRING
import webapp.Constants.HTTP
import webapp.Constants.HTTPS
import webapp.Constants.JUMPLINE
import webapp.Constants.PRODUCTION
import webapp.Constants.SERVER_PORT
import webapp.Constants.SERVER_SERVLET_CONTEXT_PATH
import webapp.Constants.SERVER_SSL_KEY_STORE
import webapp.Constants.SPRING_APPLICATION_NAME
import webapp.Constants.STARTUP_HOST_WARN_LOG_MSG
import webapp.Constants.STARTUP_LOG_MSG_KEY
import java.net.InetAddress.getLocalHost
import java.net.UnknownHostException
import java.util.*
import java.util.Locale.getDefault

object Logging {
    @JvmStatic
    private val log: Logger by lazy { getLogger(Application::class.java) }

    @JvmStatic
    fun i(message: String) = log.info(message)

    @JvmStatic
    fun d(message: String) = log.debug(message)

    @JvmStatic
    fun w(message: String) = log.warn(message)

    @JvmStatic
    fun t(message: String) = log.trace(message)

    @JvmStatic
    fun e(message: String) = log.error(message)

    @JvmStatic
    fun e(message: String, defaultMessage: String?) = log.error(message, defaultMessage)

    @JvmStatic
    fun e(message: String, e: Exception?) = log.error(message, e)

    @JvmStatic
    fun w(message: String, e: Exception?) = log.warn(message, e)


    /*=================================================================================*/
    @Suppress("UnusedReceiverParameter")
    fun ApplicationContext.`continue`() = Unit

    /*=================================================================================*/

    private fun startupLogMessage(
        appName: String?,
        goVisitMessage: String,
        protocol: String,
        serverPort: String?,
        contextPath: String,
        hostAddress: String,
        profiles: String,
        activeProfiles: String
    ): String = """$JUMPLINE$JUMPLINE$JUMPLINE
----------------------------------------------------------
go visit $goVisitMessage    
----------------------------------------------------------
Application '$appName' is running!
Access URLs
    Local:      $protocol://localhost:$serverPort$contextPath
    External:   $protocol://$hostAddress:$serverPort$contextPath${
        if (profiles.isNotBlank()) JUMPLINE + buildString {
            append("Profile(s): ")
            append(profiles)
        } else EMPTY_STRING
    }${
        if (activeProfiles.isNotBlank()) JUMPLINE + buildString {
            append("Active(s) profile(s): ")
            append(activeProfiles)
        } else EMPTY_STRING
    }
----------------------------------------------------------
$JUMPLINE$JUMPLINE""".trimIndent()


    /*=================================================================================*/

    internal fun ApplicationContext.checkProfileLog(): ApplicationContext = apply {
        environment.activeProfiles.run {
            if (contains(DEVELOPMENT) && contains(PRODUCTION))
                log.error(
                    getBean<MessageSource>().getMessage(
                        STARTUP_LOG_MSG_KEY,
                        arrayOf(DEVELOPMENT, PRODUCTION),
                        getDefault()
                    )
                )
            if (contains(DEVELOPMENT) && contains(CLOUD))
                log.error(
                    getBean<MessageSource>().getMessage(
                        STARTUP_LOG_MSG_KEY,
                        arrayOf(DEVELOPMENT, CLOUD),
                        getDefault()
                    )
                )
        }
    }

    /*=================================================================================*/

    internal fun ApplicationContext.bootstrapLog(): ApplicationContext = apply {
        startupLogMessage(
            appName = environment.getProperty(SPRING_APPLICATION_NAME),
            goVisitMessage = getBean<Properties>().goVisitMessage,
            protocol = if (environment.getProperty(SERVER_SSL_KEY_STORE) != null) HTTPS
            else HTTP,
            serverPort = environment.getProperty(SERVER_PORT),
            contextPath = environment.getProperty(SERVER_SERVLET_CONTEXT_PATH)
                ?: EMPTY_CONTEXT_PATH,
            hostAddress = try {
                getLocalHost().hostAddress
            } catch (e: UnknownHostException) {
                w(STARTUP_HOST_WARN_LOG_MSG)
                DEV_HOST
            },
            profiles = if (environment.defaultProfiles.isNotEmpty())
                environment
                    .defaultProfiles
                    .reduce { accumulator, profile -> "$accumulator, $profile" }
            else EMPTY_STRING,
            activeProfiles = if (environment.activeProfiles.isNotEmpty())
                environment
                    .activeProfiles
                    .reduce { accumulator, profile -> "$accumulator, $profile" }
            else EMPTY_STRING,
        ).run { i(this) }
    }
}
/*=================================================================================*/
