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

    override fun toString(): String = buildString {
        appendLine("PartedDisk(")
        appendLine("    device=${device.path}")
        appendLine("    type=${type}")
        appendLine("    partitions=[")
        partitions.forEach { partition ->
            appendLine("        ${partition.summary()}")
        }
        appendLine("    ]")
        appendLine(")")
    }

    override fun summary(): String = "PartedDisk(device=${device.path}, type=${type}, partitions=${partitions.count()})"

    companion object {
        /** Gets a disk object from a device object */
        fun fromDevice(device: PartedDevice): Result<Owned> =
            PartedBindings.getDisk(device.pointer)
                ?.let { Result.success(Owned(it, device)) }
                ?: Result.failure(
                    PartedDeviceException("No disk detected on device: ${device.summary()}")
                )
    }

    class Owned(
        override val pointer: OwnedSafeCPointer<PedDisk>,
        device: PartedDevice
    ) : PartedDisk(device), OwnedSafeCObject<PedDisk> {
        override fun close() = pointer.free()
    }
}