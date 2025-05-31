import log.getOrExit
import parted.PartedConstraint
import parted.PartedDevice
import parted.PartedPartition
import parted.types.PartedDiskType
import parted.types.PartedFilesystemType
import parted.types.PartedPartitionType
import kotlin.system.exitProcess

fun main() {
    PartedDevice.open("/dev/sda").getOrExit()
        .use { device ->
            println("Device Information:\n$device\n")

            val disk = device.openDisk().getOrElse { result ->
                println(result.message)
                println("Do you wish to create a GPT partition table (y/n)?")

                if (readln().trim().lowercase() != "y") {
                    println("Operation cancelled by user.")
                    exitProcess(0)
                }

                device.createDisk(PartedDiskType.GPT)
                    .getOrExit()
            }

            val partition = PartedPartition.new(
                disk,
                PartedPartitionType.NORMAL,
                PartedFilesystemType.EXT4,
                10, 20
            ).getOrExit()

            val constraint = PartedConstraint.fromDevice(device).getOrExit()

            disk.add(partition, constraint).getOrExit()
            disk.commit().getOrExit()

            println("Disk Information:\n$disk\n")
        }

    println("done!")
}