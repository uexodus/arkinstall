package parted.types

import cinterop.types.BitFlag
import kotlinx.cinterop.ExperimentalForeignApi
import native.libparted.PED_PARTITION_EXTENDED
import native.libparted.PED_PARTITION_FREESPACE
import native.libparted.PED_PARTITION_LOGICAL
import native.libparted.PED_PARTITION_METADATA
import native.libparted.PED_PARTITION_NORMAL
import native.libparted.PED_PARTITION_PROTECTED

@OptIn(ExperimentalForeignApi::class)
class PartedPartitionType(flags: UInt) : BitFlag(flags) {
    override val knownFlags: Map<BitFlag, String> = mapOf(
        NORMAL to "NORMAL",
        LOGICAL to "LOGICAL",
        EXTENDED to "EXTENDED",
        FREESPACE to "FREESPACE",
        METADATA to "METADATA",
        PROTECTED to "PROTECTED"
    )

    override fun create(flags: UInt): BitFlag = PartedPartitionType(flags)

    companion object {
        val NORMAL = PartedPartitionType(PED_PARTITION_NORMAL)
        val LOGICAL = PartedPartitionType(PED_PARTITION_LOGICAL)
        val EXTENDED = PartedPartitionType(PED_PARTITION_EXTENDED)
        val FREESPACE = PartedPartitionType(PED_PARTITION_FREESPACE)
        val METADATA = PartedPartitionType(PED_PARTITION_METADATA)
        val PROTECTED = PartedPartitionType(PED_PARTITION_PROTECTED)
    }
}