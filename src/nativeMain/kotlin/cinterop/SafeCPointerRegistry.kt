package cinterop

import cinterop.exceptions.PointedTypeRedefinitionException
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import log.Logger
import log.logFatal
import kotlin.reflect.KClass

/**
 * A global registry that tracks all active [SafeCPointer] instances.
 *
 * - A single address can be wrapped by only one [SafeCPointer] instance at a time.
 * - Each pointer must be associated with a single pointed type, which cannot change until the pointer is freed or released.
 */
@OptIn(ExperimentalForeignApi::class)
object SafeCPointerRegistry {
    private val logger = Logger(SafeCPointerRegistry::class)
    private val registry = mutableMapOf<Long, Entry<out CPointed>>()

    private class Entry<T : CPointed>(
        val pointedType: KClass<*>,
        val pointer: SafeCPointer<T>,
    )

    /**
     * Retrieves an existing [SafeCPointer] instance for the given [cPointer] from the registry,
     * or creates and registers a new one using the provided [create] lambda.
     *
     * @throws PointedTypeRedefinitionException if a pointed type was previously registered as a different type.
     */
    fun <T : CPointed, R : SafeCPointer<T>, N : NativeType<T>> getOrCreate(
        cPointer: CPointer<T>,
        pointedType: KClass<N>,
        create: () -> R
    ): R {
        val address = cPointer.rawValue.toLong()

        val entry = registry.getOrPut(address) {
            val pointer = create()
            logger.d { "Registered ${pointer.toDebugString()} type: (${pointedType.qualifiedName})" }

            Entry(pointedType, pointer)
        }

        if (entry.pointedType != pointedType) {
            logFatal(logger, PointedTypeRedefinitionException(entry.pointedType, pointedType))
        }

        @Suppress("UNCHECKED_CAST")
        return entry.pointer as R
    }

    /**
     * Removes the registered [SafeCPointer] for the given [cPointer] address.
     */
    fun forget(cPointer: CPointer<*>) {
        registry.remove(cPointer.rawValue.toLong())
            ?.let {
                logger.d { "Forgotten ${it.pointer.toDebugString()} type: (${it.pointedType.qualifiedName})" }
            }
            ?: logger.w { "Attempted to remove unknown pointer $cPointer" }
    }
}
