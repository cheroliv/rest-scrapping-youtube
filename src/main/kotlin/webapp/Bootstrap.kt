package webapp

import org.apache.logging.log4j.LogManager.getLogger
import org.apache.logging.log4j.Logger
import org.springframework.beans.factory.getBean
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.MessageSource
import webapp.Bootstrap.log
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
import java.util.Locale.getDefault

/*=================================================================================*/

/*=================================================================================*/

object Bootstrap {
    @JvmStatic
    fun main(args: Array<String>) = runApplication<Application>(*args)
        .checkProfileLog()
        .bootstrapLog()
        .`continue`()

    @JvmStatic
    val log: Logger by lazy { getLogger(Bootstrap.javaClass) }
}

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

internal fun ApplicationContext.bootstrapLog(): ApplicationContext = apply {
    startupLogMessage(
        appName = environment.getProperty(SPRING_APPLICATION_NAME),
        goVisitMessage = getBean<Properties>().goVisitMessage,
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
}
/*=================================================================================*/
/*=================================================================================*/

//object CliBootstrap {
//    @JvmStatic
//    fun main(args: Array<String>) {
//        runApplication<Application>(*args) {
//            setAdditionalProfiles(Constants.PROFILE_CLI)
//            setDefaultProperties(Constants.PROFILE_CLI_PROPS)
//        }
//        kotlin.system.exitProcess(Constants.NORMAL_TERMINATION)
//    }
//}

/*=================================================================================*/
//@org.springframework.stereotype.Component
//@org.springframework.context.annotation.Profile(Constants.PROFILE_CLI)
//class CliRunner : CommandLineRunner {
//    override fun run(vararg args: String?) = runBlocking {
//        log.info("command line interface: $args")
//    }
//}
/*=================================================================================*/
