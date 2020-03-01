package pw.modder.answernator.utils

import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.io.File

@Serializable
class Config(
    val token: String,
    val lang: String,
    val prefix: Char,
    val author: String
) {
    companion object {
        val DEFAULT = Config("", "ru_RU", '!', "135017849604276224")

        @UnstableDefault
        fun loadFrom(file: File): Config {
            return Json.parse(serializer(), file.readText(Charsets.UTF_8))
        }
    }
}

@UnstableDefault
object GlobalConfig {
    private val config: Config
    init {
        val configFile = File(".").resolve("config.json")
        if (!configFile.exists()) {
            Json(JsonConfiguration(prettyPrint = true)).stringify(Config.serializer(), Config.DEFAULT)
            throw Exception("Missing config")
        }
        config = Config.loadFrom(configFile)
    }

    fun get(): Config {
        return config
    }
}