package cmd.epoll.bindings

import kotlinx.cinterop.*
import platform.linux.*

@OptIn(ExperimentalForeignApi::class)
object EpollBindings {
    fun create(): Int = epoll_create1(0)

    fun register(epollFd: Int, fd: Int, events: UInt): Int = memScoped {
        val epollEvent = alloc<epoll_event> {
            this.events = events
        }
        epoll_ctl(epollFd, EPOLL_CTL_ADD, fd, epollEvent.ptr)
    }

    fun wait(epollFd: Int, maxEvents: Int, timeout: Int): List<UInt> = memScoped {
        val events = allocArray<epoll_event>(maxEvents)

        val count = epoll_wait(epollFd, events, maxEvents, timeout)
        List(count) { events[it].events }
    }
}