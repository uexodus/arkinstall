package parted.types

import cinterop.types.BitFlag
import kotlinx.cinterop.ExperimentalForeignApi
import native.libparted.*

@OptIn(ExperimentalForeignApi::class)
class PartedPartitionType(flags: UInt) : BitFlag(flags) {
    override val knownFlags: Map<BitFlag, String> by lazy { ALL }
    override fun create(flags: UInt): BitFlag = from(flags)

    companion object {
        val NORMAL = PartedPartitionType(PED_PARTITION_NORMAL)
        val LOGICAL = PartedPartitionType(PED_PARTITION_LOGICAL)
        val EXTENDED = PartedPartitionType(PED_PARTITION_EXTENDED)
        val FREESPACE = PartedPartitionType(PED_PARTITION_FREESPACE)
        val METADATA = PartedPartitionType(PED_PARTITION_METADATA)
        val PROTECTED = PartedPartitionType(PED_PARTITION_PROTECTED)

        private val instances = listOf(
            NORMAL to "NORMAL",
            LOGICAL to "LOGICAL",
            EXTENDED to "EXTENDED",
            FREESPACE to "FREESPACE",
            METADATA to "METADATA",
            PROTECTED to "PROTECTED"
        )

        private val ALL: Map<BitFlag, String> = instances.toMap()

        fun from(flags: UInt): PartedPartitionType =
            instances.firstOrNull { it.first.flags == flags }?.first ?: PartedPartitionType(flags)
    }

}