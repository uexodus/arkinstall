package parted

import cinterop.OwnedSafeCObject
import cinterop.OwnedSafeCPointer
import cinterop.SafeCObject
import cinterop.SafeCPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import native.libparted.PedPartition
import parted.bindings.PartedBindings
import parted.exception.PartedPartitionException
import parted.types.PartedFilesystemType
import parted.types.PartedPartitionType

/** A wrapper for a [PedPartition](https://www.gnu.org/software/parted/api/struct__PedPartition.html) object */
@OptIn(ExperimentalForeignApi::class)
sealed class PartedPartition(
    val disk: PartedDisk
) : SafeCObject<PedPartition> {

    /** Returns the next linked [PartedPartition] in the partition list for the owning [disk], if any */
    val next: PartedPartition?
        get() = pointer.immut { ptr ->
            ptr.pointed.next?.let {
                Borrowed(PartedBindings.fromPartitionPointer(it), disk)
            }
        }

    /** The position of this partition in the [disk]'s partition table */
    val number: Int
        get() = pointer.immut { it.pointed.num }

    /** Returns the previous linked [PartedPartition] in the partition list for the owning [disk], if any */
    val previous: PartedPartition?
        get() = pointer.immut { ptr ->
            ptr.pointed.prev?.let {
                Borrowed(PartedBindings.fromPartitionPointer(it), disk)
            }
        }

    /** The type of partition, a bitfield */
    val type: PartedPartitionType
        get() = pointer.immut { PartedPartitionType(it.pointed.type) }

    /** The geometry of the partition */
    val geometry: PartedGeometry
        get() = pointer.immut {
            val geometryPointer = PartedBindings.fromGeometryPointer(it.pointed.geom.ptr)
            pointer.addChild(geometryPointer)
            PartedGeometry.Borrowed(geometryPointer, disk.device)
        }

    override fun toString() = """
        PartedPartition(
            number=$number,
            type=$type,
            geometry=${geometry.summary()},
            previous=${previous?.summary()},
            next=${next?.summary()},
        )
    """.trimIndent()

    override fun summary(): String = "PartedPartition(no=$number, size=${geometry.size}, type=$type)"

    class Owned(
        override val pointer: OwnedSafeCPointer<PedPartition>,
        disk: PartedDisk
    ) : PartedPartition(disk), OwnedSafeCObject<PedPartition> {
        override fun close() = pointer.free()
    }

    class Borrowed(
        override val pointer: SafeCPointer<PedPartition>,
        disk: PartedDisk
    ) : PartedPartition(disk) {
        override fun close() = pointer.release()
    }

    companion object {
        /** Creates a new partition on disk, but does not add the partition to disk's partition table */
        fun new(
            disk: PartedDisk,
            partitionType: PartedPartitionType,
            filesystemType: PartedFilesystemType,
            start: Long,
            end: Long
        ): Result<Owned> = runCatching {
            val ptr = PartedBindings.createPartition(
                disk.pointer,
                partitionType,
                filesystemType.pointer,
                start,
                end
            ) ?: throw PartedPartitionException(
                "Partition creation failed, filesystem=$filesystemType start=$start end=$end"
            )
            Owned(ptr, disk)
        }
    }
}