import cmd.SysCommand
import cmd.runCommand
import kotlinx.io.files.Path
import log.getOrExit
import log.logger
import parted.PartedDevice
import parted.PartedDisk
import parted.PartedPartition
import parted.builder.PartedDiskBuilder
import parted.types.PartedDiskType.GPT
import parted.types.PartedFilesystemType.EXT4
import parted.types.PartedFilesystemType.FAT32
import parted.types.PartedPartitionFlag.BOOT
import unit.GiB


fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: program <device path>")
        println("Example: arkinstall /dev/sda")
        return
    }

    val devicePath = Path(args.first())
    val device = PartedDevice.open(devicePath).getOrExit(logger<PartedDevice>())

    device.use { dev ->
        if (dev.openDisk().isSuccess) {
            logger<PartedDisk>().w { "Partition table already exists on $devicePath!" }
        }

        if (!promptYesNo("Create a GPT table on $devicePath?")) return

        val disk = dev.new(GPT) {
            partition(1.GiB) { type = FAT32; flags = setOf(BOOT) }
            partition(remainingSpace) { type = EXT4 }
        }.getOrExit(logger<PartedDiskBuilder>())

        disk.commit().onFailure {
            println("Failed to commit disk changes: ${it.message}")
            return
        }

        val (boot, root) = disk.partitions.take(2)
            .map { p -> p.path().getOrExit(logger<PartedPartition>()) }


        runCommand("mkfs.fat -F 32 $boot").getOrExit(logger<SysCommand>())
        runCommand("mkfs.ext4 -F $root").getOrExit(logger<SysCommand>())

        println("Final Disk Layout:\n$disk")
    }

    println("Done.")
}

fun promptYesNo(question: String): Boolean {
    print("$question (y/n): ")
    return readln().trim().lowercase() == "y"
}