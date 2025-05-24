package parted.types

import cinterop.NativeType
import kotlinx.cinterop.ExperimentalForeignApi
import native.libparted.PedConstraint
import native.libparted.PedDevice
import native.libparted.PedDisk
import native.libparted.PedDiskType
import native.libparted.PedFileSystemType
import native.libparted.PedGeometry
import native.libparted.PedPartition

@OptIn(ExperimentalForeignApi::class)
object NativePedDisk : NativeType<PedDisk>

@OptIn(ExperimentalForeignApi::class)
object NativePedDevice : NativeType<PedDevice>

@OptIn(ExperimentalForeignApi::class)
object NativePedDiskType : NativeType<PedDiskType>

@OptIn(ExperimentalForeignApi::class)
object NativePedFileSystemType : NativeType<PedFileSystemType>

@OptIn(ExperimentalForeignApi::class)
object NativePedPartition : NativeType<PedPartition>

@OptIn(ExperimentalForeignApi::class)
object NativePedConstraint : NativeType<PedConstraint>

@OptIn(ExperimentalForeignApi::class)
object NativePedGeometry : NativeType<PedGeometry>
