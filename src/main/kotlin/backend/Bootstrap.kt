package backend

import backend.Constants.CLOUD
import backend.Constants.DEVELOPMENT
import backend.Constants.DEV_HOST
import backend.Constants.EMPTY_CONTEXT_PATH
import backend.Constants.EMPTY_STRING
import backend.Constants.HTTP
import backend.Constants.HTTPS
import backend.Constants.JUMPLINE
import backend.Constants.PRODUCTION
import backend.Constants.SERVER_PORT
import backend.Constants.SERVER_SERVLET_CONTEXT_PATH
import backend.Constants.SERVER_SSL_KEY_STORE
import backend.Constants.SPRING_APPLICATION_NAME
import backend.Constants.STARTUP_HOST_WARN_LOG_MSG
import backend.Constants.STARTUP_LOG_MSG_KEY
import backend.Log.log
import jakarta.annotation.PostConstruct
import org.apache.logging.log4j.LogManager.getLogger
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import java.net.InetAddress.getLocalHost
import java.net.UnknownHostException
import java.util.*
import java.util.Locale.getDefault

/*=================================================================================*/
object Log {
    @JvmStatic
    val log: Logger by lazy { getLogger(Log.javaClass) }
}

/*=================================================================================*/

@Suppress("unused")
@Component
class BackendComponent(private val context: ApplicationContext) {
    @PostConstruct
    private fun init() = context.checkProfileLog()
}
/*=================================================================================*/

internal fun ApplicationContext.checkProfileLog(): ApplicationContext = apply {
    environment.activeProfiles.run {
        if (contains(DEVELOPMENT) && contains(PRODUCTION)) log.error(
            getBean<MessageSource>().getMessage(
                STARTUP_LOG_MSG_KEY,
                arrayOf(DEVELOPMENT, PRODUCTION),
                getDefault()
            )
        )
        if (contains(DEVELOPMENT) && contains(CLOUD)) log.error(
            getBean<MessageSource>().getMessage(
                STARTUP_LOG_MSG_KEY,
                arrayOf(DEVELOPMENT, CLOUD),
                getDefault()
            )
        )
    }
}

/*=================================================================================*/

internal fun ApplicationContext.bootstrapLog() = startupLogMessage(
    appName = environment.getProperty(SPRING_APPLICATION_NAME),
    goVisitMessage = getBean<ApplicationProperties>().goVisitMessage,
    protocol = if (environment.getProperty(SERVER_SSL_KEY_STORE) != null) HTTPS
    else HTTP,
    serverPort = environment.getProperty(SERVER_PORT),
    contextPath = environment.getProperty(SERVER_SERVLET_CONTEXT_PATH) ?: EMPTY_CONTEXT_PATH,
    hostAddress = try {
        getLocalHost().hostAddress
    } catch (e: UnknownHostException) {
        log.warn(STARTUP_HOST_WARN_LOG_MSG)
        DEV_HOST
    },
    profiles = if (environment.defaultProfiles.isNotEmpty())
        environment.defaultProfiles
            .reduce { accumulator, profile -> "$accumulator, $profile" }
    else EMPTY_STRING,
    activeProfiles = if (environment.activeProfiles.isNotEmpty())
        environment.activeProfiles
            .reduce { accumulator, profile -> "$accumulator, $profile" }
    else EMPTY_STRING,
).run { log.info(this) }

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
Application '$appName' is running! Access URLs:
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

