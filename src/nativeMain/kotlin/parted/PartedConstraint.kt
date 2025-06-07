package parted

import base.Summarisable
import cinterop.SafeCPointer
import cinterop.SafeCPointerFactory
import cinterop.SafeCPointerRegistry
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import native.libparted.PedConstraint
import parted.bindings.PartedBindings
import parted.exception.PartedConstraintException
import parted.types.NativePedConstraint

@OptIn(ExperimentalForeignApi::class)
class PartedConstraint private constructor(
    cPointer: CPointer<PedConstraint>,
    destroyer: ((CPointer<PedConstraint>) -> Unit)? = null
) : SafeCPointer<PedConstraint>(cPointer, destroyer), Summarisable {

    override fun toString(): String = "PartedConstraint()"

    override fun summary(): String = "PartedConstraint()"

    companion object : SafeCPointerFactory<PedConstraint, NativePedConstraint, PartedConstraint> {
        override val pointedType = NativePedConstraint::class

        fun fromDevice(device: PartedDevice) = runCatching {
            val constraintPointer = device.immut { dev -> PartedBindings.constraintFromDevice(dev) }
                ?: throw PartedConstraintException("Failed to get constraint from device: $device")

            createOwned(constraintPointer)
        }

        override fun createBorrowed(cPointer: CPointer<PedConstraint>): PartedConstraint {
            return SafeCPointerRegistry.getOrCreate(cPointer, pointedType) {
                PartedConstraint(cPointer)
            }
        }

        override fun createOwned(cPointer: CPointer<PedConstraint>): PartedConstraint {
            return SafeCPointerRegistry.getOrCreate(cPointer, pointedType) {
                PartedConstraint(cPointer) { constraint -> PartedBindings.destroyConstraint(constraint) }
            }
        }
    }
}