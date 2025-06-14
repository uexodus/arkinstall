package parted.builder

import parted.PartedConstraint
import parted.PartedDevice
import parted.PartedDisk
import parted.PartedPartition
import parted.types.PartedDiskType
import parted.types.PartedFilesystemType
import parted.types.PartedPartitionType
import parted.validation.DiskBounds
import unit.B
import unit.Size

data class PartitionConfig(
    val size: Size,
    val filesystemType: PartedFilesystemType
)

@DslMarker
annotation class DiskDsl

@DiskDsl
data class PartitionBuilder(
    val size: Size,
    var type: PartedFilesystemType = PartedFilesystemType.EXT4
) {
    fun build(): PartitionConfig = PartitionConfig(
        size, type
    )
}

class PartedDiskBuilder(
    val device: PartedDevice,
    var diskType: PartedDiskType = PartedDiskType.GPT
) {
    private val partitions = mutableListOf<PartitionConfig>()
    private val bounds = DiskBounds(device)

    var usedSpace = 0L.B

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

            val draft = PartedPartition.new(
                disk, PartedPartitionType.NORMAL,
                config.filesystemType, start, end
            ).getOrThrow()

            val constraint = PartedConstraint.fromDevice(disk.device).getOrThrow()

            disk.add(draft, constraint).getOrThrow()

            start = end + 1
        }

        disk
    }
}