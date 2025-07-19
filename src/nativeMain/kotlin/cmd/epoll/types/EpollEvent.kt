package cmd.epoll.types

import cinterop.types.BitFlag
import platform.linux.EPOLLERR
import platform.linux.EPOLLHUP
import platform.linux.EPOLLIN

class EpollEvent(flags: UInt) : BitFlag(flags) {
    override val knownFlags: Map<BitFlag, String> by lazy {
        mapOf(
            IN to "IN",
            ERR to "ERR",
            HUP to "HUP"
        )
    }


    override fun create(flags: UInt): BitFlag =
        knownFlags.toList().firstOrNull {
            it.first.flags == flags
        }?.first ?: EpollEvent(flags)


    companion object {
        val ERR = EpollEvent(EPOLLERR.toUInt())
        val IN = EpollEvent(EPOLLIN.toUInt())
        val HUP = EpollEvent(EPOLLHUP.toUInt())
    }
}