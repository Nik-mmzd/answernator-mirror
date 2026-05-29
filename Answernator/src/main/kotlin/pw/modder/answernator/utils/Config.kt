package pw.modder.answernator.utils

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*

private val JSON = Json { encodeDefaults = true; ignoreUnknownKeys = true }

@Deprecated("Switch to v4")
@Serializable
open class Config(
    val token: String = "token-here",
    val lang: String = "ru",
    val prefix: Char = '!',
    val author: String = "135017849604276224",
    val langs: List<String> = listOf("ru", "en"),
    @SerialName("status") val defaultStatus: String = "\$help",
    @SerialName("blacklisted_commands") val commandsBlacklist: List<String> = listOf()
) {
    @Transient
    val locale = Locale(lang)

    companion object {
        val DEFAULT = Config()

        fun loadFrom(file: File): Config {
            return JSON.decodeFromString(serializer(), file.readText(Charsets.UTF_8))
        }
    }
}
