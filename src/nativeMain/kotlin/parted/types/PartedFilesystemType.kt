package parted.types

import cinterop.SafeCPointer
import cinterop.SafeCPointerFactory
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKString
import log.Logger
import log.logFatal
import native.libparted.PedFileSystemType
import parted.bindings.PartedBindings
import kotlin.reflect.KClass

@OptIn(ExperimentalForeignApi::class)
enum class PartedFilesystemType(private val typeName: String) {
    UNKNOWN("unknown"),
    BTRFS("btrfs"),
    EXT2("ext2"),
    EXT3("ext3"),
    EXT4("ext4"),
    F2FS("f2fs"),
    FAT16("fat16"),
    FAT32("fat32"),
    LINUX_SWAP("linux-swap(v1)"),
    NTFS("ntfs"),
    XFS("xfs");

    /**
     * Returns the SafeCPointer for a known [PartedFilesystemType] value.
     */
    fun pointer(): SafeCPointer<PedFileSystemType> =
        PartedBindings.getFileSystemType(typeName).let { cPointer ->
            SafeCPointer.create(cPointer, pointedType)
        }

    override fun toString() = typeName

    companion object : SafeCPointerFactory<PedFileSystemType, NativePedFileSystemType, PartedFilesystemType> {
        override val pointedType: KClass<NativePedFileSystemType> = NativePedFileSystemType::class

        val logger = Logger(PartedFilesystemType::class)
        private val nameMap = entries.associateBy { it.typeName }

        /**
         * Converts a native pointer into a [PartedFilesystemType] enum.
         */
        private fun fromPointer(fsType: SafeCPointer<PedFileSystemType>): PartedFilesystemType {
            val name = fsType.immut { it.pointed.name?.toKString() }
            val match = nameMap[name] ?: UNKNOWN
            if (match == UNKNOWN) {
                logger.i { "Unknown filesystem type '$name', defaulting to UNKNOWN" }
            }
            return match
        }

        override fun createBorrowed(cPointer: CPointer<PedFileSystemType>): PartedFilesystemType {
            return fromPointer(SafeCPointer.create(cPointer, pointedType))
        }

        override fun createOwned(cPointer: CPointer<PedFileSystemType>): PartedFilesystemType {
            logFatal(logger, IllegalStateException("Owned filesystem types do not exist."))
        }
    }
}
