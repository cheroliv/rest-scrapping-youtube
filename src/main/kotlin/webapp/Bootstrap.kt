package webapp

import org.springframework.boot.runApplication
import webapp.Logging.bootstrapLog
import webapp.Logging.checkProfileLog
import webapp.Logging.`continue`

object Bootstrap {
    @JvmStatic
    fun main(args: Array<String>) = runApplication<Application>(*args)
        .checkProfileLog()
        .bootstrapLog()
        .`continue`()
}