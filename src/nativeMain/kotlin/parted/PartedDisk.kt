package parted

import cinterop.OwnedSafeCObject
import cinterop.OwnedSafeCPointer
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
class PartedDisk private constructor(
    override val pointer: OwnedSafeCPointer<PedDisk>,
    val device: PartedDevice
) : OwnedSafeCObject<PedDisk> {

    private val _partitions: List<PartedPartition>
        get() = pointer.immut { ptr ->
            ptr.pointed.part_list
                ?.asList(PartedBindings::fromPartitionPointer) { it.pointed.next }
                ?.map { PartedPartition(it, this) }
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

    override fun close() = pointer.close()

    override fun toString(): String = "PartedDisk(type = $type, partitions = ${partitions.count()}"

    override fun summary(): String = "Disk - $type, partitions=${partitions.count()}"

    companion object {
        /** Gets a disk object from a device object */
        fun fromDevice(device: PartedDevice): Result<PartedDisk> =
            PartedBindings.getDisk(device.pointer)
                ?.let { Result.success(PartedDisk(it, device)) }
                ?: Result.failure(PartedDeviceException("No disk detected on device: ${device.summary()}"))
    }
}