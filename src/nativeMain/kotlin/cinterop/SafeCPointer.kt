package cinterop

import cinterop.exceptions.InvalidBorrowStateException
import cinterop.exceptions.PointerAlreadyReleasedException
import cinterop.exceptions.ReleaseDuringBorrowException
import cinterop.exceptions.UseAfterFreeException
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import log.Logger
import log.logFatal
import kotlin.reflect.KClass

/**
 * A safe wrapper around a native [CPointer], enforcing Rust-like borrow rules at runtime.
 *
 * ### Terminology
 * A [SafeCPointer] can be either owned or borrowed:
 *  - Owned: A pointer where we are directly responsible for freeing.
 *  - Borrowed: A pointer where freeing responsibility is delegated to something else.
 *
 * ### Pointer states
 * At any given time, a [SafeCPointer] can have one of several [PointerState]:
 * - [PointerState.ACTIVE]: can be borrowed.
 * - [PointerState.IMMUTABLY_BORROWED]: temporarily locked for read-only access.
 * - [PointerState.MUTABLY_BORROWED] temporarily locked for exclusive write access.
 * - [PointerState.RELEASED]: inaccessible but not freed.
 * - [PointerState.FREED]: freed and inaccessible.
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
open class SafeCPointer<T : CPointed> protected constructor(
    private val cPointer: CPointer<T>,
    private var destroyer: ((CPointer<T>) -> Unit)? = null
) : AutoCloseable {
    private val logger = Logger(SafeCPointer::class)

    /**
     * The pointer from which this pointer was derived, or null if it's a root pointer
     */
    private var parent: SafeCPointer<*>? = null

    /**
     * All pointers that were derived from this pointer
     */
    private val children = mutableSetOf<SafeCPointer<*>>()

    /**
     * The current [PointerState] of this pointer
     */
    var state: PointerState = PointerState.ACTIVE
        protected set

    /** Whether this pointer's native memory should be freed by us or not */
    val isOwned: Boolean get() = destroyer != null

    /**
     * True if the pointer has been freed via [free]
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
    fun release() {
        if (isDead) {
            logger.d { "Avoided release on pointer ${toDebugString()}" }
            return
        }

        if (isBorrowed) logFatal(logger, ReleaseDuringBorrowException(this))

        for (child in children.toList()) {
            child.release()
        }

        parent?.removeChild(this)
        state = PointerState.RELEASED
        logger.d { "Pointer released ${toDebugString()}" }

        SafeCPointerRegistry.forget(cPointer)
    }

    /**
     * Frees this pointer and all of its children, recursively.
     * After calling this, the pointer enters the [PointerState.FREED] state.
     *
     * - Any child that is owned will be freed.
     * - Any child that is borrowed will be released.
     * - All freed/released pointers are removed from the [SafeCPointerRegistry].
     *
     * @throws ReleaseDuringBorrowException if the pointer is currently borrowed.
     */
    fun free() {
        if (!isOwned) {
            logger.i { "Tried to free borrowed pointer, releasing instead ${toDebugString()}" }
            release()
            return
        }

        if (isDead) {
            logger.d { "Avoided free on pointer ${toDebugString()}" }
            return
        }

        if (isBorrowed) logFatal(logger, ReleaseDuringBorrowException(this))

        for (child in children.toList()) {
            child.close()
        }

        try {
            destroyer!!.invoke(cPointer)
        } finally {
            parent?.removeChild(this)
            state = PointerState.FREED
            logger.d { "Pointer freed ${toDebugString()}" }
            SafeCPointerRegistry.forget(cPointer)
        }
    }


    /**
     * Registers one or more [child] pointers derived from this pointer.
     * Each child will be added to this pointer’s [children] set, and its [parent] will be set to this pointer.
     *
     * @throws PointerAlreadyReleasedException if this pointer has already been released.
     */
    internal fun addChild(vararg child: SafeCPointer<*>) {
        logger.d { "Adding child(ren) to ${toDebugString()}: ${child.joinToString { it.toDebugString() }}" }
        if (isReleased) logFatal(logger, PointerAlreadyReleasedException(this))

        for (c in child) {
            children.add(c)
            c.parent = this
        }
    }

    /**
     * Unregisters a previously added [child] pointer.
     * The child is removed from this pointer’s [children] set, and its [parent] reference is cleared.
     */
    internal fun removeChild(child: SafeCPointer<*>) {
        children.remove(child)
        child.parent = null
        logger.d { "Removed child ${child.toDebugString()} from ${toDebugString()}" }
    }

    internal fun demoteToBorrowed() {
        destroyer = null
    }

    override fun close() {
        if (isOwned) free() else release()
    }

    fun toDebugString(): String = "${this::class.simpleName}@${cPointer.rawValue} [$state]"

    companion object {
        /**
         * Retrieves an existing [SafeCPointer] instance for the given [cPointer] from the registry,
         * or creates and registers a new one if not present.
         */
        fun <T : CPointed, N : NativeType<T>> create(
            cPointer: CPointer<T>,
            klass: KClass<N>,
            free: ((CPointer<T>) -> Unit)? = null
        ): SafeCPointer<T> =
            SafeCPointerRegistry.getOrCreate(cPointer, klass) {
                SafeCPointer(cPointer, free)
            }

        inline fun <reified T : CPointed, reified N : NativeType<T>> create(
            cPointer: CPointer<T>,
            noinline free: ((CPointer<T>) -> Unit)? = null
        ): SafeCPointer<T> = create(cPointer, N::class, free)
    }
}

