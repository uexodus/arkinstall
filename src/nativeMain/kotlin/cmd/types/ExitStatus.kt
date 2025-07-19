package cmd.types

import cmd.bindings.WaitBindings

class ExitStatus(pid: Int) {
    private val status = WaitBindings.waitPid(pid)

    val exitedNormally: Boolean = (status and 0xff) == 0
    val exitStatus: Int = (status and 0xff00) shr 8
    val isSuccessful = (exitedNormally && exitStatus == 0)
}