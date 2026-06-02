package pw.modder.answernator.db.guild

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

@Deprecated("Deprecated. Use for migrations only")
internal object Configs: IntIdTable() {
    val guildId = varchar("guild_id", 18).uniqueIndex("guild_index")
    val features = integer("features")
    val lang = varchar("lang", 2)
    val commandPrefix = char("command_prefix")

    val greetingText = varchar("greeting_text", 2000)
    val greetingChannel = varchar("greeting_channel_id", 18).nullable()
    val muteRoleId = varchar("mute_role_id", 18).nullable()
    val defaultRoleId = varchar("default_role_id", 18).nullable()

    val antiSpamWarnText = varchar("antispam_text_warn", 500)
    val antiSpamBanText = varchar("antispam_text_ban", 500)
    val antiSpamWarn = integer("antispam_warning_count")
    val antiSpamBan = integer("antispam_ban_count")

    val memberJoinLogChannel = varchar("member_add_channel", 18).nullable() // https://discordapp.com/developers/docs/topics/gateway#guild-member-add
    val memberLeaveLogChannel = varchar("member_remove_channel", 18).nullable() // https://discordapp.com/developers/docs/topics/gateway#guild-member-remove
    val memberBanLogChannel = varchar("ban_add_channel", 18).nullable() // https://discordapp.com/developers/docs/topics/gateway#guild-ban-add
    val memberUnbanLogChannel = varchar("ban_remove_channel", 18).nullable() // https://discordapp.com/developers/docs/topics/gateway#guild-ban-remove
    val memberMuteLogChannel = varchar("mute_channel", 18).nullable() // bot internal
    val memberUnmuteLogChannel = varchar("unmute_channel", 18).nullable() // bot internal

//    val messageDeleteLogChannel = varchar("message_delete_channel", 18).default("") // https://discordapp.com/developers/docs/topics/gateway#message-delete
//    val messageBulkDeleteLogChannel = varchar("message_bulk_delete_channel", 18).default("") // https://discordapp.com/developers/docs/topics/gateway#message-delete-bulk
//    val messageChangedLogChannel = varchar("message_update_channel", 18).default("") // https://discordapp.com/developers/docs/topics/gateway#message-update
}

enum class Features {
    GREETING, DEFAULT_ROLE,
    ANTI_SPAM, ANTI_SPAM_SILENT,
    LOG_BAN, LOG_UNBAN, LOG_MUTE, LOG_UNMUTE, LOG_JOIN, LOG_LEAVE,
    MUTE_RANDOM_REASON
}
