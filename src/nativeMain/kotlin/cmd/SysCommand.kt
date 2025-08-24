package cmd

import cinterop.util.read
import cinterop.util.toNullTerminatedCString
import cmd.bindings.PtyBindings.forkPty
import cmd.epoll.Epoll
import cmd.epoll.types.EpollEvent.Companion.ERR
import cmd.epoll.types.EpollEvent.Companion.HUP
import cmd.epoll.types.EpollEvent.Companion.IN
import cmd.exception.SysCommandException
import cmd.types.ExitStatus
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import platform.posix._exit
import platform.posix.close
import platform.posix.execvp
import unit.KiB
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalForeignApi::class)
class SysCommand(
    private val command: List<String>,
    private val printOutput: Boolean = false,
) {
    val cmd = command.joinToString(" ")

    private var output = StringBuilder()
    private val epoll = Epoll()

    init {
        execute()
    }

    /** Executes the given [command] in pty */
    private fun execute() {
        require(command.isNotEmpty() && command.first().isNotBlank()) { "Cannot execute empty command." }

        val (pid, fd) = forkPty()

        if (pid == 0) {
            memScoped {
                command.toNullTerminatedCString(this).use { commandPtr ->
                    commandPtr.immut { execvp(command.first(), it) }
                }
            }
            _exit(1)
        }

        epoll.register(fd, IN or HUP)

        try {
            poll(pid, fd)
        } finally {
            epoll.close()
            close(fd)
        }
    }

    private fun poll(pid: Int, fd: Int) {
        while (true) {
            val events = epoll.poll(0.1.seconds)

            for (event in events) {
                if (event.has(ERR)) {
                    drain(fd)
                    throw SysCommandException(cmd, ExitStatus(pid), output.toString())
                }
                if (event.has(HUP)) {
                    drain(fd)
                    val status = ExitStatus(pid)
                    println(status.exitStatus)
                    if (!status.isSuccessful) {
                        throw SysCommandException(cmd, status, output.toString())
                    }
                    return
                }
                if (event.has(IN)) {
                    val bytes = fd.read(8.KiB)
                    appendOutput(bytes)
                }
            }
        }
    }

    private fun appendOutput(bytes: ByteArray) {
        val str = bytes.decodeToString()
        if (printOutput) print(str)
        output.append(str)
    }

    private fun drain(fd: Int) {
        while (true) {
            try {
                val bytes = fd.read(8.KiB)
                if (bytes.isEmpty()) break
                appendOutput(bytes)
            } catch (_: RuntimeException) {
                break
            }
        }
    }

}