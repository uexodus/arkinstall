import log.getOrExit
import parted.PartedDevice

fun main() {
    PartedDevice.open("/dev/sda").getOrExit().use { device ->
        println("Device Information:")
        println(device)
        println()

        device.openDisk().getOrExit().use { disk ->
            println("Disk information:")
            println("Type: ${disk.type}")
            for (partition in disk.partitions) {
                println(partition)
            }
        }
    }

    println("done!")
}