package pw.modder.answernator.db

import org.jetbrains.exposed.dao.id.IntIdTable
import pw.modder.answernator.utils.Globals

@Deprecated("Use db.guild.Configs instead")
object GuildConfigs: IntIdTable() {
    val guildId = varchar("guild_id", 18).index(isUnique = true)
    val lang = varchar("lang", 2).default(Globals.config.lang)
    val greetNewUsers = bool("greet_new_users").default(false)
    val greetingText = varchar("greeting_text", 2000).default("%2\$s greets %1\$s!")
    val greetingsChannel = varchar("greeting_chanel_id", 18).default("")
    val muteRole = varchar("mute_role_id", 18).default("")
    val defaultRole = varchar("default_role_id", 18).default("")
    val antiSpam = bool("anti_spam_enabled").default(false)
}