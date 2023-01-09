package webapp

import org.springframework.boot.runApplication
import webapp.Logging.bootstrapLog
import webapp.Logging.checkProfileLog
import webapp.Logging.`continue`

/*=================================================================================*/

/*=================================================================================*/

object Bootstrap {
    @JvmStatic
    fun main(args: Array<String>) = runApplication<Application>(*args)
        .checkProfileLog()
        .bootstrapLog()
        .`continue`()

//    @JvmStatic
//    val log: Logger by lazy { getLogger(Application::class.java) }
}

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
