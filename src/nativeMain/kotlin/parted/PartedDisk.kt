package parted

import base.Summarisable
import cinterop.SafeCPointer
import cinterop.SafeCPointerFactory
import cinterop.SafeCPointerRegistry
import cinterop.util.asList
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.pointed
import native.libparted.PedDisk
import parted.bindings.PartedBindings
import parted.exception.PedDiskException
import parted.exception.PedPartitionException
import parted.types.NativePedDisk
import parted.types.PartedDiskType
import parted.types.PartedPartitionType
import parted.validation.DiskBounds
import unit.Size

@OptIn(ExperimentalForeignApi::class)
class PartedDisk private constructor(
    cPointer: CPointer<PedDisk>,
    destroyer: ((CPointer<PedDisk>) -> Unit)? = null
) : SafeCPointer<PedDisk>(cPointer, destroyer), Summarisable {

    val device: PartedDevice
        get() = immut { PartedDevice.createBorrowed(it.pointed.dev!!) }

    private val bounds = DiskBounds(device)

    /** All partitions linked to this disk. */
    val partitions: List<PartedPartition>
        get() = immut { ptr ->
            val rawList = ptr.pointed.part_list
                ?: return@immut emptyList()

            val partitions = rawList.asList(PartedPartition::createBorrowed) { it.pointed.next }

            partitions.forEach { addChild(it) }

            partitions.filter { it.type.has(PartedPartitionType.NORMAL) }
        }


    /** The disk label type (e.g. msdos, gpt) */
    val type: PartedDiskType
        get() = immut {
            it.pointed.type
                ?.let { type -> PartedDiskType.createBorrowed(type) }
                ?: PartedDiskType.UNKNOWN
        }

    /** The usable size of the disk, excluding reserved sectors */
    val size: Size = bounds.size

    /** Commit in-memory partition table to disk */
    fun commit(): Result<Unit> = runCatching {
        val success = PartedBindings.commitDisk(this)

        if (!success) {
            throw PedDiskException("Failed to commit changes to device ${device.path}")
        }
    }

    /**
     * Add a new partition to the disk.
     * You must discard the original partition (ownership is transferred).
     */
    fun add(
        partition: PartedPartition,
        constraint: PartedConstraint
    ): Result<PartedPartition> = runCatching {
        bounds.withinBounds(partition).getOrElse {
            throw PedDiskException("${partition.summary()} is not within writable bounds. ${it.message}")
        }

        if (bounds.overlapsPartitions(partitions, partition)) {
            throw PedDiskException("${partition.summary()} overlaps with an existing partition!")
        }

        val success = PartedBindings.addPartition(this, partition, constraint)

        if (!success) {
            throw PedPartitionException("Failed to add ${partition.summary()} to disk ${device.path}.")
        }

        // Ownership of the pointer is now managed by the disk.
        partition.demoteToBorrowed()
        partition
    }

    override fun toString(): String = buildString {
        appendLine("PartedDisk(")
        appendLine("    device=${device.path}")
        appendLine("    type=${type}")
        appendLine("    partitions=[")
        partitions.forEach { partition -> appendLine("        ${partition.summary()}") }
        appendLine("    ]")
        append(")")
    }

    override fun summary(): String = "PartedDisk(device=${device.path}, type=${type}, partitions=${partitions.size})"

    companion object : SafeCPointerFactory<PedDisk, NativePedDisk, PartedDisk> {
        override val pointedType = NativePedDisk::class

        /** Retrieve the current disk on the device */
        fun fromDevice(device: PartedDevice): Result<PartedDisk> = runCatching {
            val diskPointer = PartedBindings.getDisk(device)

            createOwned(diskPointer).also { device.addChild(it) }
        }

        /** Create a new partition table on the device */
        fun new(device: PartedDevice, type: PartedDiskType): Result<PartedDisk> = runCatching {
            val diskPointer = PartedBindings.createDisk(device, type)

            createOwned(diskPointer).also { device.addChild(it) }
        }

        override fun createBorrowed(cPointer: CPointer<PedDisk>): PartedDisk {
            return SafeCPointerRegistry.getOrCreate(cPointer, pointedType) {
                PartedDisk(cPointer)
            }
        }

        override fun createOwned(cPointer: CPointer<PedDisk>): PartedDisk {
            return SafeCPointerRegistry.getOrCreate(cPointer, pointedType) {
                PartedDisk(cPointer) { PartedBindings.destroyDisk(it) }
            }
        }
    }
}
