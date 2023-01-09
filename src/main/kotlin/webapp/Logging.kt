package webapp


import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.context.MessageSource
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*

object Logging {
    @JvmStatic
    val log: Logger by lazy { getLogger(Application::class.java) }

    /*=================================================================================*/
    @Suppress("UnusedReceiverParameter")
    fun ApplicationContext.`continue`() = Unit

    /*=================================================================================*/

    fun startupLogMessage(
        appName: String?,
        goVisitMessage: String,
        protocol: String,
        serverPort: String?,
        contextPath: String,
        hostAddress: String,
        profiles: String,
        activeProfiles: String
    ): String = """${Constants.JUMPLINE}${Constants.JUMPLINE}${Constants.JUMPLINE}
----------------------------------------------------------
go visit $goVisitMessage    
----------------------------------------------------------
Application '$appName' is running!
Access URLs
    Local:      $protocol://localhost:$serverPort$contextPath
    External:   $protocol://$hostAddress:$serverPort$contextPath${
        if (profiles.isNotBlank()) Constants.JUMPLINE + buildString {
            append("Profile(s): ")
            append(profiles)
        } else Constants.EMPTY_STRING
    }${
        if (activeProfiles.isNotBlank()) Constants.JUMPLINE + buildString {
            append("Active(s) profile(s): ")
            append(activeProfiles)
        } else Constants.EMPTY_STRING
    }
----------------------------------------------------------
${Constants.JUMPLINE}${Constants.JUMPLINE}""".trimIndent()


    /*=================================================================================*/

    internal fun ApplicationContext.checkProfileLog(): ApplicationContext = apply {
        environment.activeProfiles.run {
            if (contains(Constants.DEVELOPMENT) && contains(Constants.PRODUCTION)) log.error(
                getBean<MessageSource>().getMessage(
                    Constants.STARTUP_LOG_MSG_KEY,
                    arrayOf(Constants.DEVELOPMENT, Constants.PRODUCTION),
                    Locale.getDefault()
                )
            )
            if (contains(Constants.DEVELOPMENT) && contains(Constants.CLOUD)) log.error(
                getBean<MessageSource>().getMessage(
                    Constants.STARTUP_LOG_MSG_KEY,
                    arrayOf(Constants.DEVELOPMENT, Constants.CLOUD),
                    Locale.getDefault()
                )
            )
        }
    }

    /*=================================================================================*/

    internal fun ApplicationContext.bootstrapLog(): ApplicationContext = apply {
        startupLogMessage(
            appName = environment.getProperty(Constants.SPRING_APPLICATION_NAME),
            goVisitMessage = getBean<Properties>().goVisitMessage,
            protocol = if (environment.getProperty(Constants.SERVER_SSL_KEY_STORE) != null) Constants.HTTPS
            else Constants.HTTP,
            serverPort = environment.getProperty(Constants.SERVER_PORT),
            contextPath = environment.getProperty(Constants.SERVER_SERVLET_CONTEXT_PATH)
                ?: Constants.EMPTY_CONTEXT_PATH,
            hostAddress = try {
                InetAddress.getLocalHost().hostAddress
            } catch (e: UnknownHostException) {
                log.warn(Constants.STARTUP_HOST_WARN_LOG_MSG)
                Constants.DEV_HOST
            },
            profiles = if (environment.defaultProfiles.isNotEmpty())
                environment.defaultProfiles
                    .reduce { accumulator, profile -> "$accumulator, $profile" }
            else Constants.EMPTY_STRING,
            activeProfiles = if (environment.activeProfiles.isNotEmpty())
                environment.activeProfiles
                    .reduce { accumulator, profile -> "$accumulator, $profile" }
            else Constants.EMPTY_STRING,
        ).run { log.info(this) }
    }
}
/*=================================================================================*/
