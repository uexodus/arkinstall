package log.backends
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.datetime.*
import kotlinx.io.Buffer
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString
import log.LogConfiguration
import log.types.LogLevel

class FileLogger(override val logLevel: LogLevel = LogLevel.DEBUG) : LogBackend {
    private val channel = Channel<String>(Channel.UNLIMITED)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job = scope.launch {
        for (line in channel) writeToFile(line)
    }

    override fun log(level: LogLevel, message: String) {
        if (level > logLevel) return

        val timestamp = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .format(LogConfiguration.timestampFormat)

        val line = "$timestamp - [$level] $message"

        runBlocking {
            channel.send(line)
        }
    }

    private fun writeToFile(line: String) {
        val fs = SystemFileSystem
        val path = LogConfiguration.logFilePath
        val dir = path.parent

        if (dir != null && !fs.exists(dir)) {
            fs.createDirectories(dir)
        }

        val buffer = Buffer().apply { writeString("$line\n") }
        fs.sink(path, append = true).use { sink ->
            sink.write(buffer, buffer.size)
        }
    }

    override fun close() {
        runBlocking {
            channel.close()
            job.join()
        }
    }
}
