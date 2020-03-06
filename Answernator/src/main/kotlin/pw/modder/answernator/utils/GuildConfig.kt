package pw.modder.answernator.utils

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.list
import java.util.*

@UnstableDefault
@Serializable
data class GuildConfig(
    val guildId: String,
    val lang: String = GlobalConfig.get().lang,
    val greetNewUsers: Boolean = false,
    val greetingText: String = "%2\$s greets %1\$s!",
    val greetingsChannel: String = ""
) {
    @Transient
    val locale: Locale = Locale(lang)
}

@UnstableDefault
object GuildConfigs {
    private const val filename = "guilds.json"

    private fun read(): List<GuildConfig> {
        GlobalConfig.get().dataPath.resolve(filename).also { if (!it.exists()) return listOf() }.reader().use {
            return Json(JsonConfiguration.Default).parse(GuildConfig.serializer().list, it.readText())
        }
    }

    private fun save() {
        GlobalConfig.get().dataPath.resolve(filename).writer().use {
            it.write(Json(JsonConfiguration.Default).stringify(GuildConfig.serializer().list, guilds))
        }
    }

    private var guilds = read()

    fun get(guildId: String): GuildConfig {
        return guilds.singleOrNull { it.guildId == guildId } ?: GuildConfig(guildId).also {
            guilds = guilds + it
            save()
        }
    }

    fun replace(guildId: String, config: GuildConfig) {
        guilds = guilds.filterNot { it.guildId == guildId } + config
        save()
    }
}