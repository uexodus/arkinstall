package log.bindings

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import native.syslog.closelog
import native.syslog.openlog
import native.syslog.syslog

@OptIn(ExperimentalForeignApi::class)
/** Bindings for [syslog(3)](https://linux.die.net/man/3/syslog) */
object SyslogBindings {
    fun openLog(identity: CPointer<ByteVar>) {
        openlog(identity, 0, 0)
    }

    fun systemLog(priority: Int, message: String) {
        syslog(priority, "%s", message)
    }

    fun closeLog() {
        closelog()
    }
}