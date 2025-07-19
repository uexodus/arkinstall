package parted

import base.Summarisable
import cinterop.SafeCPointer
import cinterop.SafeCPointerFactory
import cinterop.SafeCPointerRegistry
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKString
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import native.libparted.PedDevice
import parted.bindings.PartedBindings
import parted.builder.PartedDiskBuilder
import parted.exception.PedDeviceException
import parted.types.NativePedDevice
import parted.types.PartedDeviceType
import parted.types.PartedDiskType
import unit.Size

/** A wrapper for a [PedDevice](https://www.gnu.org/software/parted/api/struct__PedDevice.html) object */
@OptIn(ExperimentalForeignApi::class)
class PartedDevice private constructor(
    cPointer: CPointer<PedDevice>,
    destroyer: ((CPointer<PedDevice>) -> Unit)? = null
) : SafeCPointer<PedDevice>(cPointer, destroyer), Summarisable {

    /** Total disk size in sectors, including reserved regions. */
    val length: Long
        get() = immut { it.pointed.length }

    /** Total disk size in bytes, including reserved regions. */
    val size: Size
        get() = Size.fromSectors(length, logicalSectorSize)

    /** Returns the next linked [PartedDevice] in the parted device list, if any */
    val next: PartedDevice?
        get() = immut {
            it.pointed.next?.let { cPointer -> createBorrowed(cPointer) }
        }

    /** Number of times this device has been opened with `ped_device_open()` */
    val openCount: Int
        get() = immut { it.pointed.open_count }

    /** Description of hardware (manufacturer, model) for the block device. */
    val model: String
        get() = immut { it.pointed.model?.toKString() ?: "" }

    /** /dev entry for the block device. */
    val path: String
        get() = immut { it.pointed.path?.toKString() ?: "" }

    /** Physical sector size, in bytes */
    val physicalSectorSize: Size
        get() = immut { Size(it.pointed.phys_sector_size) }

    /** Whether the device is marked as read-only */
    val readOnly: Boolean
        get() = immut { it.pointed.read_only == 1 }

    /** Logical sector size, in bytes */
    val logicalSectorSize: Size
        get() = immut { Size(it.pointed.sector_size) }

    /** Device type (SCSI, IDE, etc.) */
    val type: PartedDeviceType
        get() = immut { PartedDeviceType.fromOrdinal(it.pointed.type) }

    fun openDisk(): Result<PartedDisk> {
        return PartedDisk.fromDevice(this)
    }

    fun createDisk(type: PartedDiskType, block: PartedDiskBuilder.() -> Unit): Result<PartedDisk> {
        return PartedDiskBuilder(this, type)
            .apply(block).build()
    }

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
        return "PartedDevice(path=${path}, model=${model}, size=$size)"
    }

    companion object : SafeCPointerFactory<PedDevice, NativePedDevice, PartedDevice> {
        override val pointedType = NativePedDevice::class

        /** Attempts to open the device at the given [path] */
        fun open(path: String, refreshDevices: Boolean = true): Result<PartedDevice> = runCatching {
            if (refreshDevices) PartedBindings.refreshDevices()

            val devicePath = Path(path)
            if (!SystemFileSystem.exists(devicePath)) {
                throw PedDeviceException("Device not found at path $path")
            }

            println("Getting device from path $devicePath")

            val cPointer = PartedBindings.getDevice(path)

            createOwned(cPointer)
        }

        override fun createBorrowed(cPointer: CPointer<PedDevice>): PartedDevice {
            return SafeCPointerRegistry.getOrCreate(cPointer, pointedType) {
                PartedDevice(it)
            }
        }

        override fun createOwned(cPointer: CPointer<PedDevice>): PartedDevice {
            return SafeCPointerRegistry.getOrCreate(cPointer, pointedType) {
                PartedDevice(it) { dev -> PartedBindings.destroyDevice(dev) }
            }
        }
    }
}