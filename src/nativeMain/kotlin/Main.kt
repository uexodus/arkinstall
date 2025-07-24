import cmd.SysCommand
import cmd.runCommand
import log.getOrExit
import log.logger
import parted.PartedDevice
import parted.PartedDisk
import parted.builder.PartedDiskBuilder
import parted.types.PartedDiskType
import parted.types.PartedFilesystemType
import parted.types.PartedPartitionFlag
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
                flags = setOf(PartedPartitionFlag.BOOT)
            }
            partition(remainingSpace) {
                type = PartedFilesystemType.EXT4
            }
        }.getOrExit(logger<PartedDiskBuilder>())

        disk.commit().onFailure {
            println("Failed to commit disk changes: ${it.message}")
            return
        }

        runCommand("mkfs.fat -F 32 ${devicePath}1").getOrExit(logger<SysCommand>())
        runCommand("mkfs.ext4 -F ${devicePath}2").getOrExit(logger<SysCommand>())

        println("Final Disk Layout:\n$disk")
    }

    println("Done.")
}

fun promptYesNo(question: String): Boolean {
    print("$question (y/n): ")
    return readln().trim().lowercase() == "y"
}