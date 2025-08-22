package cmd

import cinterop.util.read
import cinterop.util.toNullTerminatedCString
import cmd.bindings.PtyBindings.forkPty
import cmd.epoll.Epoll
import cmd.epoll.types.EpollEvent.Companion.HUP
import cmd.epoll.types.EpollEvent.Companion.IN
import cmd.exception.SysCommandException
import cmd.types.ExitStatus
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import platform.posix.execvp
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalForeignApi::class)
class SysCommand(
    private val printOutput: Boolean = false,
    vararg command: String
) {
    val command = command.toList()
    val cmd = command.joinToString(" ")

    var output: String = ""
        private set

    private val epoll = Epoll()

    /** Executes the given [command] in pty */
    fun execute(): Result<Unit> {
        val (pid, fd) = forkPty()

        if (pid == 0) {
            memScoped {
                command.toNullTerminatedCString(this).use { commandPtr ->
                    commandPtr.immut { execvp(command.first(), it) }
                }
            }
            exitProcess(1)
        }

        epoll.register(fd, IN or HUP)

        while (true) {
            val events = epoll.poll(0.1.seconds)

            for (event in events) {
                if (event.has(IN)) {
                    val buffer = fd.read(8192)
                    val newOutput = buffer.decodeToString()

                    if (printOutput) print(newOutput)
                    output += newOutput

                }
                if (event.has(HUP)) {
                    epoll.close()

                    val status = ExitStatus(pid)

                    if (status.isSuccessful) {
                        return Result.success(Unit)
                    }

                    return Result.failure(SysCommandException(cmd, status))
                }
            }
        }
    }
}

fun runCommand(vararg command: String) = SysCommand(printOutput = true, *command).execute()