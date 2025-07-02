package parted.types

import cinterop.SafeCPointer
import cinterop.SafeCPointerFactory
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKString
import log.Logger
import log.logFatal
import native.libparted.PedDiskType
import parted.bindings.PartedBindings
import parted.exception.PedDiskTypeException
import kotlin.reflect.KClass

@OptIn(ExperimentalForeignApi::class)
enum class PartedDiskType(val typeName: String) {
    UNKNOWN("unknown"),
    GPT("gpt"),
    MSDOS("msdos");

    /**
     * Returns the SafeCPointer for a known [PartedDiskType] value.
     */
    fun pointer(): SafeCPointer<PedDiskType> =
        PartedBindings.getDiskType(typeName).let { cPointer ->
            SafeCPointer.create(cPointer, pointedType)
        }

    override fun toString() = typeName

    companion object : SafeCPointerFactory<PedDiskType, NativePedDiskType, PartedDiskType> {
        override val pointedType: KClass<NativePedDiskType> = NativePedDiskType::class

        private val logger = Logger(PartedDiskType::class)
        private val nameMap = entries.associateBy { it.typeName }

        /**
         * Converts a native pointer into a [PartedDiskType] enum.
         */
        private fun fromPointer(diskType: SafeCPointer<PedDiskType>): PartedDiskType {
            val name = diskType.immut { it.pointed.name?.toKString() }
            val match = nameMap[name] ?: UNKNOWN
            if (match == UNKNOWN) {
                logger.d { "Unknown disk type '$name', defaulting to UNKNOWN" }
            }
            return match
        }

        override fun createBorrowed(cPointer: CPointer<PedDiskType>): PartedDiskType {
            return fromPointer(SafeCPointer.create(cPointer, pointedType))
        }

        override fun createOwned(cPointer: CPointer<PedDiskType>): PartedDiskType {
            logFatal(logger, PedDiskTypeException("Owned disk types do not exist."))
        }
    }
}