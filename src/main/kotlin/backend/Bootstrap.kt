package backend

import backend.Constants.JUMPLINE
import backend.Constants.CLOUD
import backend.Constants.DEVELOPMENT
import backend.Constants.DEV_HOST
import backend.Constants.EMPTY_CONTEXT_PATH
import backend.Constants.EMPTY_STRING
import backend.Constants.HTTP
import backend.Constants.HTTPS
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
    private fun init() = checkProfileLog(context)
}
/*=================================================================================*/

private fun checkProfileLog(context: ApplicationContext) =
    context.environment.activeProfiles.run {
        when {
            contains(element = DEVELOPMENT) &&
                    contains(element = PRODUCTION)
            -> log.error(
                context.getBean<MessageSource>().getMessage(
                    STARTUP_LOG_MSG_KEY,
                    arrayOf(
                        DEVELOPMENT,
                        PRODUCTION
                    ),
                    getDefault()
                )
            )
        }
        when {
            contains(DEVELOPMENT) &&
                    contains(CLOUD)
            -> log.error(
                context.getBean<MessageSource>().getMessage(
                    STARTUP_LOG_MSG_KEY,
                    arrayOf(
                        DEVELOPMENT,
                        CLOUD
                    ),
                    getDefault()
                )
            )
        }
    }



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



internal fun bootstrapLog(context: ApplicationContext) =
    log.info(
        startupLogMessage(
            appName = context.environment.getProperty(SPRING_APPLICATION_NAME),
            goVisitMessage = context.getBean<ApplicationProperties>().goVisitMessage,
            protocol = if (context.environment.getProperty(SERVER_SSL_KEY_STORE) != null) HTTPS
            else HTTP,
            serverPort = context.environment.getProperty(SERVER_PORT),
            contextPath = context.environment.getProperty(SERVER_SERVLET_CONTEXT_PATH) ?: EMPTY_CONTEXT_PATH,
            hostAddress = try {
                getLocalHost().hostAddress
            } catch (e: UnknownHostException) {
                log.warn(STARTUP_HOST_WARN_LOG_MSG)
                DEV_HOST
            },
            profiles = when {
                context.environment.defaultProfiles.isNotEmpty() ->
                    context.environment
                        .defaultProfiles
                        .reduce { accumulator, profile -> "$accumulator, $profile" }

                else -> EMPTY_STRING
            },
            activeProfiles = when {
                context.environment.activeProfiles.isNotEmpty() ->
                    context.environment
                        .activeProfiles
                        .reduce { accumulator, profile -> "$accumulator, $profile" }

                else -> EMPTY_STRING
            },
        )
    )


/*=================================================================================*/

