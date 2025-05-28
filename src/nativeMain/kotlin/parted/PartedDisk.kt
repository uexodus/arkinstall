package parted

import cinterop.OwnedSafeCObject
import cinterop.OwnedSafeCPointer
import cinterop.SafeCObject
import cinterop.util.asList
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.pointed
import native.libparted.PedDisk
import parted.bindings.PartedBindings
import parted.exception.PartedDeviceException
import parted.exception.PartedDiskException
import parted.types.PartedDiskType
import parted.types.PartedPartitionType

/** A wrapper for a [PedDisk](https://www.gnu.org/software/parted/api/struct__PedDisk.html) object **/
@OptIn(ExperimentalForeignApi::class)
sealed class PartedDisk(val device: PartedDevice) : SafeCObject<PedDisk> {

    private val _partitions: List<PartedPartition>
        get() = pointer.immut { ptr ->
            ptr.pointed.part_list
                ?.asList(PartedBindings::fromPartitionPointer) { it.pointed.next }
                ?.map {
                    pointer.addChild(it)
                    PartedPartition.Borrowed(it, this)
                }
                ?: listOf()
        }

    /** A linked list of partitions in this disk */
    val partitions: List<PartedPartition>
        get() = _partitions.filter { it.type.has(PartedPartitionType.NORMAL) }

    /** Returns the disk label type */
    val type: PartedDiskType
        get() = pointer.immut { ptr ->
            PartedDiskType.fromPointer(
                ptr.pointed.type?.let { PartedBindings.fromDiskTypePointer(it) }
            )
        }

    /** Commits the in-memory partition table changes to disk and informs the kernel. */
    fun commit(): Result<Unit> {
        val success = PartedBindings.commitToDisk(pointer)

        return if (success) {
            Result.success(Unit)
        } else {
            Result.failure(PartedDiskException("Failed to commit changes to device ${device.path}"))
        }
    }

    override fun toString(): String = buildString {
        appendLine("PartedDisk(")
        appendLine("    device=${device.path}")
        appendLine("    type=${type}")
        appendLine("    partitions=[")
        partitions.forEach { partition ->
            appendLine("        ${partition.summary()}")
        }
        appendLine("    ]")
        append(")")
    }

    override fun summary(): String = "PartedDisk(device=${device.path}, type=${type}, partitions=${partitions.count()})"

    companion object {
        /** Retrieves the disk associated with the given device, if a partition table exists. */
        fun fromDevice(device: PartedDevice): Result<Owned> =
            PartedBindings.getDisk(device.pointer)
                ?.let {
                    device.pointer.addChild(it)
                    Result.success(Owned(it, device))
                }
                ?: Result.failure(
                    PartedDeviceException("No disk detected on device: ${device.summary()}")
                )

        /** Creates a partition table of the given [type] on the [device] */
        fun newDisk(device: PartedDevice, type: PartedDiskType) =
            PartedBindings.createDisk(device.pointer, type.pointer)
                ?.let {
                    device.pointer.addChild(it)
                    Result.success(Owned(it, device))
                }
                ?: Result.failure(
                    PartedDeviceException("Failed to create partition table on device: ${device.summary()}")
                )
    }

    class Owned(
        override val pointer: OwnedSafeCPointer<PedDisk>,
        device: PartedDevice
    ) : PartedDisk(device), OwnedSafeCObject<PedDisk> {
        override fun close() = pointer.free()
    }
}