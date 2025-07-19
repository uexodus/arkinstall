package cmd.bindings

import kotlinx.cinterop.*
import platform.posix.waitpid

@OptIn(ExperimentalForeignApi::class)
object WaitBindings {
    fun waitPid(pid: Int): Int = memScoped {
        val status = alloc<IntVar>()
        waitpid(pid, status.ptr, 0)
        return status.value
    }
}