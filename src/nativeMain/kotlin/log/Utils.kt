package log

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.fprintf
import platform.posix.stderr
import kotlin.system.exitProcess

@OptIn(ExperimentalForeignApi::class)
fun printlnError(message: String) = fprintf(stderr, "%s\n", message)

fun logFatal(logger: Logger, exception: Throwable): Nothing {
    logger.e(exception)
    exitProcess(1)
}

inline fun <reified T> Result<T>.getOrExit(logger: Logger): T = getOrElse { exception ->
    logFatal(logger, exception)
}

inline fun <reified T : Any> logger(): Logger = Logger(T::class)