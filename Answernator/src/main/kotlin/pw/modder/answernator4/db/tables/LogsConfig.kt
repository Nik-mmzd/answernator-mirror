package pw.modder.answernator4.db.tables

import dev.kord.common.Locale
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import pw.modder.answernator4.db.asSnowflake

object LogsConfigs : IdTable<Long>() {
    // id is the guild snowflake, supplied externally — not autoincremented.
    override val id: Column<EntityID<Long>> = long("id").entityId()
    override val primaryKey = PrimaryKey(id)

    val memberJoinChannel = long("member_join_log").asSnowflake().nullable()
    val memberLeaveChannel = long("member_leave_log").asSnowflake().nullable()
    val memberBanLogChannel = long("member_ban_log").asSnowflake().nullable()
    val memberUnbanLogChannel = long("member_unban_log").asSnowflake().nullable()
    val memberMuteLogChannel = long("member_mute_log").asSnowflake().nullable()
    val memberUpdateLogChannel = long("member_update_log").asSnowflake().nullable()
    val botLogChannel = long("bot_log").asSnowflake().nullable()
    val locale = varchar("locale", 16).transform(
        wrap = { Locale.fromString(it) },
        unwrap = { "${it.language}${it.country?.let { c -> "-$c" } ?: ""}" }
    )
}

class LogsConfig(id: EntityID<Long>): LongEntity(id) {
    companion object : LongEntityClass<LogsConfig>(LogsConfigs)

    var memberJoinChannel by LogsConfigs.memberJoinChannel
    var memberLeaveChannel by LogsConfigs.memberLeaveChannel
    var memberBanLogChannel by LogsConfigs.memberBanLogChannel
    var memberUnbanLogChannel by LogsConfigs.memberUnbanLogChannel
    var memberMuteLogChannel by LogsConfigs.memberMuteLogChannel
    var memberUpdateLogChannel by LogsConfigs.memberUpdateLogChannel
    var botLogChannel by LogsConfigs.botLogChannel
    var locale by LogsConfigs.locale
}
