import log.getOrExit
import parted.PartedDevice

fun main() {
    PartedDevice.open("/dev/sda").getOrExit().use {
        println("Device Information:")
        println(it)
        println()

        it.openDisk().getOrExit().use { disk ->
            println("Disk information:")
            println("Type: ${disk.type}")
        }
    }

    println("done!")
}