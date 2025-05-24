package cinterop

import cinterop.exceptions.PointedTypeRedefinitionException
import cinterop.exceptions.PointerRedefinitionException
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import log.Logger
import log.logFatal
import kotlin.reflect.KClass

/**
 * A global registry that tracks all active [SafeCPointer] instances.
 *
 * Prevents multiple [SafeCPointer] or [OwnedSafeCPointer] instances from wrapping the same raw address,
 * which would bypass the safety mechanisms these wrappers are supposed to enforce.
 *
 * - A single address can be wrapped by only one [SafeCPointer] or [OwnedSafeCPointer] instance at a time.
 * - Each pointer must be associated with a single pointed type, which cannot change until the pointer is freed or released.
 */
@OptIn(ExperimentalForeignApi::class)
object SafeCPointerRegistry {
    private val logger = Logger(SafeCPointerRegistry::class)
    private val registry = mutableMapOf<Long, Entry<out CPointed>>()

    private sealed class Entry<T : CPointed>(
        val pointedType: KClass<*>,
        val pointer: SafeCPointer<T>,
    ) {
        class Safe<T : CPointed>(
            pointedType: KClass<*>,
            pointer: SafeCPointer<T>
        ) : Entry<T>(pointedType, pointer)

        class Owned<T : CPointed>(
            pointedType: KClass<*>,
            pointer: SafeCPointer<T>
        ) : Entry<T>(pointedType, pointer)
    }

    /**
     * Retrieves an existing [SafeCPointer] instance for the given [cPointer] from the registry,
     * or creates and registers a new one using the provided [create] lambda.
     *
     * @throws PointerRedefinitionException if the pointer was previously registered as a non-owning [SafeCPointer].
     * @throws PointedTypeRedefinitionException if a pointed type was previously registered as a different type.
     */
    fun <T : CPointed, N : NativeType<T>> getOrCreateSafeCPointer(
        cPointer: CPointer<T>,
        expectedPointedType: KClass<N>,
        create: () -> SafeCPointer<T>
    ): SafeCPointer<T> {
        return getOrCreate(
            cPointer,
            expectedPointedType,
            create,
            Entry.Safe::class,
            Entry.Owned::class,
            PointerRedefinitionException(
                OwnedSafeCPointer::class,
                SafeCPointer::class
            )
        )
    }

    /**
     * Retrieves an existing [OwnedSafeCPointer] instance for the given [cPointer] from the registry, or
     * creates and registers a new one using the provided [create] lambda.
     *
     * @throws PointerRedefinitionException if the pointer was previously registered as a non-owning [SafeCPointer].
     * @throws PointedTypeRedefinitionException if a pointed type was previously registered as a different type.
     */
    fun <T : CPointed, N : NativeType<T>> getOrCreateOwnedSafeCPointer(
        cPointer: CPointer<T>,
        expectedPointedType: KClass<N>,
        create: () -> OwnedSafeCPointer<T>
    ): OwnedSafeCPointer<T> {
        return getOrCreate(
            cPointer,
            expectedPointedType,
            create,
            Entry.Owned::class,
            Entry.Safe::class,
            PointerRedefinitionException(SafeCPointer::class, OwnedSafeCPointer::class)
        )
    }

    private fun <T : CPointed, R : SafeCPointer<T>, N : NativeType<T>> getOrCreate(
        cPointer: CPointer<T>,
        expectedPointedType: KClass<N>,
        create: () -> R,
        expectedEntryType: KClass<out Entry<*>>,
        conflictingEntryType: KClass<out Entry<*>>,
        conflictException: PointerRedefinitionException
    ): R {
        val address = cPointer.rawValue.toLong()

        registry[address]?.let { entry ->
            if (entry::class == conflictingEntryType) {
                logFatal(logger, conflictException)
            }

            if (entry.pointedType != expectedPointedType) {
                logFatal(logger, PointedTypeRedefinitionException(entry.pointedType, expectedPointedType))
            }

            logger.d { "Reusing ${entry.pointer} type: (${expectedPointedType.qualifiedName})" }

            @Suppress("UNCHECKED_CAST")
            return entry.pointer as R
        }

        val pointer = create()
        registry[address] = when (expectedEntryType) {
            Entry.Safe::class -> {
                Entry.Safe(expectedPointedType, pointer)
            }

            Entry.Owned::class -> {
                Entry.Owned(expectedPointedType, pointer)
            }

            else -> error("Unsupported entry type: $expectedEntryType")
        }
        logger.d { "Registered $pointer type: (${expectedPointedType.qualifiedName})" }
        return pointer
    }

    /**
     * Removes the registered [SafeCPointer] or [OwnedSafeCPointer] for the given [cPointer] address.
     */
    fun forget(cPointer: CPointer<*>) {
        registry.remove(cPointer.rawValue.toLong())
            ?.let {
                logger.d { "Forgotten ${it.pointer} type: (${it.pointedType.qualifiedName})" }
            }
            ?: logger.w { "Attempted to remove unknown pointer $cPointer" }
    }
}
