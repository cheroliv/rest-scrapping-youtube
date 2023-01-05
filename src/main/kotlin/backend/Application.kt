package backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.*

/*=================================================================================*/

@SpringBootApplication
@EnableConfigurationProperties(ApplicationProperties::class)
class BackendApplication
/*=================================================================================*/

object BackendBootstrap {
    @JvmStatic
    fun main(args: Array<String>) = runApplication<BackendApplication>(*args).bootstrapLog()
}

/*=================================================================================*/

//object CliBootstrap {
//    @JvmStatic
//    fun main(args: Array<String>) {
//        runApplication<BackendApplication>(*args) {
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
