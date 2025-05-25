package cinterop.util

import cinterop.OwnedSafeCPointer
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
fun <T : CPointed> CPointer<T>.asList(
    creator: (CPointer<T>) -> OwnedSafeCPointer<T>,
    next: (CPointer<T>) -> CPointer<T>?,
): List<OwnedSafeCPointer<T>> {
    val result = mutableListOf<OwnedSafeCPointer<T>>()
    var current: OwnedSafeCPointer<T>? = creator(this@asList)

    while (current != null) {
        result.add(current)
        current = current.immut { ptr ->
            next(ptr)?.let { creator(it) }
        }
    }
    return result
}