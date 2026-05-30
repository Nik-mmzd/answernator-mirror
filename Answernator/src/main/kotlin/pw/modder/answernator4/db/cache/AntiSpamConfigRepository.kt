package pw.modder.answernator4.db.cache

import dev.kord.common.entity.Snowflake
import org.jetbrains.exposed.v1.jdbc.Database
import pw.modder.answernator4.db.tables.AntiSpamConfig

/** Immutable snapshot of a guild's anti-spam configuration, safe to hold outside a transaction. */
data class AntiSpamConfigData(
    val guildId: Snowflake,
    val isEnabled: Boolean,
    val isMrBeastEnabled: Boolean,
    val warningText: String,
    val muteText: String,
    val banText: String,
    val warningThreshold: Int,
    val muteThreshold: Int,
    val banRepeats: Int,
)

class AntiSpamConfigRepository(database: Database) : CachedConfigRepository<AntiSpamConfigData>(database) {
    override fun load(guildId: Long): AntiSpamConfigData? =
        AntiSpamConfig.findById(guildId)?.let {
            AntiSpamConfigData(
                guildId = Snowflake(guildId),
                isEnabled = it.isEnabled,
                isMrBeastEnabled = it.isMrBeastEnabled,
                warningText = it.warningText,
                muteText = it.muteText,
                banText = it.banText,
                warningThreshold = it.warningThreshold,
                muteThreshold = it.muteThreshold,
                banRepeats = it.banRepeats,
            )
        }
}
