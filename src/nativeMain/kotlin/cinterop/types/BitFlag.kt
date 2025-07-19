package cinterop.types

abstract class BitFlag(val flags: UInt) {
    open val knownFlags: Map<BitFlag, String> by lazy { emptyMap() }

    fun has(flag: BitFlag): Boolean = flags == flag.flags || flags and flag.flags != 0u

    protected abstract fun create(flags: UInt): BitFlag

    infix fun or(other: BitFlag): BitFlag {
        return create(this.flags or other.flags)
    }

    override fun toString(): String {
        val activeFlags = knownFlags.entries
            .filter { (flag, _) -> has(flag) }
            .joinToString(" | ") { (_, name) -> name }

        return activeFlags.ifEmpty { "UNKNOWN($flags)" }
    }
}
