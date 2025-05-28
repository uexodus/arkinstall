import log.getOrExit
import parted.PartedDevice
import parted.types.PartedDiskType
import kotlin.system.exitProcess

fun main() {
    PartedDevice.open("/dev/sda").getOrExit()
        .use { device ->
            println("Device Information:\n$device\n")

            val disk = device.openDisk().getOrElse { result ->
                println(result.message)
                println("Do you wish to create a GPT partition table (y/n)?")
                if (readln().trim().lowercase() == "y") {
                    device.createDisk(PartedDiskType.GPT)
                        .onSuccess { it.commit() }
                        .getOrExit()
                } else {
                    println("Operation cancelled by user.")
                    exitProcess(0)
                }
            }

            println("Disk Information:\n$disk\n")
        }

    println("done!")
}