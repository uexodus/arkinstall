package cmd.epoll.bindings

import cinterop.SafeCPointer
import kotlinx.cinterop.*
import platform.linux.*

@OptIn(ExperimentalForeignApi::class)
object EpollBindings {
    fun create(): Int = epoll_create1(0)

    fun register(epollFd: Int, fd: SafeCPointer<IntVar>, events: UInt): Int = memScoped {
        val epollEvent = alloc<epoll_event> {
            this.events = events
        }
        fd.immut { epoll_ctl(epollFd, EPOLL_CTL_ADD, it.pointed.value, epollEvent.ptr) }
    }

    fun wait(epollFd: Int, maxEvents: Int, timeout: Int): List<UInt> = memScoped {
        val events = allocArray<epoll_event>(maxEvents)

        val count = epoll_wait(epollFd, events, maxEvents, timeout)
        List(count) { events[it].events }
    }
}