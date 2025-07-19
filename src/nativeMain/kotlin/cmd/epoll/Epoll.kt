package cmd.epoll

import cinterop.SafeCPointer
import cinterop.types.BitFlag
import cmd.epoll.bindings.EpollBindings
import cmd.epoll.types.EpollEvent
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlin.time.Duration

@OptIn(ExperimentalForeignApi::class)
class Epoll : AutoCloseable {
    val epollFd = EpollBindings.create()

    /** Registers a file descriptor with epoll to monitor the given [events] */
    fun register(fd: SafeCPointer<IntVar>, events: BitFlag) {
        EpollBindings.register(epollFd, fd, events.flags)
    }

    /** Waits for events on the registered file descriptors within the given [timeout]. **/
    fun poll(timeout: Duration): List<EpollEvent> {
        return EpollBindings.wait(epollFd, 10, timeout.inWholeMilliseconds.toInt())
            .map { EpollEvent(it) }
    }

    override fun close() {
        platform.posix.close(epollFd)
    }
}