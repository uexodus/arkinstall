package disk.filesystem

import kotlinx.io.files.Path
import parted.types.PartedFilesystemType

sealed interface Filesystem {
    val partedFilesystemType: PartedFilesystemType
    fun format(devicePath: Path)
    fun mount(partitionPath: Path, mountPoint: Path?)
}