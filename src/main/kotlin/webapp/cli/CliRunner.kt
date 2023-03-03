@file:Suppress("unused")

package webapp.cli

import kotlinx.coroutines.runBlocking
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import webapp.Constants.CLI
import webapp.Logging.i

@Component
@Profile(CLI)
class CliRunner : CommandLineRunner {
    override fun run(vararg args: String?) = runBlocking {
        i("command line interface: $args")
    }
}