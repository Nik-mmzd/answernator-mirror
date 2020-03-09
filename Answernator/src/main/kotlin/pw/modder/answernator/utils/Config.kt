package pw.modder.answernator.utils

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*

@Serializable
open class Config(
    val token: String = "token-here",
    val lang: String = "ru",
    val prefix: Char = '!',
    val author: String = "135017849604276224",
    val langs: List<String> = listOf("ru", "en"),
    val defaultStatus: String = "\$help"
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
