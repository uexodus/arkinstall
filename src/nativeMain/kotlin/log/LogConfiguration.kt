package log


import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.char
import kotlinx.io.files.Path
import log.backends.FileLogger
import log.backends.SysLogger

object LogConfiguration {
    /** Name of the program that appears in `journalctl` and dictates the log path. */
    const val PROGRAM_NAME = "arkinstall"

    /** Default log file location */
    val logFilePath: Path = Path("/var/log/$PROGRAM_NAME/all.log")

    /** Timestamp format used in all file log entries (e.g. default: 2025-05-01 18:32:01.464035457) */
    var timestampFormat = LocalDateTime.Format {
        date(LocalDate.Formats.ISO) // eg 2025-05-01
        char(' ')
        hour()
        char(':')
        minute()
        char(':')
        second()
        char('.')
        secondFraction(9)
    }

    val backends = setOf(FileLogger(), SysLogger())
}