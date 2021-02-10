package pw.modder.answernator.utils

import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.io.File
import kotlin.random.Random

object Globals {
    private const val configFileName = "config.json"

    val config: Config
    init {
        val configFile = File(configFileName)
        if (!configFile.exists()) {
            configFile.writeText(Json(JsonConfiguration.Stable.copy(prettyPrint = true)).stringify(Config.serializer(), Config.DEFAULT))
            throw Exception("Missing config")
        }
        config = Config.loadFrom(configFile)
    }

    val deps = Utils.loadDependenciesList()

    fun getDependencyVersion(group: String, name: String) = deps.single { it.group == group && it.name == name }.version

    val random = Random(System.currentTimeMillis())

    val httpClient = HttpClient()
}