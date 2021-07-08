package pw.modder.answernator.db.guild

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Configs: IntIdTable() {
    val guildId = varchar("guild_id", 18)
    val features = integer("features")
    val lang = varchar("lang", 2)

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

class Config(id: EntityID<Int>): IntEntity(id) {
    companion object : IntEntityClass<Config>(Configs)

    var guildId by Configs.guildId
    var features by Configs.features
    var lang by Configs.lang

    var greeting by Configs.greetingText
    var greetingChannel by Configs.greetingChannel
    var muteRole by Configs.muteRoleId
    var defaultRole by Configs.defaultRoleId

    var antiSpamWarnText by Configs.antiSpamWarnText
    var antiSpamBanText by Configs.antiSpamBanText
    var antiSpamWarn by Configs.antiSpamWarn
    var antiSpamBan by Configs.antiSpamBan

    var memberJoinLogChannel by Configs.memberJoinLogChannel
    var memberLeaveLogChannel by Configs.memberLeaveLogChannel
    var memberBanLogChannel by Configs.memberBanLogChannel
    var memberUnbanLogChannel by Configs.memberUnbanLogChannel
    var memberMuteLogChannel by Configs.memberMuteLogChannel
    var memberUnmuteLogChannel by Configs.memberUnmuteLogChannel

//    var messageDeleteLogChannel by Configs.messageDeleteLogChannel
//    var messageBulkDeleteLogChannel by Configs.messageBulkDeleteLogChannel
//    var messageChangedLogChannel by Configs.messageChangedLogChannel

    var mutes by Mute via MutesRef

    fun isEnabled(feature: Features): Boolean {
        return (features and(1 shl feature.ordinal)) > 0
    }

    fun getEnabled(): List<Features> {
        return Features.values().filter { isEnabled(it) }
    }

    fun enable(feature: Features) {
        features = features or(1 shl feature.ordinal)
    }

    fun disable(feature: Features) {
        features = features and((1 shl feature.ordinal).inv())
    }
}

enum class Features {
    GREETING, DEFAULT_ROLE, ANTI_SPAM, ANTI_SPAM_SILENT, LOG_BAN, LOG_UNBAN, LOG_MUTE, LOG_UNMUTE, LOG_JOIN, LOG_LEAVE
}