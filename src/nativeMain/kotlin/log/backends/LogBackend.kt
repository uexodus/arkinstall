package log.backends

import log.types.LogLevel

interface LogBackend {
    val logLevel: LogLevel
    fun log(level: LogLevel, message: String)
    fun close() {}
}
