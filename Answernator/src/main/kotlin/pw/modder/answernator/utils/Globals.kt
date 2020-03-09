package pw.modder.answernator.utils

import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.list
import java.io.File

@UnstableDefault
object Globals {
    private const val guildsConfigFileName = "guilds.json"
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

    private fun readGuildConfigs(filename: String = guildsConfigFileName): List<GuildConfig> {
        config.dataPath.resolve(filename).also { if (!it.exists()) return listOf() }.reader().use {
            return Json(JsonConfiguration.Default).parse(GuildConfig.serializer().list, it.readText())
        }
    }

    private fun saveGuildConfigs(filename: String = guildsConfigFileName) {
        config.dataPath.resolve(filename).writer().use {
            it.write(Json(JsonConfiguration.Default).stringify(GuildConfig.serializer().list, guildConfigs))
        }
    }

    private var guildConfigs = readGuildConfigs()

    fun getGuildConfig(guildId: String): GuildConfig {
        return guildConfigs.singleOrNull { it.guildId == guildId } ?: GuildConfig(guildId).also {
            guildConfigs = guildConfigs + it
            saveGuildConfigs()
        }
    }

    fun replaceGuildConfig(guildId: String, config: GuildConfig) {
        guildConfigs = guildConfigs.filterNot { it.guildId == guildId } + config
        saveGuildConfigs()
    }
}