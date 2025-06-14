package parted.validation

import parted.PartedDevice
import parted.PartedPartition
import unit.MiB
import unit.Size

class DiskBounds(
    device: PartedDevice,
    reservedStart: Size = 1L.MiB,
    reservedEnd: Size = 1L.MiB
) {
    val start: Long = reservedStart.toSectors(device.logicalSectorSize)
    val size: Size = device.size - reservedStart - reservedEnd
    val end: Long = (device.size - reservedEnd).toSectors(device.logicalSectorSize)

    fun withinBounds(partition: PartedPartition): Result<Unit> = runCatching {
        val proposedStart = partition.geometry.start
        val proposedEnd = partition.geometry.end

        require(proposedStart >= start) {
            "Invalid partition start: proposed start ($proposedStart) is before allowed start ($start)"
        }

        require(proposedEnd <= end) {
            "Invalid partition end: proposed end ($proposedEnd) exceeds allowed end ($end)"
        }

        require(proposedStart < proposedEnd) {
            "Invalid partition range: proposed end ($proposedEnd) must be greater than proposed start ($proposedStart)"
        }
    }

    fun overlapsPartitions(partitions: List<PartedPartition>, partition: PartedPartition): Boolean {
        return partitions.any { it.overlaps(partition) }
    }
}