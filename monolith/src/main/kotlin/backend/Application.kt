@file:Suppress("unused")

package backend

import backend.Constants.SPRING_PROFILE_CONF_DEFAULT_KEY
import backend.Constants.SPRING_PROFILE_DEVELOPMENT
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(ApplicationProperties::class)
class BackendApplication

object BackendBootstrap {
    @JvmStatic
    fun main(args: Array<String>) = runApplication<BackendApplication>(*args) {
        setDefaultProperties(hashMapOf<String, Any>(SPRING_PROFILE_CONF_DEFAULT_KEY to SPRING_PROFILE_DEVELOPMENT))
        setAdditionalProfiles(SPRING_PROFILE_DEVELOPMENT)
    }.run { bootstrapLog(this) }
}

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