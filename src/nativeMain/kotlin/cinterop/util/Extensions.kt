package cinterop.util

import cinterop.SafeCPointer
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi

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