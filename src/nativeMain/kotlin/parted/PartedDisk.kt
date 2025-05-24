package parted

import cinterop.OwnedSafeCObject
import cinterop.OwnedSafeCPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.pointed
import native.libparted.PedDisk
import parted.bindings.PartedBindings
import parted.exception.PartedDeviceException
import parted.types.PartedDiskType

@OptIn(ExperimentalForeignApi::class)
class PartedDisk private constructor(
    override val pointer: OwnedSafeCPointer<PedDisk>,
    val device: PartedDevice
) : OwnedSafeCObject<PedDisk> {

    val type: PartedDiskType
        get() = pointer.immut { ptr ->
            PartedDiskType.fromPointer(
                ptr.pointed.type?.let { PartedBindings.fromDiskTypePointer(it) }
            )
        }

    override fun summary(): String {
        TODO("Not yet implemented")
    }

    override fun close() = pointer.close()

    companion object {
        fun fromDevice(device: PartedDevice): Result<PartedDisk> =
            PartedBindings.getDisk(device.pointer)
                ?.let { Result.success(PartedDisk(it, device)) }
                ?: Result.failure(PartedDeviceException("No disk detected on device: ${device.summary()}"))
    }
}