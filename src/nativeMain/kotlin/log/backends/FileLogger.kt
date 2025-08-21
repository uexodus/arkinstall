package log.backends

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import kotlinx.io.Buffer
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString
import log.LogConfiguration
import log.types.LogLevel
import kotlin.reflect.KClass

class FileLogger(override var logLevel: LogLevel) : LogBackend {
    private val channel = Channel<String>(Channel.UNLIMITED)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job = scope.launch {
        for (line in channel) appendToFile(line)
    }

    override fun log(level: LogLevel, module: KClass<*>, message: String) {
        if (level > logLevel) return

        val line = "${timestamp()} - [${module.simpleName}] $message"

        runBlocking { channel.send(line) }
    }

    override fun error(module: KClass<*>, exception: KClass<*>, message: String) {
        if (LogLevel.ERROR > logLevel) return

        val line = "${timestamp()} - [${module.simpleName}] [${exception.simpleName}] $message"

        runBlocking { channel.send(line) }
    }

    private fun timestamp(): String {
        return Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .format(LogConfiguration.timestampFormat)
    }

    private fun appendToFile(line: String) {
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
