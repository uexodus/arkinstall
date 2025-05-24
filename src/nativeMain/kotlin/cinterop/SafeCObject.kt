package cinterop

import kotlinx.cinterop.CPointed
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
interface SafeCObject<T : CPointed> : AutoCloseable {
    val pointer: SafeCPointer<T>

    fun summary(): String
}