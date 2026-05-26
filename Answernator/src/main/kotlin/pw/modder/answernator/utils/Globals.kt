package pw.modder.answernator.utils

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.random.Random

private val logger = KotlinLogging.logger {}
object Globals {
    private const val configFileName = "config.json"
    private val JSON = Json { prettyPrint = true; encodeDefaults = true }

    val config: Config
    init {
        val configFile = File(configFileName)
        if (!configFile.exists()) {
            configFile.writeText(JSON.encodeToString(Config.serializer(), Config.DEFAULT))
            throw RuntimeException("Missing config")
        }
        config = Config.loadFrom(configFile)
    }

    val deps = try {
        Utils.loadDependenciesList()
    } catch (e: IOException) {
        logger.warn(e) { "Cannot load dependencies list" }
        emptyList()
    }

    fun getDependencyVersion(group: String, name: String): String {
        val nameWithJvm = "${name}-jvm"
        return deps.singleOrNull { it.group == group && (it.name == name || it.name == nameWithJvm) }?.version ?: "unknown"
    }

    val random = Random(System.currentTimeMillis())

    val httpClient = HttpClient()
}
