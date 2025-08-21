package log.backends

import log.printlnError
import log.types.LogLevel
import kotlin.reflect.KClass
import kotlin.system.exitProcess

class ConsoleLogger(override var logLevel: LogLevel) : LogBackend {
    override fun log(level: LogLevel, module: KClass<*>, message: String) {
        if (level > logLevel) return

        when (level) {
            LogLevel.DEBUG -> debug(message)
            LogLevel.INFO -> info(message)
            LogLevel.NOTICE -> info(message)
            LogLevel.WARNING -> warn(message)
            else -> {
                printlnError("Unexpected log level, for errors, use the error() function: $level")
                exitProcess(1)
            }
        }
    }

    override fun error(module: KClass<*>, exception: KClass<*>, message: String) {
        if (LogLevel.ERROR > logLevel) return
        printlnError("$RED_BOLD${exception.simpleName} - $message$RESET")
    }

    private fun debug(message: String) = printlnError("$GRAY$message$RESET")

    private fun info(message: String) = printlnError("$WHITE$message$RESET")

    private fun warn(message: String) = printlnError("$YELLOW$message$RESET")

    companion object {
        private const val RESET = "\u001B[0m"
        private const val GRAY = "\u001B[90m"
        private const val WHITE = "\u001B[97m"
        private const val YELLOW = "\u001B[33m"
        private const val RED_BOLD = "\u001B[1;31m"
    }
}