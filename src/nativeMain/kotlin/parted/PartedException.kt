package parted

import cinterop.SafeCPointer
import cinterop.SafeCPointerFactory
import cinterop.SafeCPointerRegistry
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.pointed
import kotlinx.cinterop.toKString
import log.logFatal
import log.types.LogLevel
import native.libparted.PedException
import parted.types.NativePedException
import parted.types.PartedExceptionType
import parted.types.PartedFilesystemType.Companion.logger

@OptIn(ExperimentalForeignApi::class)
class PartedException(
    cPointer: CPointer<PedException>
) : SafeCPointer<PedException>(cPointer) {

    val message: String = immut {
        it.pointed.message?.toKString() ?: "Unknown exception (no message)"
    }

    val type: PartedExceptionType = immut {
        PartedExceptionType.fromOrdinal(it.pointed.type.toInt())
    }

    override fun toString() = message

    companion object : SafeCPointerFactory<PedException, NativePedException, PartedException> {
        override val pointedType = NativePedException::class

        fun PartedExceptionType.toLogLevel(): LogLevel = when (this) {
            PartedExceptionType.DEBUG -> LogLevel.DEBUG
            PartedExceptionType.INFORMATION -> LogLevel.INFO
            PartedExceptionType.WARNING -> LogLevel.WARNING
            PartedExceptionType.ERROR -> LogLevel.ERROR
            PartedExceptionType.FATAL -> LogLevel.ERROR
            PartedExceptionType.BUG -> LogLevel.ERROR
            PartedExceptionType.NO_FEATURE -> LogLevel.ERROR
        }

        override fun createBorrowed(cPointer: CPointer<PedException>): PartedException {
            return SafeCPointerRegistry.getOrCreate(cPointer, pointedType) {
                PartedException(cPointer)
            }
        }

        override fun createOwned(cPointer: CPointer<PedException>): PartedException {
            logFatal(logger, IllegalStateException("Cannot have an owned type of 'PartedException'"))
        }
    }
}