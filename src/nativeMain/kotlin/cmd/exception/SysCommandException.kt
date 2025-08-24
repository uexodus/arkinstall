package cmd.exception

import cmd.types.ExitStatus

class SysCommandException(cmd: String, status: ExitStatus, output: String) : Exception(
    "'$cmd' failed with non zero exit status ${status.exitStatus}.\nOutput:\n$output"
)