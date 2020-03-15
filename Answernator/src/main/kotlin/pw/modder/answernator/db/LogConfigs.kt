package pw.modder.answernator.db

import org.jetbrains.exposed.dao.id.IntIdTable

object LogConfigs: IntIdTable() {
    val guildId = varchar("guild_id", 18).index(isUnique = true)

    val memberJoinLogChannel = varchar("member_add_channel", 18).default("") // https://discordapp.com/developers/docs/topics/gateway#guild-member-add
    val memberLeaveLogChannel = varchar("member_remove_channel", 18).default("") // https://discordapp.com/developers/docs/topics/gateway#guild-member-remove
    val memberBanLogChannel = varchar("ban_add_channel", 18).default("") // https://discordapp.com/developers/docs/topics/gateway#guild-ban-add
    val memberUnbanLogChannel = varchar("ban_remove_channel", 18).default("") // https://discordapp.com/developers/docs/topics/gateway#guild-ban-remove
    val memberMuteLogChannel = varchar("mute_channel", 18).default("") // bot internal
    val memberUnmuteLogChannel = varchar("unmute_channel", 18).default("") // bot internal

    val messageDeleteLogChannel = varchar("message_delete_channel", 18).default("") // https://discordapp.com/developers/docs/topics/gateway#message-delete
    val messageBulkDeleteLogChannel = varchar("message_bulk_delete_channel", 18).default("") // https://discordapp.com/developers/docs/topics/gateway#message-delete-bulk
    val messageChangedLogChannel = varchar("message_update_channel", 18).default("") // https://discordapp.com/developers/docs/topics/gateway#message-update
}