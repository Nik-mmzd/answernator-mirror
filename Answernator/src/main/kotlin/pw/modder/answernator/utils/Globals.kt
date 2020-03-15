package pw.modder.answernator.utils

import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.io.File
import kotlin.random.Random

@UnstableDefault
object Globals {
    private const val configFileName = "config.json"

    val config: Config
    init {
        val configFile = File(configFileName)
        if (!configFile.exists()) {
            configFile.writeText(Json(JsonConfiguration(prettyPrint = true)).stringify(Config.serializer(), Config.DEFAULT))
            throw Exception("Missing config")
        }
        config = Config.loadFrom(configFile)
    }

     val deps = Utils.loadDependenciesList()

    fun getDependencyVersion(group: String, name: String) = deps.single { it.group == group && it.name == name }.version

    val random = Random(System.currentTimeMillis())
}