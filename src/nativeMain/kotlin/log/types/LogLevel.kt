package log.types

/** Log levels that match [syslog(3)](https://linux.die.net/man/3/syslog) severity levels */
enum class LogLevel {
    EMERGENCY, // system is unusable
    ALERT,  // action must be taken immediately
    CRITICAL, // critical conditions
    ERROR, // error conditions
    WARNING, // warning conditions
    NOTICE, // normal, but significant, condition
    INFO,  // informational message
    DEBUG, // debug-level message
}