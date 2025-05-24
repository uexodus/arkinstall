package cinterop

import kotlinx.cinterop.CPointed
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
interface OwnedSafeCObject<T : CPointed> : SafeCObject<T> {
    override val pointer: OwnedSafeCPointer<T>
}