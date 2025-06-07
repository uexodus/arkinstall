package parted

import base.Summarisable
import cinterop.SafeCPointer
import cinterop.SafeCPointerFactory
import cinterop.SafeCPointerRegistry
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import native.libparted.PedPartition
import parted.bindings.PartedBindings
import parted.exception.PartedPartitionException
import parted.types.NativePedPartition
import parted.types.PartedFilesystemType
import parted.types.PartedPartitionType

/** A wrapper for a [PedPartition](https://www.gnu.org/software/parted/api/struct__PedPartition.html) object */
@OptIn(ExperimentalForeignApi::class)
class PartedPartition private constructor(
    cPointer: CPointer<PedPartition>,
    destroyer: ((CPointer<PedPartition>) -> Unit)? = null
) : SafeCPointer<PedPartition>(cPointer, destroyer), Summarisable {

    val disk: PartedDisk
        get() = immut { PartedDisk.createBorrowed(it.pointed.disk!!) }

    /** Returns the next linked [PartedPartition] in the partition list for the owning [disk], if any */
    val next: PartedPartition?
        get() = immut { ptr ->
            ptr.pointed.next?.let { createBorrowed(it) }
        }

    /** The position of this partition in the [disk]'s partition table */
    val number: Int
        get() = immut { it.pointed.num }

    /** Returns the previous linked [PartedPartition] in the partition list for the owning [disk], if any */
    val previous: PartedPartition?
        get() = immut { ptr ->
            ptr.pointed.prev?.let {
                createBorrowed(it)
            }
        }

    /** The type of partition, a bitfield */
    val type: PartedPartitionType
        get() = immut { PartedPartitionType(it.pointed.type) }

    /** The geometry of the partition */
    val geometry: PartedGeometry by lazy {
        immut {
            val geometryPointer = PartedGeometry.createBorrowed(it.pointed.geom.ptr)
            addChild(geometryPointer)
            geometryPointer
        }
    }

    /** Returns true if the [other] partition overlaps with this partition */
    fun overlaps(other: PartedPartition): Boolean {
        return geometry.start <= other.geometry.end && geometry.end >= other.geometry.start
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

    companion object : SafeCPointerFactory<PedPartition, NativePedPartition, PartedPartition> {
        override val pointedType = NativePedPartition::class

        /** Creates a new partition on disk, but does not add the partition to disk's partition table */
        fun new(
            disk: PartedDisk,
            partitionType: PartedPartitionType,
            filesystemType: PartedFilesystemType,
            start: Long,
            end: Long
        ): Result<PartedPartition> = runCatching {
            val cPointer = disk.immut { diskPointer ->
                filesystemType.pointer().immut { fsType ->
                    PartedBindings.createPartition(
                        diskPointer,
                        partitionType.flags,
                        fsType,
                        start,
                        end
                    )
                }
            } ?: throw PartedPartitionException(
                "Partition creation failed, filesystem=$filesystemType start=$start end=$end"
            )

            PartedPartition(cPointer)
        }

        override fun createBorrowed(cPointer: CPointer<PedPartition>): PartedPartition {
            return SafeCPointerRegistry.getOrCreate(cPointer, pointedType) {
                PartedPartition(cPointer)
            }
        }

        override fun createOwned(cPointer: CPointer<PedPartition>): PartedPartition {
            return SafeCPointerRegistry.getOrCreate(cPointer, pointedType) {
                PartedPartition(cPointer) { partition -> PartedBindings.destroyPartition(partition) }
            }
        }
    }
}