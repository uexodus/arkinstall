package parted

import cinterop.OwnedSafeCObject
import cinterop.OwnedSafeCPointer
import cinterop.SafeCObject
import cinterop.SafeCPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKString
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import native.libparted.PedDevice
import parted.bindings.PartedBindings
import parted.exception.PartedDeviceException
import parted.types.PartedDeviceType
import parted.types.PartedDiskType
import unit.Size

/** A wrapper for a [PedDevice](https://www.gnu.org/software/parted/api/struct__PedDevice.html) object */
@OptIn(ExperimentalForeignApi::class)
sealed class PartedDevice : SafeCObject<PedDevice> {

    /** Total disk size in sectors, including reserved regions. */
    val length: Long
        get() = pointer.immut { it.pointed.length }

    /** Total disk size in bytes, including reserved regions. */
    val size: Size
        get() = Size.fromSectors(length, logicalSectorSize)

    /** Returns the next linked [PartedDevice] in the parted device list, if any */
    val next: PartedDevice?
        get() = pointer.immut {
            it.pointed.next?.let { nextPtr ->
                Borrowed(PartedBindings.fromDevicePointer(nextPtr))
            }
        }

    /** Number of times this device has been opened with `ped_device_open()` */
    val openCount: Int
        get() = pointer.immut { it.pointed.open_count }

    /** Description of hardware (manufacturer, model) for the block device. */
    val model: String?
        get() = pointer.immut { it.pointed.model?.toKString() }

    /** /dev entry for the block device. */
    val path: String?
        get() = pointer.immut { it.pointed.path?.toKString() }

    /** Physical sector size, in bytes */
    val physicalSectorSize: Size
        get() = pointer.immut { Size(it.pointed.phys_sector_size) }

    /** Whether the device is marked as read-only */
    val readOnly: Boolean
        get() = pointer.immut { it.pointed.read_only == 1 }

    /** Logical sector size, in bytes */
    val logicalSectorSize: Size
        get() = pointer.immut { Size(it.pointed.sector_size) }

    /** Device type (SCSI, IDE, etc.) */
    val type: PartedDeviceType
        get() = pointer.immut { PartedDeviceType.fromOrdinal(it.pointed.type) }

    fun openDisk(): Result<PartedDisk> {
        return PartedDisk.fromDevice(this)
    }

    fun createDisk(type: PartedDiskType): Result<PartedDisk> {
        return PartedDisk.new(this, type)
    }

    override fun close() = pointer.close()

    override fun toString(): String = """
        PartedDevice(
            path=$path,
            model=$model,
            type=$type,
            readOnly=$readOnly,
            length=$length sectors,
            size=$size,
            physicalSectorSize=$physicalSectorSize,
            logicalSectorSize=$logicalSectorSize,
            openCount=$openCount
        )
    """.trimIndent()

    override fun summary(): String {
        return "PartedDevice(path=${path ?: "Unknown"}, model=${model ?: "Unknown"}, size=$size)"
    }

    class Owned(
        override val pointer: OwnedSafeCPointer<PedDevice>
    ) : PartedDevice(), OwnedSafeCObject<PedDevice> {
        override fun close() = pointer.free()
    }

    class Borrowed(
        override val pointer: SafeCPointer<PedDevice>
    ) : PartedDevice() {
        override fun close() = pointer.release()
    }

    companion object {
        /** Attempts to open the device at the given path */
        fun open(path: String, refreshDevices: Boolean = true): Result<Owned> = runCatching {
            if (refreshDevices) PartedBindings.refreshDevices()

            val devicePath = Path(path)

            if (!SystemFileSystem.exists(devicePath)) {
                throw PartedDeviceException("Device not found at path $path")
            }

            val devicePointer = PartedBindings.getDevice(path)
                ?: throw PartedDeviceException("Failed to open device at path $path")

            Owned(devicePointer)
        }
    }
}