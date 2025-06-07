package cinterop.exceptions

import cinterop.SafeCPointer
import kotlin.reflect.KClass

/**
 * Base class for SafeCPointer exceptions.
 */
sealed class SafeCPointerException(message: String) : IllegalStateException(message)

/**
 * Thrown when attempting to use a pointer after it has been freed.
 */
class UseAfterFreeException(
    pointer: SafeCPointer<*>,
    accessFunction: String
) : SafeCPointerException(
    "Attempted to use ${pointer.toDebugString()} in '$accessFunction' after it was marked freed/released!"
)

/**
 * Thrown when releasing a pointer while it is borrowed.
 */
class ReleaseDuringBorrowException(
    pointer: SafeCPointer<*>
) : SafeCPointerException(
    "Cannot release ${pointer.toDebugString()} inside of a immut or mut block!"
)

/**
 * Thrown when performing an operation on a pointer that's already borrowed.
 */
class InvalidBorrowStateException(
    pointer: SafeCPointer<*>,
    attemptedAccess: String
) : SafeCPointerException(
    "Cannot perform '$attemptedAccess' on ${pointer.toDebugString()} because it is already borrowed!"
)

/**
 * Thrown when a pointer's pointed type conflicts with an existing registration.
 */
class PointedTypeRedefinitionException(
    existing: KClass<*>,
    attempted: KClass<*>
) : SafeCPointerException(
    "Pointed type '${attempted.simpleName}' conflicts with existing type '${existing.simpleName}' " +
            "in the pointer store."
)

/**
 * Thrown when adding a child to a pointer that's already released.
 */
class PointerAlreadyReleasedException(
    pointer: SafeCPointer<*>
) : SafeCPointerException(
    "Cannot add a child to ${pointer.toDebugString()} because it has already been released."
)
