package parted.types

import cinterop.NativeType
import kotlinx.cinterop.ExperimentalForeignApi
import native.libparted.*

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

@OptIn(ExperimentalForeignApi::class)
object NativePedException : NativeType<PedException>
