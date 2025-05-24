package parted.exception

class PartedDeviceException(message: String? = null) : Exception(message)
class PartedDiskException(message: String? = null) : Exception(message)
class PartedDiskTypeException(message: String? = null) : Exception(message)
class PartedPartitionException(message: String? = null) : Exception(message)
class PartedConstraintException(message: String? = null) : Exception(message)
class PartedFileSystemTypeException(message: String? = null) : Exception(message)