package pw.modder.answernator.utils

import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.random.Random

object Globals {
    private const val configFileName = "config.json"
    private val JSON = Json { prettyPrint = true; encodeDefaults = true }

    val config: Config
    init {
        val configFile = File(configFileName)
        if (!configFile.exists()) {
            configFile.writeText(JSON.encodeToString(Config.serializer(), Config.DEFAULT))
            throw Exception("Missing config")
        }
        config = Config.loadFrom(configFile)
    }

    val deps = Utils.loadDependenciesList()

    fun getDependencyVersion(group: String, name: String): String {
        val nameWithJvm = "${name}-jvm"
        return deps.single { it.group == group && (it.name == name || it.name == nameWithJvm) }.version
    }

    val random = Random(System.currentTimeMillis())

    val httpClient = HttpClient()
}