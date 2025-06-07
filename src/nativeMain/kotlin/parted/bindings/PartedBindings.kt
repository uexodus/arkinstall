package parted.bindings

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import native.libparted.*

/** Bindings for [libparted](https://www.gnu.org/software/parted/api/) */
@OptIn(ExperimentalForeignApi::class)
object PartedBindings {
    fun refreshDevices() = ped_device_probe_all()

    fun destroyDevice(dev: CPointer<PedDevice>) = ped_device_destroy(dev)

    fun destroyDisk(disk: CPointer<PedDisk>) = ped_disk_destroy(disk)

    fun destroyPartition(partition: CPointer<PedPartition>) = ped_partition_destroy(partition)

    fun destroyConstraint(constraint: CPointer<PedConstraint>) = ped_constraint_destroy(constraint)

    fun destroyGeometry(geom: CPointer<PedGeometry>) = ped_geometry_destroy(geom)

    fun getDevice(name: String): CPointer<PedDevice>? = ped_device_get(name)

    fun getDisk(dev: CPointer<PedDevice>): CPointer<PedDisk>? = ped_disk_new(dev)

    fun createDisk(dev: CPointer<PedDevice>, diskType: CPointer<PedDiskType>): CPointer<PedDisk>? =
        ped_disk_new_fresh(dev, diskType)

    fun getDiskType(name: String): CPointer<PedDiskType>? = ped_disk_type_get(name)

    fun commitDisk(disk: CPointer<PedDisk>): Boolean {
        return ped_disk_commit(disk) == 1
    }

    fun getFileSystemType(name: String?): CPointer<PedFileSystemType>? = ped_file_system_type_get(name)

    fun createPartition(
        disk: CPointer<PedDisk>,
        type: UInt,
        fsType: CPointer<PedFileSystemType>,
        start: PedSector,
        end: PedSector
    ): CPointer<PedPartition>? = ped_partition_new(disk, type, fsType, start, end)

    fun addPartition(
        disk: CPointer<PedDisk>,
        part: CPointer<PedPartition>,
        constraint: CPointer<PedConstraint>
    ): Boolean {
        return ped_disk_add_partition(disk, part, constraint) == 1
    }

    fun constraintFromDevice(dev: CPointer<PedDevice>): CPointer<PedConstraint>? =
        ped_device_get_constraint(dev)
}
