package disk.filesystem

import cmd.SysCommand
import disk.exceptions.PartitionMountException
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import parted.types.PartedFilesystemType

sealed class MkfsFilesystem(
    override val partedFilesystemType: PartedFilesystemType,
    private val filesystemType: String,
    private vararg val options: String
) : Filesystem {

    override fun format(devicePath: Path) {
        SysCommand(
            listOf("mkfs.$filesystemType", *options, devicePath.toString()),
            printOutput = true
        )
    }

    override fun mount(partitionPath: Path, mountPoint: Path?) {
        if (mountPoint == null) throw PartitionMountException(partitionPath, mountPoint)

        if (!SystemFileSystem.exists(mountPoint)) {
            SystemFileSystem.createDirectories(mountPoint)
        }
        SysCommand(listOf("mount", partitionPath.toString(), mountPoint.toString()))
    }
}