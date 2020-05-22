package pw.modder.answernator.utils

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.io.File
import java.util.*

@Serializable
open class Config(
    val token: String = "token-here",
    @SerialName("bot_id") val botId: String = "180080318135402497",
    val lang: String = "ru",
    val prefix: Char = '!',
    val author: String = "135017849604276224",
    val langs: List<String> = listOf("ru", "en"),
    @SerialName("status") val defaultStatus: String = "\$help"
) {
    @Transient
    val locale = Locale(lang)

    companion object {
        val DEFAULT = Config()

        fun loadFrom(file: File): Config {
            return Json(JsonConfiguration.Stable).parse(serializer(), file.readText(Charsets.UTF_8))
        }
    }
}
