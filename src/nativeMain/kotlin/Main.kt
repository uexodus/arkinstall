import log.getOrExit
import log.logger
import parted.PartedDevice
import parted.PartedDisk
import parted.builder.PartedDiskBuilder
import parted.types.PartedDiskType
import parted.types.PartedFilesystemType
import unit.GiB


fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: program <device path>")
        println("Example: arkinstall /dev/sda")
        return
    }

    val devicePath = args.first()

    PartedDevice.open(devicePath).getOrExit(logger<PartedDevice>()).use { device ->
        println("Device Information:\n$device\n")

        device.openDisk().onSuccess {
            logger<PartedDisk>().w { "Partition table already exists on device $devicePath!" }
        }

        if (!promptYesNo("Create a GPT partition table on $devicePath?")) {
            println("Operation cancelled by user.")
            return
        }

        val disk = device.createDisk(PartedDiskType.GPT) {
            partition(1L.GiB) {
                type = PartedFilesystemType.FAT32
            }
            partition(remainingSpace) {
                type = PartedFilesystemType.EXT4
            }
        }.getOrExit(logger<PartedDiskBuilder>())

        disk.commit().onFailure {
            println("Failed to commit disk changes: ${it.message}")
            return
        }

        println("Final Disk Layout:\n$disk")
    }

    println("Done.")
}

fun promptYesNo(question: String): Boolean {
    print("$question (y/n): ")
    return readln().trim().lowercase() == "y"
}