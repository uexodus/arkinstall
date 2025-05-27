package parted

import cinterop.OwnedSafeCObject
import cinterop.OwnedSafeCPointer
import cinterop.SafeCObject
import cinterop.SafeCPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.pointed
import native.libparted.PedGeometry
import unit.Size

/** A basic wrapper for a [PedGeometry](https://www.gnu.org/software/parted/api/group__PedGeometry.html) object */
@OptIn(ExperimentalForeignApi::class)
sealed class PartedGeometry(val device: PartedDevice) : SafeCObject<PedGeometry> {

    /** The sector the geometry starts at */
    val start: Long
        get() = pointer.immut { it.pointed.start }

    /** The sector the geometry ends at */
    val end: Long
        get() = pointer.immut { it.pointed.end }

    /** The length of the geometry in sectors */
    val length: Long
        get() = pointer.immut { it.pointed.length }

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

    class Owned(
        override val pointer: OwnedSafeCPointer<PedGeometry>,
        device: PartedDevice
    ) : PartedGeometry(device), OwnedSafeCObject<PedGeometry> {
        override fun close() = pointer.free()
    }

    class Borrowed(
        override val pointer: SafeCPointer<PedGeometry>,
        device: PartedDevice
    ) : PartedGeometry(device) {
        override fun close() = pointer.release()
    }
}