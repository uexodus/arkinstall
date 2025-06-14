package log.backends

import kotlinx.cinterop.*
import log.LogConfiguration
import log.bindings.SyslogBindings
import log.types.LogLevel

@OptIn(ExperimentalForeignApi::class)
class SysLogger(override val logLevel: LogLevel = LogLevel.INFO) : LogBackend {
    private val arena = Arena()
    private var isLogOpen = false
    private lateinit var identityPointer: CPointer<ByteVar>

    override fun log(level: LogLevel, message: String) {
        if (level > logLevel) return
        if (!isLogOpen) initialise()

        SyslogBindings.systemLog(level.ordinal, message)
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
