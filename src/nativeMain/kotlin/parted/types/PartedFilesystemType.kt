package parted.types

import cinterop.SafeCObject
import cinterop.SafeCPointer
import kotlinx.cinterop.ExperimentalForeignApi
import log.Logger
import log.logFatal
import native.libparted.PedFileSystemType
import parted.bindings.PartedBindings
import parted.exception.PartedFileSystemTypeException

@OptIn(ExperimentalForeignApi::class)
enum class PartedFilesystemType(val typeName: String) : SafeCObject<PedFileSystemType> {
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

    override val pointer: SafeCPointer<PedFileSystemType>
        get() = PartedBindings.getFileSystemTypeByName(typeName)
            ?: logFatal(logger, PartedFileSystemTypeException("Invalid filesystem type '$typeName'."))


    override fun close() = pointer.release()

    override fun toString() = typeName

    override fun summary() = typeName

    companion object {
        val logger = Logger(PartedFilesystemType::class)
    }
}