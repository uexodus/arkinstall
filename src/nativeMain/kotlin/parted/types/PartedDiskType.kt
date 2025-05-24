package parted.types

import cinterop.SafeCObject
import cinterop.SafeCPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKString
import log.Logger
import log.logFatal
import native.libparted.PedDiskType
import parted.bindings.PartedBindings
import parted.exception.PartedDiskTypeException

@OptIn(ExperimentalForeignApi::class)
enum class PartedDiskType(val typeName: String) : SafeCObject<PedDiskType> {
    UNKNOWN("unknown"),
    GPT("gpt"),
    MSDOS("msdos");

    override val pointer: SafeCPointer<PedDiskType>
        get() = PartedBindings.getDiskTypeByName(typeName)
            ?: logFatal(logger, PartedDiskTypeException("Invalid disk type '$typeName'."))

    override fun close() = pointer.close()
    override fun summary(): String = typeName
    override fun toString(): String = typeName

    companion object {
        private val logger = Logger(PartedDiskType::class)
        private val nameMap = entries.associateBy { it.typeName }

        fun fromPointer(diskType: SafeCPointer<PedDiskType>?): PartedDiskType {
            val name = diskType?.immut { it.pointed.name?.toKString() }
            val match = nameMap[name] ?: UNKNOWN
            if (match == UNKNOWN) {
                logger.d { "Unknown disk type '$name', defaulting to UNKNOWN" }
            }
            return match
        }
    }
}