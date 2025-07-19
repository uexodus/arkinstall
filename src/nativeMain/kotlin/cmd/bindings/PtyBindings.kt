package cmd.bindings

import cinterop.SafeCPointer
import cinterop.types.NativeIntVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import platform.linux.forkpty

@OptIn(ExperimentalForeignApi::class)
object PtyBindings {
    fun forkPty(): Pair<Int, SafeCPointer<IntVar>> {
        // Create a pointer to store the master file descriptor
        val masterFd = SafeCPointer.alloc<IntVar, NativeIntVar>()

        val pid = masterFd.mut { forkpty(it, null, null, null) }

        return pid to masterFd
    }
}