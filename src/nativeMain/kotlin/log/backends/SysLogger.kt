package log.backends

import kotlinx.cinterop.*
import log.LogConfiguration
import log.bindings.SyslogBindings
import log.types.LogLevel
import kotlin.reflect.KClass

@OptIn(ExperimentalForeignApi::class)
class SysLogger(override val logLevel: LogLevel) : LogBackend {
    private val arena = Arena()
    private var isLogOpen = false
    private lateinit var identityPointer: CPointer<ByteVar>

    override fun log(level: LogLevel, module: KClass<*>, message: String) {
        if (level > logLevel) return
        if (!isLogOpen) initialise()

        val logMessage = "${module.simpleName} - $message"

        SyslogBindings.systemLog(level.ordinal, logMessage)
    }

    override fun error(module: KClass<*>, exception: KClass<*>, message: String) {
        if (LogLevel.ERROR > logLevel) return
        if (!isLogOpen) initialise()

        val logMessage = "${module.simpleName} - [${exception.simpleName}] $message"

        SyslogBindings.systemLog(LogLevel.ERROR.ordinal, logMessage)
    }

    private fun initialise() {
        identityPointer = LogConfiguration.PROGRAM_NAME.cstr.getPointer(arena)
        SyslogBindings.openLog(identityPointer)
        isLogOpen = true
    }

    override fun close() {
        if (!isLogOpen) return
        arena.clear()
        isLogOpen = false
        SyslogBindings.closeLog()
    }
}
