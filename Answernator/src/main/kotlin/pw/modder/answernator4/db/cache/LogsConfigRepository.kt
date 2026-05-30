package pw.modder.answernator4.db.cache

import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.v1.jdbc.Database
import pw.modder.answernator4.db.tables.LogsConfig

/** Immutable snapshot of a guild's logging configuration, safe to hold outside a transaction. */
data class LogsConfigData(
    val guildId: Snowflake,
    val memberJoinChannel: Snowflake?,
    val memberLeaveChannel: Snowflake?,
    val memberBanLogChannel: Snowflake?,
    val memberUnbanLogChannel: Snowflake?,
    val memberMuteLogChannel: Snowflake?,
    val botLogChannel: Snowflake?,
) {
    val isMemberJoinLogEnabled: Boolean get() = memberJoinChannel != null
    val isMemberLeaveLogEnabled: Boolean get() = memberLeaveChannel != null
    val isMemberBanLogEnabled: Boolean get() = memberBanLogChannel != null
    val isMemberUnbanLogEnabled: Boolean get() = memberUnbanLogChannel != null
    val isMemberMuteLogEnabled: Boolean get() = memberMuteLogChannel != null
    val isBotLogChannelEnabled: Boolean get() = botLogChannel != null
}

class LogsConfigRepository(database: Database) : CachedConfigRepository<LogsConfigData>(database) {
    override fun load(guildId: Long): LogsConfigData? =
        LogsConfig.findById(guildId)?.let {
            LogsConfigData(
                guildId = Snowflake(guildId),
                memberJoinChannel = it.memberJoinChannel,
                memberLeaveChannel = it.memberLeaveChannel,
                memberBanLogChannel = it.memberBanLogChannel,
                memberUnbanLogChannel = it.memberUnbanLogChannel,
                memberMuteLogChannel = it.memberMuteLogChannel,
                botLogChannel = it.botLogChannel,
            )
        }
}
