package pw.modder.answernator.utils

import kotlinx.serialization.UnstableDefault

@UnstableDefault
class CommandError(cause: String, val command: Command): Exception(cause)