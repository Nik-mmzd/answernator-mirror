package pw.modder.answernator.utils

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.io.File
import java.util.*

@Serializable
open class Config(
    @Required val token: String = "token-here",
    @Required val lang: String = "ru",
    @Required val prefix: Char = '!',
    @Required val author: String = "135017849604276224",
    @Required val langs: List<String> = listOf("ru", "en"),
    @Required val defaultStatus: String = "\$help"
) {
    @Transient
    val locale = Locale(lang)

    @Transient
    val dataPath = File("data").also { if (!it.exists()) it.mkdirs() }

    companion object {
        val DEFAULT = Config()

        @UnstableDefault
        fun loadFrom(file: File): Config {
            return Json.parse(serializer(), file.readText(Charsets.UTF_8))
        }
    }
}
