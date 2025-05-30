package parted.bindings

import cinterop.OwnedSafeCPointer
import cinterop.SafeCPointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.pointed
import log.Logger
import native.libparted.*
import parted.types.*

/** Bindings for [libparted](https://www.gnu.org/software/parted/api/) */
@OptIn(ExperimentalForeignApi::class)
object PartedBindings {
    val logger = Logger(PartedBindings::class)

    fun refreshDevices() {
        ped_device_probe_all()
    }

    fun fromOwnedDevicePointer(deviceCPointer: CPointer<PedDevice>): OwnedSafeCPointer<PedDevice> {
        return OwnedSafeCPointer.create(deviceCPointer, NativePedDevice::class) {
            ped_device_destroy(it)
        }
    }

    fun fromDevicePointer(deviceCPointer: CPointer<PedDevice>): SafeCPointer<PedDevice> {
        return SafeCPointer.create(deviceCPointer, NativePedDevice::class)
    }

    fun getDevice(devicePath: String): OwnedSafeCPointer<PedDevice>? {
        val ptr = ped_device_get(devicePath) ?: return null
        return fromOwnedDevicePointer(ptr)
    }

    fun fromOwnedDiskPointer(diskCPointer: CPointer<PedDisk>): OwnedSafeCPointer<PedDisk> {
        return OwnedSafeCPointer.create(diskCPointer, NativePedDisk::class) {
            ped_disk_destroy(it)
        }
    }

    fun getDisk(device: SafeCPointer<PedDevice>): OwnedSafeCPointer<PedDisk>? {
        val diskCPointer = device.immut { ped_disk_new(it) } ?: return null
        return fromOwnedDiskPointer(diskCPointer)
    }

    fun createDisk(
        device: SafeCPointer<PedDevice>,
        diskType: SafeCPointer<PedDiskType>
    ): OwnedSafeCPointer<PedDisk>? {
        val diskCPointer = device.immut { devicePtr ->
            diskType.immut { typePtr ->
                ped_disk_new_fresh(devicePtr, typePtr)
            }
        } ?: return null
        return fromOwnedDiskPointer(diskCPointer)
    }

    fun fromDiskTypePointer(diskTypeCPointer: CPointer<PedDiskType>): SafeCPointer<PedDiskType> {
        return SafeCPointer.create(diskTypeCPointer, NativePedDiskType::class)
    }

    fun getDiskTypeByName(name: String): SafeCPointer<PedDiskType>? {
        val ptr = ped_disk_type_get(name) ?: return null
        return fromDiskTypePointer(ptr)
    }

    fun commitToDisk(disk: SafeCPointer<PedDisk>): Boolean {
        return disk.mut { ped_disk_commit(it) } == 1
    }

    fun getFileSystemTypeByName(name: String?): SafeCPointer<PedFileSystemType>? {
        val ptr = ped_file_system_type_get(name) ?: return null
        return SafeCPointer.create(ptr, NativePedFileSystemType::class)
    }

    fun fromOwnedPartitionPointer(
        partitionCPointer: CPointer<PedPartition>
    ): OwnedSafeCPointer<PedPartition> {
        return OwnedSafeCPointer.create(
            partitionCPointer, NativePedPartition::class
        ) {
            // Only free the partition object if it's not part of a disk
            if (it.pointed.disk == null) {
                ped_partition_destroy(it)
            } else {
                logger.i { "Didn't actually free pointer ${it.rawValue} as it belongs to a disk" }
            }
        }
    }

    fun fromPartitionPointer(
        partitionCPointer: CPointer<PedPartition>
    ): SafeCPointer<PedPartition> {
        return SafeCPointer.create(partitionCPointer, NativePedPartition::class)
    }

    fun createPartition(
        disk: SafeCPointer<PedDisk>,
        partitionType: PartedPartitionType,
        filesystemType: SafeCPointer<PedFileSystemType>,
        start: PedSector,
        end: PedSector
    ): OwnedSafeCPointer<PedPartition>? {
        val partitionCPointer = disk.immut { diskPointer ->
            filesystemType.immut { filesystemTypePointer ->
                ped_partition_new(
                    diskPointer,
                    partitionType.flags,
                    filesystemTypePointer,
                    start,
                    end
                )
            }
        } ?: return null
        return fromOwnedPartitionPointer(partitionCPointer)
    }

    fun getDiskType(device: SafeCPointer<PedDevice>): SafeCPointer<PedDiskType>? {
        val ptr = device.immut { ped_disk_probe(it) } ?: return null
        return SafeCPointer.create(ptr, NativePedDiskType::class)
    }

    fun addPartition(
        disk: SafeCPointer<PedDisk>,
        partition: OwnedSafeCPointer<PedPartition>,
        constraint: SafeCPointer<PedConstraint>
    ): Boolean {
        return disk.mut { diskPtr ->
            partition.mut { partPtr ->
                constraint.immut { constraintPtr ->
                    ped_disk_add_partition(diskPtr, partPtr, constraintPtr) == 1
                }
            }
        }
    }

    fun fromOwnedConstraintPointer(constraintPointer: CPointer<PedConstraint>): OwnedSafeCPointer<PedConstraint> {
        return OwnedSafeCPointer.create(constraintPointer, NativePedConstraint::class) {
            ped_constraint_destroy(it)
        }
    }

    fun createDeviceConstraint(device: SafeCPointer<PedDevice>): OwnedSafeCPointer<PedConstraint>? {
        val ptr = device.immut { ped_device_get_constraint(it) } ?: return null
        return fromOwnedConstraintPointer(ptr)
    }

    fun fromGeometryPointer(geometryCPointer: CPointer<PedGeometry>): SafeCPointer<PedGeometry> {
        return SafeCPointer.create(geometryCPointer, NativePedGeometry::class)
    }
}
