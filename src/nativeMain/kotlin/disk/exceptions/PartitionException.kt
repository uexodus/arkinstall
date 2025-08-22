package disk.exceptions

import kotlinx.io.files.Path

class PartitionMountException(partitionPath: Path, mountPoint: Path?) : Exception(
    "Failed to mount partition $partitionPath on path $mountPoint"
)