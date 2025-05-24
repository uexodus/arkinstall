package log

import log.types.LogLevel
import kotlin.reflect.KClass

class Logger(private val owner: KClass<*>) {
    /** Logs a [LogLevel.DEBUG] message **/
    fun d(msg: () -> String) = write(LogLevel.DEBUG, msg)

    /** Logs a [LogLevel.INFO] message **/
    fun i(msg: () -> String) = write(LogLevel.INFO, msg)

    /** Logs a [LogLevel.WARNING] message **/
    fun w(msg: () -> String) = write(LogLevel.WARNING, msg)

    /** Logs a [LogLevel.ERROR] message **/
    fun e(msg: () -> String) = write(LogLevel.ERROR, msg)


    private fun write(level: LogLevel, msg: () -> String) {
        val logText = "${owner.simpleName} - ${msg().trim()}"
        for (backend in LogConfiguration.backends) {
            backend.log(level, logText)
        }
    }
}