package pw.modder.answernator.commands.utils

import kotlinx.serialization.Serializable

@Serializable
data class UbuntuWord(val first: List<String>, val second: List<String>)