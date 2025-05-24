package cinterop

import cinterop.exceptions.ReleaseDuringBorrowException
import kotlinx.cinterop.*
import log.Logger
import log.logFatal
import kotlin.reflect.KClass

/**
 * A [SafeCPointer] that **owns** the pointer it wraps and is responsible for freeing it.
 *
 * [OwnedSafeCPointer] is intended for interop with C libraries where **you are responsible**
 * for manually freeing native memory, such as:
 *
 * - Pointers returned by C APIs that require the caller to clean up
 * - Pointers allocated in Kotlin using [nativeHeap], which must be released using [nativeHeap.free]
 *
 * Unlike the [SafeCPointer] you provide a custom free lambda to control how the pointer is freed.
 *
 * ### Example:
 * ```kotlin
 * val unsafePtr = nativeHeap.alloc<IntVar>().ptr
 * val ownedPtr = OwnedSafeCPointer.create(unsafePointer, NativeIntVar::class) {
 *     nativeHeap.free(it)
 * }
 *
 * ownedPointer.mut { it.pointed.value = 15 }
 * ownedPointer.free() // Frees the pointer
 *
 * // Safer alternative using `.use {}` to auto-free:
 * val ownedPointer = OwnedSafeCPointer.create(unsafePtr, NativeIntVar::class) {
 *     nativeHeap.free(it)
 * }
 *
 * ownedPointer.use { pointer ->
 *     pointer.mut { it.pointed.value = 42 }
 * }
 * ownedPointer.immut { println(it) } // throws: ownedPointer is freed
 * //
 * ```
 */
@OptIn(ExperimentalForeignApi::class)
class OwnedSafeCPointer<T : CPointed> private constructor(
    cPointer: CPointer<T>,
    private val freeLambda: (CPointer<T>) -> Unit
) : SafeCPointer<T>(cPointer), AutoCloseable {
    private val logger = Logger(OwnedSafeCPointer::class)

    /**
     * Frees this pointer and all of its children, recursively.
     *
     * After calling this, the pointer enters the [PointerState.FREED] state.
     * This indicates that the underlying memory has been released, and the pointer is no longer usable.
     *
     * - Any child that is a [OwnedSafeCPointer] will be freed.
     * - Any child that is a [SafeCPointer] will be released.
     * - All freed/released pointers are removed from the [SafeCPointerRegistry].
     *
     * @throws ReleaseDuringBorrowException if the pointer is currently borrowed.
     */
    fun free() {
        if (isDead) {
            logger.d { "Avoided free on freed pointer $this" }
            return
        }

        if (isBorrowed) logFatal(logger, ReleaseDuringBorrowException(this))

        for (child in children) {
            if (child is OwnedSafeCPointer<*>) {
                child.free()
            } else {
                child.release()
            }
        }

        try {
            freeLambda(cPointer)
        } finally {
            parent?.removeChild(this)
            state = PointerState.FREED
            logger.d { "Pointer freed $this" }
            SafeCPointerRegistry.forget(cPointer)
        }
    }

    override fun release() {
        if (isFreed) return
        super.release()
    }

    override fun close() {
        free()
    }

    companion object {
        fun <T : CPointed, N : NativeType<T>> create(
            cPointer: CPointer<T>,
            klass: KClass<N>,
            destroyer: (CPointer<T>) -> Unit
        ): OwnedSafeCPointer<T> {
            return SafeCPointerRegistry.getOrCreateOwnedSafeCPointer(cPointer, klass) {
                OwnedSafeCPointer(cPointer, destroyer)
            }
        }
    }
}