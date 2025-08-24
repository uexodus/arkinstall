package cmd.bindings

import cinterop.SafeCPointer
import cinterop.types.NativeIntVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.pointed
import kotlinx.cinterop.value
import platform.linux.forkpty

@OptIn(ExperimentalForeignApi::class)
object PtyBindings {
    fun forkPty(): Pair<Int, Int> {
        // Create a pointer to store the master file descriptor
        val masterFd = SafeCPointer.alloc<IntVar, NativeIntVar>()

        val pid = masterFd.mut {
            forkpty(it, null, null, null)
        }

        return pid to masterFd.immut { it.pointed.value }
    }
}