package cinterop

import cinterop.exceptions.InvalidBorrowStateException
import cinterop.exceptions.PointerAlreadyReleasedException
import cinterop.exceptions.ReleaseDuringBorrowException
import cinterop.exceptions.UseAfterFreeException
import kotlinx.cinterop.*
import log.Logger
import log.logFatal
import kotlin.reflect.KClass

/**
 * A safe wrapper around a native [CPointer], enforcing Rust-like borrow rules at runtime.
 *
 * This class does **not** take ownership of the pointer it wraps.
 *
 * It assumes that the pointer is managed externally, usually by a C library or a Kotlin [AutofreeScope].
 * As such, [SafeCPointer] does **not** support memory freeing. If you need to manually free memory,
 * use [OwnedSafeCPointer], which accepts a free lambda.
 *
 * ### Purpose
 * Kotlin/Native interop with C exposes raw pointers that are inherently unsafe:
 * - They can be used after being freed.
 * - Multiple mutable accesses can lead to undefined behavior.
 * - There is no compiler-level enforcement of pointer lifetimes or exclusivity.
 *
 * This class provides a Rust-like runtime borrow-checking system to attempt to mitigate these risks
 *
 * ### Pointer states
 * At any given time, a [SafeCPointer] can have one of several [PointerState]:
 * - [PointerState.ACTIVE]: can be borrowed.
 * - [PointerState.IMMUTABLY_BORROWED]: temporarily locked for read-only access.
 * - [PointerState.MUTABLY_BORROWED] temporarily locked for exclusive write access.
 * - [PointerState.RELEASED]: inaccessible but not freed.
 * - [PointerState.FREED]: freed and inaccessible. See [OwnedSafeCPointer] for freeing
 *
 * Borrowing is always done through scoped lambdas.
 *
 * ```kotlin
 * val safePtr = SafeCPointer.create(...)
 * safePtr.immut { rawPtr -> ... }  // For shared read-only access
 * safePtr.mut { rawPtr -> ... }    // For exclusive read & write access
 * ```
 *
 * These help us enforce borrow rules:
 * - Disallows access to pointers that have been released or freed.
 * - Prevents any writes while the pointer is already borrowed (read or write).
 * - Prevents simultaneous reads and writes to the same pointer.
 * - Prevents releasing or freeing while the pointer is being read or written
 *
 * However, this class cannot prevent you from mutating inside the immutable borrow block.
 * The system relies on **you** to follow the borrow semantics correctly.
 *
 * ### Parent–child relationships
 * [SafeCPointer] supports parent–child hierarchies, which is useful when working with pointers derived from
 * a parent, such as struct fields, array slices, or pointer offsets.
 * Releasing a parent pointer recursively releases all of its added child pointers,
 * which prevents accidental use of memory when the parent is no longer valid.
 *
 * ```kotlin
 * val parent = SafeCPointer.create(...)
 * val child = SafeCPointer.create(...)
 * parent.addChild(child)
 *
 * parent.release()   // also releases child
 * child.immut { ... } // throws: already released
 * ```
 */
@OptIn(ExperimentalForeignApi::class)
open class SafeCPointer<T : CPointed> protected constructor(protected val cPointer: CPointer<T>) : AutoCloseable {
    private val logger = Logger(SafeCPointer::class)

    /**
     * The pointer from which this pointer was derived, or null if this is a root.
     */
    var parent: SafeCPointer<*>? = null
        protected set

    private val _children = mutableSetOf<SafeCPointer<*>>()

    /**
     * All pointers that were derived from this pointer
     */
    val children: Set<SafeCPointer<*>> get() = _children.toSet()

    /**
     * The current [PointerState] of this pointer
     */
    var state: PointerState = PointerState.ACTIVE
        protected set

    /**
     * True if the pointer has been freed via [OwnedSafeCPointer.free]
     */
    val isFreed get() = state == PointerState.FREED

    /**
     * True if the pointer has been released via [SafeCPointer.release] (but not freed)
     */
    val isReleased: Boolean get() = state == PointerState.RELEASED

