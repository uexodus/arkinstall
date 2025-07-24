package unit

import kotlin.math.absoluteValue

/**
 * Represents a size in bytes.
 */
value class Size(val bytes: Long) : Comparable<Size> {
    companion object {
        /** Creates a [Size] from an [amount] and a [unit]. */
        fun of(amount: Long, unit: SizeUnit): Size = Size(amount * unit.bytes)

        /** Creates a [Size] from a number of [sectors] and a [sectorSize]. */
        fun fromSectors(sectors: Long, sectorSize: Size): Size = Size(sectors * sectorSize.bytes)
    }

    /** Returns the number of full sectors in this [Size] object. */
    fun toSectors(sectorSize: Size): Long =
        bytes / sectorSize.bytes

    /** Aligns this [Size] upward to the nearest multiple of [sectorSize]. */
    fun alignUpTo(sectorSize: Size): Size =
        Size(((bytes + sectorSize.bytes - 1) / sectorSize.bytes) * sectorSize.bytes)

    /** Aligns this [Size] downward to the nearest multiple of [sectorSize]. */
    fun alignDownTo(sectorSize: Size): Size =
        Size((bytes / sectorSize.bytes) * sectorSize.bytes)

    /** Adds two [Size] objects together */
    operator fun plus(other: Size): Size = Size(bytes + other.bytes)

    /** Subtracts two [Size] objects from another */
    operator fun minus(other: Size): Size = Size(bytes - other.bytes)

    /** Multiplies a [Size] by a scalar */
    operator fun times(factor: Long): Size = Size(bytes * factor)

    /** Divides a [Size] by a scalar */
    operator fun div(divisor: Long): Size = Size(bytes / divisor)

    override operator fun compareTo(other: Size): Int = bytes.compareTo(other.bytes)

    /** Returns a human-readable string representation of the size in the largest fitting unit. */
    override fun toString(): String {
        val absBytes = bytes.absoluteValue.toDouble()
        val unit = SizeUnit.entries.reversed().first { absBytes >= it.bytes }
        val value = bytes.toDouble() / unit.bytes

        val formatted = if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            ((value * 100).toLong() / 100.0).toString()
        }
        Enum
        return "$formatted ${unit.name}"
    }
}

/** Enum representations of each byte unit and their corresponding number of bytes */
enum class SizeUnit(val bytes: Long) {
    B(1), KiB(1L shl 10), MiB(1L shl 20), GiB(1L shl 30), TiB(1L shl 40)
}


/** Extension property to create a [Size] in bytes. */
val Int.B get() = Size.of(this.toLong(), SizeUnit.B)

/** Extension property to create a [Size] in kibibytes.*/
val Int.KiB get() = Size.of(this.toLong(), SizeUnit.KiB)

/** Extension property to create a [Size] in mebibyte.*/
val Int.MiB get() = Size.of(this.toLong(), SizeUnit.MiB)

/** Extension property to create a [Size] in gibibytes.*/
val Int.GiB get() = Size.of(this.toLong(), SizeUnit.GiB)

/** Extension property to create a [Size] in tebibytes.*/
val Int.TiB get() = Size.of(this.toLong(), SizeUnit.TiB)


