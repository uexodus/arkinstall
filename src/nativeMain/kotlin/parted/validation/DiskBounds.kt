package parted.validation

import parted.PartedDevice
import parted.PartedPartition
import unit.MiB
import unit.Size

class DiskBounds(
    val device: PartedDevice,
    reservedStart: Size = 1L.MiB,
    reservedEnd: Size = 1L.MiB
) {
    val start: Long = reservedStart.toSectors(device.logicalSectorSize)
    val size: Size = device.size - reservedStart - reservedEnd
    val end: Long = (device.size - reservedEnd).toSectors(device.logicalSectorSize)

    fun withinBounds(partition: PartedPartition): Result<Unit> = runCatching {
        val proposedStart = partition.geometry.start
        val proposedEnd = partition.geometry.end

        require(proposedStart >= start) { "Proposed start $proposedStart < allowed start $start" }
        require(proposedEnd <= end) { "Proposed end $proposedEnd > allowed end $end" }
        require(proposedStart < proposedEnd) { "Proposed end $proposedEnd <= start $proposedStart" }
    }

    fun overlapsPartitions(partitions: List<PartedPartition>, partition: PartedPartition): Boolean {
        return partitions.any { it.overlaps(partition) }
    }
}