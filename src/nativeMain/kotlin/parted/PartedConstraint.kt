package parted

import cinterop.OwnedSafeCObject
import cinterop.OwnedSafeCPointer
import cinterop.SafeCObject
import kotlinx.cinterop.ExperimentalForeignApi
import native.libparted.PedConstraint
import parted.bindings.PartedBindings
import parted.exception.PartedConstraintException

@OptIn(ExperimentalForeignApi::class)
sealed class PartedConstraint : SafeCObject<PedConstraint> {

    override fun toString(): String = "PartedConstraint()"

    override fun summary(): String = "PartedConstraint()"

    class Owned(
        override val pointer: OwnedSafeCPointer<PedConstraint>
    ) : PartedConstraint(), OwnedSafeCObject<PedConstraint> {
        override fun close() = pointer.free()
    }

    companion object {
        fun fromDevice(device: PartedDevice) = runCatching {
            val constraintPointer = PartedBindings.createDeviceConstraint(device.pointer)
                ?: throw PartedConstraintException("Failed to get constraint from device: $device")

            Owned(constraintPointer)
        }
    }
}