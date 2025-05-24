package parted.types

enum class PartedDeviceType {
    UNKNOWN, SCSI, IDE,
    DAC960, CPQARRAY, FILE,
    ATARAID, I2O, UBD,
    DASD, VIODASD, SX8,
    DM, XVD, SDMMC,
    VIRTBLK, AOE, MD,
    LOOP, NVME, RAM,
    PMEM;

    companion object {
        fun fromOrdinal(code: UInt): PartedDeviceType =
            entries.getOrElse(code.toInt()) { UNKNOWN }
    }
}