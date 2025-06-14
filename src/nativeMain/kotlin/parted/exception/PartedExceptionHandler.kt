package parted.exception

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
import log.Logger
import native.libparted.PED_EXCEPTION_IGNORE
import native.libparted.PED_EXCEPTION_UNHANDLED
import native.libparted.PedException
import native.libparted.PedExceptionOption
import parted.PartedException
import parted.PartedException.Companion.toLogLevel
import parted.bindings.PartedBindings
import parted.exception.PartedExceptionHandler.drainExceptions
import parted.types.PartedExceptionType

@OptIn(ExperimentalForeignApi::class)
object PartedExceptionHandler {
    var registered: Boolean = false

    private val exceptions: MutableSet<PartedException> = mutableSetOf()

    private val handler = staticCFunction<CPointer<PedException>?, PedExceptionOption> {
        if (it == null) return@staticCFunction PED_EXCEPTION_UNHANDLED

        val exception = PartedException.createBorrowed(it)
        exceptions.add(exception)
        PED_EXCEPTION_IGNORE
    }

    fun register() {
        PartedBindings.setExceptionHandler(handler)
        registered = true
    }

    fun drainExceptions(): List<PartedException> {
        val drained = exceptions.toList()
        exceptions.clear()
        return drained
    }
}

inline fun <T, reified E : Exception> partedTry(
    noinline exceptionFactory: (String) -> E,
    block: () -> T
): T {
    if (!PartedExceptionHandler.registered) PartedExceptionHandler.register()
    val result = try {
        block()
    } finally {
        val exceptions = drainExceptions()
        val logger = Logger(E::class)

        val errors: ArrayList<PartedException> = arrayListOf()

        for (e in exceptions) {
            if (e.type < PartedExceptionType.ERROR) logger.log(e.type.toLogLevel()) { e.message }
            else errors.add(e)
        }
        if (errors.isNotEmpty()) {
            throw exceptionFactory(exceptions.joinToString("\n") { it.message })
        }
    }

    return result
}

inline fun <T, reified E : Exception> partedTryNotNull(
    noinline exceptionFactory: (String) -> E,
    defaultNullMessage: String,
    block: () -> T?
): T {
    return partedTry(exceptionFactory, block)
        ?: throw exceptionFactory(defaultNullMessage)
}

