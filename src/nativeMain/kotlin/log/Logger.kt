package log

import log.types.LogLevel
import kotlin.reflect.KClass

class Logger(private val owner: KClass<*>) {
    /** Logs a [LogLevel.DEBUG] message **/
    fun d(msg: () -> String) = log(LogLevel.DEBUG, msg)

    /** Logs a [LogLevel.INFO] message **/
    fun i(msg: () -> String) = log(LogLevel.INFO, msg)

    /** Logs a [LogLevel.WARNING] message **/
    fun w(msg: () -> String) = log(LogLevel.WARNING, msg)

    /** Logs a [LogLevel.ERROR] message **/
    fun e(exception: Throwable) = error(exception)

    fun log(level: LogLevel, msg: () -> String) {
        for (backend in LogConfiguration.backends) {
            backend.log(level, owner, msg().trim())
        }
    }

    private fun error(exception: Throwable) {
        for (backend in LogConfiguration.backends) {
            backend.error(owner, exception::class, exception.message ?: "Exception has no message.")
        }
    }
}