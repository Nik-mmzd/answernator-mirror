package pw.modder.answernator.db

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

@Deprecated("Use db.guild.LogConfig instead")
class LogConfig(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<LogConfig>(LogConfigs)

    var guildId by LogConfigs.guildId

    var memberJoinLogChannel by LogConfigs.memberJoinLogChannel
    var memberLeaveLogChannel by LogConfigs.memberLeaveLogChannel
    var memberBanLogChannel by LogConfigs.memberBanLogChannel
    var memberUnbanLogChannel by LogConfigs.memberUnbanLogChannel
    var memberMuteLogChannel by LogConfigs.memberMuteLogChannel
    var memberUnmuteLogChannel by LogConfigs.memberUnmuteLogChannel

    var messageDeleteLogChannel by LogConfigs.messageDeleteLogChannel
    var messageBulkDeleteLogChannel by LogConfigs.messageBulkDeleteLogChannel
    var messageChangedLogChannel by LogConfigs.messageChangedLogChannel
}