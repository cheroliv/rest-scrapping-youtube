package webapp

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import webapp.Logging.d
import webapp.Logging.e
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.Future

@EnableAsync
@Configuration
@EnableScheduling
@Suppress("unused")
class Scheduling(
    private val taskExecutionProperties: TaskExecutionProperties
) : AsyncConfigurer {


    @Bean(name = ["taskExecutor"])
    override fun getAsyncExecutor(): Executor = ExceptionHandlingAsyncTaskExecutor(ThreadPoolTaskExecutor().apply {
        queueCapacity = taskExecutionProperties.pool.queueCapacity
        @Suppress("UsePropertyAccessSyntax") setThreadNamePrefix(taskExecutionProperties.threadNamePrefix)
        corePoolSize = taskExecutionProperties.pool.coreSize
        maxPoolSize = taskExecutionProperties.pool.maxSize
    }).also { d("Creating Async Task Executor") }

    override fun getAsyncUncaughtExceptionHandler(): AsyncUncaughtExceptionHandler =
        SimpleAsyncUncaughtExceptionHandler()


    class ExceptionHandlingAsyncTaskExecutor(
        private val executor: AsyncTaskExecutor
    ) : AsyncTaskExecutor, InitializingBean, DisposableBean {
        companion object {
            const val EXCEPTION_MESSAGE = "Caught async exceptions"
        }

        override fun execute(task: Runnable): Unit = executor.execute(createWrappedRunnable(task))

        @Suppress("OVERRIDE_DEPRECATION")
        override fun execute(task: Runnable, startTimeout: Long): Unit =
            executor.execute(createWrappedRunnable(task))

        private fun <T> createCallable(task: Callable<T>): Callable<T> = Callable {
            try {
                return@Callable task.call()
            } catch (e: Exception) {
                handle(e)
                throw e
            }
        }

        private fun createWrappedRunnable(task: Runnable): Runnable = Runnable {
            try {
                task.run()
            } catch (e: Exception) {
                handle(e)
            }
        }

        private fun handle(e: Exception?): Unit = e(EXCEPTION_MESSAGE, e)

        override fun submit(task: Runnable): Future<*> = executor.submit(createWrappedRunnable(task))

        override fun <T> submit(task: Callable<T>): Future<T> = executor.submit(createCallable(task))

        @Throws(Exception::class)
        override fun destroy() {
            if (executor is DisposableBean) (executor as DisposableBean).apply(DisposableBean::destroy)
        }

        @Throws(Exception::class)
        override fun afterPropertiesSet() {
            if (executor is InitializingBean) (executor as InitializingBean).apply { afterPropertiesSet() }
        }
    }
}

/*=================================================================================*/
