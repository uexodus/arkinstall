package cinterop.util

import cinterop.SafeCPointer
import cinterop.types.NativeCPointerVarByteVar
import kotlinx.cinterop.*
import log.logFatal
import log.logger

@OptIn(ExperimentalForeignApi::class)
fun <T : CPointed, R : SafeCPointer<T>> CPointer<T>.asList(
    creator: (CPointer<T>) -> R,
    next: (CPointer<T>) -> CPointer<T>?,
): List<R> {
    val result = mutableListOf<R>()
    var current: R? = creator(this@asList)

    while (current != null) {
        result.add(current)
        current = current.immut { ptr ->
            next(ptr)?.let { creator(it) }
        }
    }
    return result
}

/** Converts a list of strings into a c array of strings terminated by a null */
@OptIn(ExperimentalForeignApi::class)
fun List<String>.toNullTerminatedCString(autofreeScope: AutofreeScope): SafeCPointer<CPointerVarOf<CPointer<ByteVar>>> {
    val cPointer = autofreeScope.allocArrayOf(map { it.cstr.getPointer(autofreeScope) } + null)
    return SafeCPointer.create(cPointer, NativeCPointerVarByteVar::class)
}

/** Reads a file descriptor stream into a buffer of the given [size] */
@OptIn(ExperimentalForeignApi::class)
fun SafeCPointer<IntVar>.read(size: Int): ByteArray {
    val buffer = ByteArray(size)
    val bytesRead = buffer.usePinned { buf ->
        immut { fd ->
            platform.posix.read(
                fd.pointed.value,
                buf.addressOf(0),
                buffer.size.toULong()
            ).toInt()
        }
    }

    return when {
        bytesRead < 0 -> logFatal(logger<SafeCPointer<IntVar>>(), RuntimeException("read() on file descriptor failed."))
        bytesRead == 0 -> byteArrayOf()
        bytesRead < size -> buffer.copyOf(bytesRead)
        else -> buffer
    }
}
