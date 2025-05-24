package parted

import parted.types.PartedDeviceType
import cinterop.OwnedSafeCObject
import cinterop.OwnedSafeCPointer
import unit.Size
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKString
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import log.Logger
import native.libparted.PedDevice
import parted.bindings.PartedBindings
import parted.exception.PartedDeviceException

/** A wrapper for a [PedDevice](https://www.gnu.org/software/parted/api/struct__PedDevice.html) object */
@OptIn(ExperimentalForeignApi::class)
class PartedDevice private constructor(
    override val pointer: OwnedSafeCPointer<PedDevice>
) : OwnedSafeCObject<PedDevice> {

    /** Total disk size in sectors, including reserved regions. */
    val length: Long
        get() = pointer.immut { it.pointed.length }

    /** Returns the next linked [PartedDevice] in the parted device list, if any */
    val next: PartedDevice?
        get() = pointer.immut {
            it.pointed.next?.let { nextPtr ->
                PartedDevice(PartedBindings.fromDevicePointer(nextPtr))
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
    val sectorSize: Size
        get() = pointer.immut { Size(it.pointed.sector_size) }

    /** Device type (SCSI, IDE, etc.) */
    val type: PartedDeviceType
        get() = pointer.immut { PartedDeviceType.fromOrdinal(it.pointed.type) }

    val size = Size.fromSectors(length, sectorSize)

    fun openDisk(): Result<PartedDisk> {
        return PartedDisk.fromDevice(this)
            .onSuccess { disk -> pointer.addChild(disk.pointer) }
            .onFailure { logger.e { it.message ?: "Unknown error" } }
    }

    override fun close() = pointer.close()

    override fun toString(): String = """
        PartedDevice(
            path = ${path ?: "Unknown"},
            model = ${model ?: "Unknown"},
            type = $type,
            readOnly = $readOnly,
            length = $length sectors,
            physicalSectorSize = $physicalSectorSize,
            sectorSize = $sectorSize,
            openCount = $openCount
        )
    """.trimIndent()

    override fun summary(): String {
        return "Device - $path, $model, $size"
    }

    companion object {
        private val logger = Logger(PartedDevice::class)

        fun open(path: String, refreshDevices: Boolean = true): Result<PartedDevice> {
            if (refreshDevices) PartedBindings.refreshDevices()

            if (!SystemFileSystem.exists(Path(path))) {
                return Result.failure(PartedDeviceException("Device not found at path $path"))
            }

            return PartedBindings.getDevice(path)
                ?.let { Result.success(PartedDevice(it)) }
                ?: Result.failure(PartedDeviceException("Failed to open device at path $path"))
        }
    }
}