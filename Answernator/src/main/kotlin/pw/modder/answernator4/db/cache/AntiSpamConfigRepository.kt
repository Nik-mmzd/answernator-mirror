package pw.modder.answernator4.db.cache

import dev.kord.common.entity.Snowflake
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pw.modder.answernator4.db.tables.AntiSpamConfig

/** Immutable snapshot of a guild's anti-spam configuration, safe to hold outside a transaction. */
data class AntiSpamConfigData(
    val guildId: Snowflake,
    val isEnabled: Boolean,
    val isMrBeastEnabled: Boolean,
    /** User-facing reply when a single warning fires. */
    val warningText: String,
    /** User-facing reply shown when the offender is muted. */
    val muteText: String,
    val warningThreshold: Int,
    val muteThreshold: Int,
    /** -1 = never ban (mute-only); 0 = ban immediately; N>0 = ban after N mutes within [muteValidity]. */
    val mutesBeforeBan: Int,
    /** Mute timeout length, minutes. */
    val muteDuration: Int,
    /** Days a mute keeps counting toward the ban threshold. */
    val muteValidity: Int,
    val logChannel: Snowflake?,
)

class AntiSpamConfigRepository(private val database: Database) : CachedConfigRepository<AntiSpamConfigData>(database) {
    override fun load(guildId: Long): AntiSpamConfigData? =
        AntiSpamConfig.findById(guildId)?.let { it.toData() }

    /** Upserts a guild's whole anti-spam configuration, then invalidates the cache so the next read reloads. */
    suspend fun put(data: AntiSpamConfigData) {
        withContext(Dispatchers.IO) {
            transaction(database) {
                val entity = AntiSpamConfig.findById(data.guildId.value.toLong())
                    ?: AntiSpamConfig.new(data.guildId.value.toLong()) {}
                entity.apply {
                    isEnabled = data.isEnabled
                    isMrBeastEnabled = data.isMrBeastEnabled
                    warningText = data.warningText
                    muteText = data.muteText
                    warningThreshold = data.warningThreshold
                    muteThreshold = data.muteThreshold
                    mutesBeforeBan = data.mutesBeforeBan
                    muteDuration = data.muteDuration
                    muteValidity = data.muteValidity
                    logChannel = data.logChannel
                }
            }
        }
        invalidate(data.guildId)
    }

    private fun AntiSpamConfig.toData() = AntiSpamConfigData(
        guildId = Snowflake(id.value),
        isEnabled = isEnabled,
        isMrBeastEnabled = isMrBeastEnabled,
        warningText = warningText,
        muteText = muteText,
        warningThreshold = warningThreshold,
        muteThreshold = muteThreshold,
        mutesBeforeBan = mutesBeforeBan,
        muteDuration = muteDuration,
        muteValidity = muteValidity,
        logChannel = logChannel,
    )
}
