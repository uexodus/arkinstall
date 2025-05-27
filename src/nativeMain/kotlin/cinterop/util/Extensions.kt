package cinterop.util

import cinterop.SafeCPointer
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
fun <T : CPointed> CPointer<T>.asList(
    creator: (CPointer<T>) -> SafeCPointer<T>,
    next: (CPointer<T>) -> CPointer<T>?,
): List<SafeCPointer<T>> {
    val result = mutableListOf<SafeCPointer<T>>()
    var current: SafeCPointer<T>? = creator(this@asList)

    while (current != null) {
        result.add(current)
        current = current.immut { ptr ->
            next(ptr)?.let { creator(it) }
        }
    }
    return result
}