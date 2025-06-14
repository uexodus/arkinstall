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
    fun e(msg: () -> String) = log(LogLevel.ERROR, msg)

    fun log(level: LogLevel, msg: () -> String) {
        val logText = "${owner.simpleName} - ${msg().trim()}"
        for (backend in LogConfiguration.backends) {
            backend.log(level, logText)
        }
    }
}