package parted

import cinterop.OwnedSafeCObject
import cinterop.OwnedSafeCPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.pointed
import native.libparted.PedPartition
import parted.bindings.PartedBindings
import parted.types.PartedPartitionType

/** A wrapper for a [PedPartition](https://www.gnu.org/software/parted/api/struct__PedPartition.html) object */
@OptIn(ExperimentalForeignApi::class)
class PartedPartition(
    override val pointer: OwnedSafeCPointer<PedPartition>,
    val disk: PartedDisk
) : OwnedSafeCObject<PedPartition> {

    /** Returns the next linked [PartedPartition] in the partition list for the owning [disk], if any */
    val next: PartedPartition?
        get() = pointer.immut { ptr ->
            ptr.pointed.next?.let {
                PartedPartition(
                    PartedBindings.fromPartitionPointer(it),
                    disk
                )
            }
        }

    /** The position of this partition in the [disk]'s partition table */
    val number: Int
        get() = pointer.immut { it.pointed.num }

    /** Returns the previous linked [PartedPartition] in the partition list for the owning [disk], if any */
    val previous: PartedPartition?
        get() = pointer.immut { ptr ->
            ptr.pointed.prev?.let {
                PartedPartition(
                    PartedBindings.fromPartitionPointer(it),
                    disk
                )
            }
        }

    /** The type of partition, a bitfield */
    val type: PartedPartitionType
        get() = pointer.immut {
            PartedPartitionType(it.pointed.type)
        }

    override fun close() = pointer.close()

    override fun toString() = "PartedPartition(no=$number, type=$type)"

    override fun summary(): String = "Partition - no=$number"
}