    /**
     * True if the pointer cannot be borrowed for any reason (freed or released)
     */
    val isDead: Boolean get() = isFreed || isReleased

    /**
     * True while any immutable or mutable borrow occurs
     */
    val isBorrowed: Boolean
        get() = state == PointerState.MUTABLY_BORROWED || state == PointerState.IMMUTABLY_BORROWED

    /**
     * For shared read-only access to the wrapped [CPointer].
     *
     * @throws UseAfterFreeException if the pointer is marked as freed or released
     * @throws InvalidBorrowStateException if the pointer is currently mutably borrowed
     */
    fun <R> immut(block: (CPointer<T>) -> R): R {
        if (isDead) logFatal(logger, UseAfterFreeException(this, "immut"))
        if (state == PointerState.MUTABLY_BORROWED) logFatal(logger, InvalidBorrowStateException(this, "immut"))

        val previousState = state
        state = PointerState.IMMUTABLY_BORROWED

        try {
            return block(cPointer)
        } finally {
            state = previousState
        }
    }

    /**
     * For exclusive read-write access to the wrapped [CPointer].
     *
     * @throws UseAfterFreeException if the pointer is marked as freed or released
     * @throws InvalidBorrowStateException if the pointer is currently immutably or mutably borrowed
     */
    fun <R> mut(block: (CPointer<T>) -> R): R {
        if (isDead) logFatal(logger, UseAfterFreeException(this, "mut"))
        if (state != PointerState.ACTIVE) logFatal(logger, InvalidBorrowStateException(this, "mut"))

        state = PointerState.MUTABLY_BORROWED

        try {
            return block(cPointer)
        } finally {
            state = PointerState.ACTIVE
        }
    }

    /**
     * Marks this pointer and all of its children as released.
     * After calling this, the pointer becomes logically invalid (but not freed).
     * It cannot be borrowed, and it is removed from the [SafeCPointerRegistry].
     *
     * @throws ReleaseDuringBorrowException if the pointer is currently borrowed.
     */
    open fun release() {
        if (isReleased) {
            logger.d { "Avoided release on released pointer $this" }
            return
        }

        if (isBorrowed) logFatal(logger, ReleaseDuringBorrowException(this))

        for (child in _children.toList()) {
            child.release()
        }

        parent?.removeChild(this)
        state = PointerState.RELEASED
        logger.d { "Pointer released $this" }

        SafeCPointerRegistry.forget(cPointer)
    }

    /**
     * Registers one or more [child] pointers derived from this pointer.
     * Each child will be added to this pointer’s [children] set, and its [parent] will be set to this pointer.
     *
     * @throws PointerAlreadyReleasedException if this pointer has already been released.
     */
    fun addChild(vararg child: SafeCPointer<*>) {
        logger.d { "Adding child(ren) to $this: ${child.joinToString()}" }
        if (isReleased) logFatal(logger, PointerAlreadyReleasedException(this))

        for (c in child) {
            _children.add(c)
            c.parent = this
        }
    }

    /**
     * Unregisters a previously added [child] pointer.
     * The child is removed from this pointer’s [_children] set, and its [parent] reference is cleared.
     */
    fun removeChild(child: SafeCPointer<*>) {
        _children.remove(child)
        child.parent = null
        logger.d { "Child removed $child from parent $this" }
    }

    override fun close() {
        release()
    }

    override fun toString(): String {
        return "${this::class.simpleName}(${cPointer.rawValue}, $state)"
    }

    companion object {
        /**
         * Retrieves an existing [SafeCPointer] instance for the given [cPointer] from the registry,
         * or creates and registers a new one if not present.
         */
        fun <T : CPointed, N : NativeType<T>> create(cPointer: CPointer<T>, klass: KClass<N>): SafeCPointer<T> {
            return SafeCPointerRegistry.getOrCreateSafeCPointer(cPointer, klass) {
                SafeCPointer(cPointer)
            }
        }
    }
}

