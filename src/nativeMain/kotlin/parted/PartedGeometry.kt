package parted

import base.Summarisable
import cinterop.SafeCPointer
import cinterop.SafeCPointerFactory
import cinterop.SafeCPointerRegistry
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.pointed
import native.libparted.PedGeometry
import parted.bindings.PartedBindings
import parted.types.NativePedGeometry
import unit.Size

/** A basic wrapper for a [PedGeometry](https://www.gnu.org/software/parted/api/group__PedGeometry.html) object */
@OptIn(ExperimentalForeignApi::class)
class PartedGeometry private constructor(
    cPointer: CPointer<PedGeometry>,
    destroyer: ((CPointer<PedGeometry>) -> Unit)? = null
) : SafeCPointer<PedGeometry>(cPointer, destroyer), Summarisable {

    val device: PartedDevice
        get() = immut { PartedDevice.createBorrowed(it.pointed.dev!!) }

    /** The sector the geometry starts at */
    val start: Long
        get() = immut { it.pointed.start }

    /** The sector the geometry ends at */
    val end: Long
        get() = immut { it.pointed.end }

    /** The length of the geometry in sectors */
    val length: Long
        get() = immut { it.pointed.length }

    /** The size of the geometry in bytes */
    val size: Size
        get() = Size.fromSectors(length, device.logicalSectorSize)

    override fun summary(): String = "PartedGeometry(start=$start, end=$end, size=$size)"

    override fun toString(): String = """
        PartedGeometry(
            start=$start,
            end=$end,
            length=$length,
            size=$size
            device=${device.path}
        )
    """.trimIndent()

    companion object : SafeCPointerFactory<PedGeometry, NativePedGeometry, PartedGeometry> {
        override val pointedType = NativePedGeometry::class

        override fun createOwned(cPointer: CPointer<PedGeometry>): PartedGeometry {
            return SafeCPointerRegistry.getOrCreate(cPointer, pointedType) {
                PartedGeometry(it) { geom -> PartedBindings.destroyGeometry(geom) }
            }
        }

        override fun createBorrowed(cPointer: CPointer<PedGeometry>): PartedGeometry {
            return SafeCPointerRegistry.getOrCreate(cPointer, pointedType) {
                PartedGeometry(it)
            }
        }
    }
}