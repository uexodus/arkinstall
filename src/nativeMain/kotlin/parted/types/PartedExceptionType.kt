package parted.types

enum class PartedExceptionType {
    DEBUG,
    INFORMATION,
    WARNING,
    ERROR,
    FATAL,
    BUG,
    NO_FEATURE;

    companion object {
        fun fromOrdinal(ordinal: Int): PartedExceptionType {
            return entries[ordinal]
        }
    }
}