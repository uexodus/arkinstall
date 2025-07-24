package parted.builder

import parted.PartedConstraint
import parted.PartedDevice
import parted.PartedDisk
import parted.PartedPartition
import parted.types.PartedDiskType
import parted.types.PartedFilesystemType
import parted.types.PartedPartitionFlag
import parted.types.PartedPartitionType
import parted.validation.DiskBounds
import unit.B
import unit.Size

data class PartitionConfig(
    val size: Size,
    val filesystemType: PartedFilesystemType,
    val flags: Set<PartedPartitionFlag>
)

@DslMarker
annotation class DiskDsl

@DiskDsl
data class PartitionBuilder(
    val size: Size,
    var type: PartedFilesystemType = PartedFilesystemType.EXT4,
    var flags: Set<PartedPartitionFlag> = setOf()
) {
    fun build(): PartitionConfig = PartitionConfig(
        size, type, flags
    )
}

class PartedDiskBuilder(
    private val device: PartedDevice,
    private var diskType: PartedDiskType = PartedDiskType.GPT
) {
    private val partitions = mutableListOf<PartitionConfig>()
    private val bounds = DiskBounds(device)

    var usedSpace = 0.B

    val remainingSpace: Size
        get() = bounds.size - usedSpace

    fun partition(size: Size, block: PartitionBuilder.() -> Unit) {
        val partition = PartitionBuilder(size).apply(block).build()
        usedSpace += size
        partitions.add(partition)
    }

    fun build(): Result<PartedDisk> = runCatching {
        val disk = PartedDisk.new(device, diskType).getOrThrow()

        val sectorSize = device.logicalSectorSize
        var start = bounds.start

        for (config in partitions) {
            val sizeInSectors = config.size.alignUpTo(sectorSize).toSectors(sectorSize)
            val end = start + sizeInSectors - 1

            val partition = PartedPartition.new(
                disk, PartedPartitionType.NORMAL,
                config.filesystemType, start, end
            ).getOrThrow()

            val constraint = PartedConstraint.fromDevice(disk.device).getOrThrow()

            disk.add(partition, constraint).getOrThrow()

            for (flag in config.flags) {
                partition.enableFlag(flag).getOrThrow()
            }

            start = end + 1
        }

        disk
    }
}