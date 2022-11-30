package backend

import backend.Constants.DEV_HOST
import backend.Constants.DOMAIN_URL
import backend.Constants.EMPTY_CONTEXT_PATH
import backend.Constants.HTTP
import backend.Constants.HTTPS
import backend.Constants.PROFILE_SEPARATOR
import backend.Constants.SERVER_PORT
import backend.Constants.SERVER_SERVLET_CONTEXT_PATH
import backend.Constants.SERVER_SSL_KEY_STORE
import backend.Constants.SPRING_APPLICATION_NAME
import backend.Constants.SPRING_PROFILE_CLOUD
import backend.Constants.SPRING_PROFILE_DEVELOPMENT
import backend.Constants.SPRING_PROFILE_PRODUCTION
import backend.Constants.STARTUP_HOST_WARN_LOG_MSG
import backend.Constants.STARTUP_LOG_MSG_KEY
import backend.Log.log
import org.apache.logging.log4j.LogManager.getLogger
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.context.MessageSource
import java.net.InetAddress.getLocalHost
import java.net.UnknownHostException
import java.util.*

/*=================================================================================*/
object Log {
    @JvmStatic
    val log: Logger by lazy { getLogger(Log.javaClass) }
}
/*=================================================================================*/

internal fun checkProfileLog(context: ApplicationContext) =
    context.environment.activeProfiles.run {
        when {
            contains(element = SPRING_PROFILE_DEVELOPMENT) &&
                    contains(element = SPRING_PROFILE_PRODUCTION)
            -> log.error(
                context.getBean<MessageSource>().getMessage(
                    STARTUP_LOG_MSG_KEY,
                    arrayOf(
                        SPRING_PROFILE_DEVELOPMENT,
                        SPRING_PROFILE_PRODUCTION
                    ),
                    Locale.getDefault()
                )
            )
        }
        when {
            contains(SPRING_PROFILE_DEVELOPMENT) &&
                    contains(SPRING_PROFILE_CLOUD)
            -> log.error(
                context.getBean<MessageSource>().getMessage(
                    STARTUP_LOG_MSG_KEY,
                    arrayOf(
                        SPRING_PROFILE_DEVELOPMENT,
                        SPRING_PROFILE_CLOUD
                    ),
                    Locale.getDefault()
                )
            )
        }
    }


/*=================================================================================*/


private fun startupLogMessage(
    appName: String?,
    protocol: String,
    serverPort: String?,
    contextPath: String,
    hostAddress: String,
    profiles: String
): String = """${"\n\n\n"}
----------------------------------------------------------
go visit $DOMAIN_URL    
----------------------------------------------------------
Application '$appName' is running! Access URLs:
Local:      $protocol://localhost:$serverPort$contextPath
External:   $protocol://$hostAddress:$serverPort$contextPath
Profile(s): $profiles
----------------------------------------------------------
${"\n\n\n"}""".trimIndent()


/*=================================================================================*/



internal fun bootstrapLog(context: ApplicationContext): Unit =
    log.info(
        startupLogMessage(
            appName = context.environment.getProperty(SPRING_APPLICATION_NAME),
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
            profiles = context.environment.activeProfiles.joinToString(separator = PROFILE_SEPARATOR)
        )
    )

/*=================================================================================*/

