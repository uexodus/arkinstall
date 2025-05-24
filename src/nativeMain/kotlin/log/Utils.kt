package log

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.fprintf
import platform.posix.stderr
import kotlin.system.exitProcess

@OptIn(ExperimentalForeignApi::class)
fun fatal(e: Throwable): Nothing {
    val brightRedBold = "\u001B[1;91m"
    val reset = "\u001B[0m"
    val message = "${brightRedBold}${e::class.simpleName}${reset}: ${e.message}"

    fprintf(stderr, "%s\n", message)
    exitProcess(1)
}

fun logFatal(logger: Logger, e: Throwable): Nothing {
    logger.e { e.message ?: "No message" }
    fatal(e)
}

fun <T> Result<T>.getOrExit(): T = getOrElse { fatal(it) }