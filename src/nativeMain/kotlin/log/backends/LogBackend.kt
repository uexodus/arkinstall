package log.backends

import log.types.LogLevel
import kotlin.reflect.KClass

interface LogBackend {
    var logLevel: LogLevel
    fun log(level: LogLevel, module: KClass<*>, message: String)
    fun error(module: KClass<*>, exception: KClass<*>, message: String)
    fun close() {}
}
