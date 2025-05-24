package cinterop.exceptions

import cinterop.SafeCPointer
import kotlin.reflect.KClass

/**
 * Base class for SafeCPointer exceptions.
 */
sealed class SafeCPointerException(message: String) : IllegalStateException(message)

private fun SafeCPointer<*>.stateName(): String = state.name.lowercase()

/**
 * Thrown when attempting to use a pointer after it has been freed.
 */
class UseAfterFreeException(
    pointer: SafeCPointer<*>,
    accessFunction: String
) : SafeCPointerException(
    "Attempted to use $pointer in '$accessFunction' after it was marked as '${pointer.stateName()}'!"
)

/**
 * Thrown when releasing a pointer while it is borrowed.
 */
class ReleaseDuringBorrowException(
    pointer: SafeCPointer<*>
) : SafeCPointerException(
    "Cannot release $pointer inside of a immut or mut block!"
)

/**
 * Thrown when performing an operation on a pointer that's already borrowed.
 */
class InvalidBorrowStateException(
    pointer: SafeCPointer<*>,
    attemptedAccess: String
) : SafeCPointerException(
    "Cannot perform '$attemptedAccess' on $pointer because it is already borrowed!"
)

/**
 * Thrown when a pointer is registered under conflicting types.
 */
class PointerRedefinitionException(
    existing: KClass<*>,
    attempted: KClass<*>
) : SafeCPointerException(
    "Pointer already registered as '${existing.simpleName}' cannot be re-registered as '${attempted.simpleName}'."
)

/**
 * Thrown when a pointer's pointed type conflicts with an existing registration.
 */
class PointedTypeRedefinitionException(
    existing: KClass<*>,
    attempted: KClass<*>
) : SafeCPointerException(
    "Pointed type '${attempted.simpleName}' conflicts with existing type '${existing.simpleName}' in the pointer store."
)

/**
 * Thrown when adding a child to a pointer that's already released.
 */
class PointerAlreadyReleasedException(
    pointer: SafeCPointer<*>
) : SafeCPointerException(
    "Cannot add a child to $pointer because it has already been released."
)
