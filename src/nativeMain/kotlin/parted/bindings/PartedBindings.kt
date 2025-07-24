package parted.bindings

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import native.libparted.*
import parted.PartedConstraint
import parted.PartedDevice
import parted.PartedDisk
import parted.PartedPartition
import parted.exception.*
import parted.types.PartedDiskType
import parted.types.PartedFilesystemType
import parted.types.PartedPartitionFlag
import parted.types.PartedPartitionType

@OptIn(ExperimentalForeignApi::class)
object PartedBindings {

    fun refreshDevices() = partedTry(::PedDeviceException) {
        ped_device_probe_all()
    }

    fun destroyDevice(dev: CPointer<PedDevice>) = partedTry(::PedDeviceException) {
        ped_device_destroy(dev)
    }

    fun destroyDisk(disk: CPointer<PedDisk>) = partedTry(::PedDiskException) {
        ped_disk_destroy(disk)
    }

    fun destroyPartition(partition: CPointer<PedPartition>) = partedTry(::PedPartitionException) {
        ped_partition_destroy(partition)
    }

    fun destroyConstraint(constraint: CPointer<PedConstraint>) = partedTry(::PedConstraintException) {
        ped_constraint_destroy(constraint)
    }

    fun destroyGeometry(geometry: CPointer<PedGeometry>) = partedTry(::PedGeometryException) {
        ped_geometry_destroy(geometry)
    }

    fun getDevice(name: String) = partedTryNotNull(
        ::PedDeviceException,
        "Failed to retrieve device: $name"
    ) {
        ped_device_get(name)
    }

    fun getDisk(device: PartedDevice) = partedTryNotNull(
        ::PedDiskException,
        "Failed to get disk from device: ${device.path}"
    ) {
        device.immut { ped_disk_new(it) }
    }

    fun createDisk(device: PartedDevice, diskType: PartedDiskType) = partedTryNotNull(
        ::PedDiskException,
        "Failed to create new disk with type ${diskType.name} on device ${device.path}"
    ) {
        device.immut { dev ->
            diskType.pointer().immut { type ->
                ped_disk_new_fresh(dev, type)
            }
        }
    }

    fun getDiskType(name: String) = partedTryNotNull(
        ::PedDiskTypeException,
        "Failed to find disk type: $name"
    ) {
        ped_disk_type_get(name)
    }

    fun commitDisk(disk: PartedDisk): Boolean = partedTry(
        ::PedDiskException
    ) {
        disk.immut { ped_disk_commit(it) == 1 }
    }

    fun getFileSystemType(name: String?) = partedTryNotNull(
        ::PedFileSystemTypeException,
        "Failed to find filesystem type: $name"
    ) {
        ped_file_system_type_get(name)
    }

    fun createPartition(
        disk: PartedDisk,
        type: PartedPartitionType,
        fsType: PartedFilesystemType,
        start: PedSector,
        end: PedSector
    ) = partedTryNotNull(
        ::PedPartitionException,
        "Failed to create partition of type $type with filesystem ${fsType.name}"
    ) {
        disk.mut { disk ->
            fsType.pointer().immut { fsType ->
                ped_partition_new(disk, type.flags, fsType, start, end)
            }
        }
    }

    fun addPartition(
        disk: PartedDisk,
        partition: PartedPartition,
        constraint: PartedConstraint
    ): Boolean = partedTry(::PedPartitionException) {
        disk.mut { disk ->
            partition.immut { part ->
                constraint.immut { constraint ->
                    ped_disk_add_partition(disk, part, constraint) == 1
                }
            }
        }
    }

    fun constraintFromDevice(device: PartedDevice) = partedTryNotNull(
        ::PedConstraintException,
        "Failed to get constraint from device: ${device.path}"
    ) {
        device.immut { ped_device_get_constraint(it) }
    }

    fun setExceptionHandler(handler: CPointer<PedExceptionHandler>) = ped_exception_set_handler(handler)

    fun setPartitionFlag(
        flag: PartedPartitionFlag,
        partition: PartedPartition,
        state: Boolean
    ) = partedTry(::PedPartitionException) {
        partition.immut { part ->
            ped_partition_set_flag(part, flag.toUInt(), if (state) 1 else 0)
        }
    }
}
