package cinterop

import kotlinx.cinterop.CPointed
import kotlinx.cinterop.ExperimentalForeignApi

/**
 * Interface for identifying a native pointer type.
 *
 * Used for runtime type checks in the [SafeCPointerRegistry]
 */
@OptIn(ExperimentalForeignApi::class)
interface NativeType<T : CPointed>