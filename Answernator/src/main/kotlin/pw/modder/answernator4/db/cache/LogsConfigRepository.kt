package pw.modder.answernator4.db.cache

import dev.kord.common.Locale
import dev.kord.common.entity.Snowflake
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pw.modder.answernator4.db.tables.LogsConfig

/** Immutable snapshot of a guild's logging configuration, safe to hold outside a transaction. */
data class LogsConfigData(
    val guildId: Snowflake,
    val memberJoinChannel: Snowflake?,
    val memberLeaveChannel: Snowflake?,
    val memberBanLogChannel: Snowflake?,
    val memberUnbanLogChannel: Snowflake?,
    val memberMuteLogChannel: Snowflake?,
    val memberUpdateLogChannel: Snowflake?,
    val locale: Locale
)

class LogsConfigRepository(private val database: Database) : CachedConfigRepository<LogsConfigData>(database) {
    override fun load(guildId: Long): LogsConfigData? =
        LogsConfig.findById(guildId)?.let { it.toData() }

    /** Upserts a guild's whole logging configuration, then invalidates the cache so the next read reloads. */
    suspend fun put(data: LogsConfigData) {
        withContext(Dispatchers.IO) {
            transaction(database) {
                val entity = LogsConfig.findById(data.guildId.value.toLong())
                    ?: LogsConfig.new(data.guildId.value.toLong()) {}
                entity.apply {
                    memberJoinChannel = data.memberJoinChannel
                    memberLeaveChannel = data.memberLeaveChannel
                    memberBanLogChannel = data.memberBanLogChannel
                    memberUnbanLogChannel = data.memberUnbanLogChannel
                    memberMuteLogChannel = data.memberMuteLogChannel
                    memberUpdateLogChannel = data.memberUpdateLogChannel
                    locale = data.locale
                }
            }
        }
        invalidate(data.guildId)
    }

    private fun LogsConfig.toData() = LogsConfigData(
        guildId = Snowflake(id.value),
        memberJoinChannel = memberJoinChannel,
        memberLeaveChannel = memberLeaveChannel,
        memberBanLogChannel = memberBanLogChannel,
        memberUnbanLogChannel = memberUnbanLogChannel,
        memberMuteLogChannel = memberMuteLogChannel,
        memberUpdateLogChannel = memberUpdateLogChannel,
        locale = locale,
    )
}
