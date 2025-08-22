package disk.filesystem

import cmd.runCommand
import kotlinx.io.files.Path
import parted.types.PartedFilesystemType.*

object Ext2Filesystem : MkfsFilesystem(EXT2, "ext2", "-F")

object Ext3Filesystem : MkfsFilesystem(EXT3, "ext3", "-F")

object Ext4Filesystem : MkfsFilesystem(EXT4, "ext4", "-F")

object Fat16Filesystem : MkfsFilesystem(FAT16, "fat", "-F", "16")

object Fat32Filesystem : MkfsFilesystem(FAT32, "fat", "-F", "32")

object BtrfsFilesystem : MkfsFilesystem(BTRFS, "btrfs", "-f")

object F2fsFilesystem : MkfsFilesystem(F2FS, "f2fs", "-f")

object NtfsFilesystem : MkfsFilesystem(NTFS, "ntfs", "-F")

object XfsFilesystem : MkfsFilesystem(XFS, "xfs", "-f")

object LinuxSwapFilesystem : Filesystem {
    override val partedFilesystemType = LINUX_SWAP

    override fun format(devicePath: Path) {
        runCommand("mkswap", "-f", devicePath.toString()).getOrThrow()
    }

    override fun mount(partitionPath: Path, mountPoint: Path?) {
        runCommand("swapon", partitionPath.toString()).getOrThrow()
    }
}