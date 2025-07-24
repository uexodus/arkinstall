package parted.types

enum class PartedPartitionFlag {
    NULL, BOOT, ROOT, SWAP, HIDDEN, RAID, LVM, LBA, HP_SERVICE, PALO, PREP, MSFTRES,
    BIOS_GRUB, ATVRECV, DIAG, LEGACY_BOOT, MSFTDATA, IRST, ESP, CHROMEOS_KERNEL,
    BLS_BOOT, LINUX_HOME, NO_AUTOMOUNT;

    fun toUInt() = ordinal.toUInt()
}