import log.getOrExit
import parted.PartedDevice
import parted.types.PartedDiskType
import parted.types.PartedFilesystemType
import unit.GiB

fun main() {
    PartedDevice.open("/dev/sda").getOrExit().use { device ->
        println("Device Information:\n$device\n")
        val disk = device.openDisk().getOrElse { error ->
            println(error.message)
            println("Do you wish to create a GPT partition table? (y/n)")

            if (readln().trim().lowercase() != "y") {
                println("Operation cancelled by user.")
            }

            // Create a 1 GiB boot partition and use the remaining space for the root partition
            device.createDisk(PartedDiskType.GPT) {
                partition(1L.GiB) {
                    type = PartedFilesystemType.FAT32
                }

                partition(remainingSpace) {
                    type = PartedFilesystemType.EXT4
                }
            }.getOrExit()
        }
        disk.commit()
        println("Disk Information:\n$disk\n")
    }

    println("done!")
}