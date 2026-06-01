package pw.modder.answernator4.di

import dev.kord.common.Locale
import dev.kord.common.asJavaLocale
import dev.kord.common.entity.Snowflake
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.kodein.di.DI
import org.kodein.di.instance
import pw.modder.answernator.db.guild.Configs
import pw.modder.answernator.db.guild.Features
import pw.modder.answernator4.BuildConfig
import pw.modder.answernator4.db.DatabaseModule
import pw.modder.answernator4.db.tables.AntiSpamConfig
import pw.modder.answernator4.db.tables.LogsConfig
import pw.modder.answernator4.interaction.l
import java.util.ResourceBundle

private val logger = KotlinLogging.logger("pw.modder.answernator4.LegacyConfigMigration")

private const val ANTI_SPAM_BUNDLE = "locale.v4.anti_spam"

/**
 * One-shot migration of the legacy single-table [Configs] (`pw.modder.answernator`) into the v4
 * split tables `AntiSpamConfigs` and `LogsConfigs`. Assumes the legacy DB file has been moved so it
 * lives in the **same** datasource as v4 — no separate connection is opened.
 *
 * Runs in [DI.Module.onReady], i.e. after [DatabaseModule] has run Flyway and created the v4 tables.
 *
 * Mapping decisions (see the migration discussion):
 *  - a log channel is copied only when its legacy `LOG_*` feature flag was enabled;
 *  - anti-spam texts come from the legacy row, falling back to the [ANTI_SPAM_BUNDLE] bundle (in the
 *    guild's locale) when blank — `muteText` has no legacy source, so it always comes from the bundle;
 *  - `muteThreshold` mirrors the ban threshold (ban takes priority over mute);
 *  - legacy-only fields (greeting, roles, command prefix, unmute channel, silent/random-reason flags)
 *    are intentionally dropped.
 */
class LegacyConfigMigrationModule : KodeinModuleProvider {
    override val module = DI.Module("LegacyConfigMigration") {
        importOnce(DatabaseModule)

        onReady {
            migrateLegacyConfigs(instance())
        }
    }

    override val version = BuildConfig.APP_VERSION
}


@Suppress("DEPRECATION")
private fun migrateLegacyConfigs(database: Database) {
    // TODO(you): idempotency gate + cleanup. Run only when the legacy `Configs` table is present,
    //   and after a successful migration RENAME it (e.g. Configs -> CONFIGS_MIGRATED) so this does
    //   not run again. Until that gate exists, a missing legacy table is swallowed below.
    val legacyRows = try {
        transaction(db = database) {
            Configs.selectAll().map { row ->
                LegacyConfig(
                    guildId = row[Configs.guildId],
                    features = row[Configs.features],
                    lang = row[Configs.lang],
                    antiSpamWarnText = row[Configs.antiSpamWarnText],
                    antiSpamBanText = row[Configs.antiSpamBanText],
                    antiSpamWarn = row[Configs.antiSpamWarn],
                    antiSpamBan = row[Configs.antiSpamBan],
                    memberJoinLogChannel = row[Configs.memberJoinLogChannel],
                    memberLeaveLogChannel = row[Configs.memberLeaveLogChannel],
                    memberBanLogChannel = row[Configs.memberBanLogChannel],
                    memberUnbanLogChannel = row[Configs.memberUnbanLogChannel],
                    memberMuteLogChannel = row[Configs.memberMuteLogChannel],
                )
            }
        }
    } catch (e: Exception) {
        logger.info(e) { "Legacy Configs table not readable — skipping legacy config migration." }
        return
    }

    if (legacyRows.isEmpty()) {
        logger.info { "No legacy configs to migrate." }
        return
    }

    logger.info { "Migrating ${legacyRows.size} legacy guild config(s) into v4 tables…" }

    var migrated = 0
    transaction(db = database) {
        for (legacy in legacyRows) {
            val guildId = legacy.guildId.toLongOrNull()
            if (guildId == null) {
                logger.warn { "Skipping legacy config with non-numeric guild id '${legacy.guildId}'." }
                continue
            }

            val guildLocale = if (legacy.lang == "ru") Locale.RUSSIAN else Locale.ENGLISH_UNITED_STATES
            val bundle = ResourceBundle.getBundle(ANTI_SPAM_BUNDLE, guildLocale.asJavaLocale())

            migrateAntiSpam(guildId, legacy, bundle)
            migrateLogs(guildId, legacy, guildLocale)

            migrated++
            logger.info {
                "Migrated guild $guildId — antiSpam.enabled=${legacy.has(Features.ANTI_SPAM)}, " +
                    "logs=${legacy.enabledLogChannelCount()} channel(s), locale=${legacy.lang}"
            }
        }
    }

    logger.info { "Legacy config migration finished: $migrated/${legacyRows.size} migrated." }
}

private fun migrateAntiSpam(guildId: Long, legacy: LegacyConfig, bundle: ResourceBundle) {
    fun text(legacyValue: String, key: String): String =
        legacyValue.ifBlank { bundle.l(key) }

    val config = AntiSpamConfig.findById(guildId) ?: AntiSpamConfig.new(guildId) {}
    config.apply {
        isEnabled = legacy.has(Features.ANTI_SPAM)
        isMrBeastEnabled = false // new feature, no legacy source
        warningText = text(legacy.antiSpamWarnText, "antispam.warning")
        muteText = bundle.l("antispam.mute") // no legacy source
        warningThreshold = legacy.antiSpamWarn
        muteThreshold = legacy.antiSpamBan // enforcement fires at the legacy ban count
        mutesBeforeBan = 0 // legacy banned straight away (no mute stage) — preserve that
        muteDuration = 60 // minutes, default (unused while mutesBeforeBan == 0)
        muteValidity = 30 // days, default
    }
}

private fun migrateLogs(guildId: Long, legacy: LegacyConfig, guildLocale: Locale) {
    val config = LogsConfig.findById(guildId) ?: LogsConfig.new(guildId) {}
    config.apply {
        // Channel copied only when the corresponding legacy LOG_* feature was enabled.
        memberJoinChannel = legacy.channelIf(Features.LOG_JOIN, legacy.memberJoinLogChannel)
        memberLeaveChannel = legacy.channelIf(Features.LOG_LEAVE, legacy.memberLeaveLogChannel)
        memberBanLogChannel = legacy.channelIf(Features.LOG_BAN, legacy.memberBanLogChannel)
        memberUnbanLogChannel = legacy.channelIf(Features.LOG_UNBAN, legacy.memberUnbanLogChannel)
        memberMuteLogChannel = legacy.channelIf(Features.LOG_MUTE, legacy.memberMuteLogChannel)
        memberUpdateLogChannel = null // no legacy source
        locale = guildLocale
    }
}

private data class LegacyConfig(
    val guildId: String,
    val features: Int,
    val lang: String,
    val antiSpamWarnText: String,
    val antiSpamBanText: String,
    val antiSpamWarn: Int,
    val antiSpamBan: Int,
    val memberJoinLogChannel: String?,
    val memberLeaveLogChannel: String?,
    val memberBanLogChannel: String?,
    val memberUnbanLogChannel: String?,
    val memberMuteLogChannel: String?,
) {
    fun has(feature: Features): Boolean = (features and (1 shl feature.ordinal)) > 0

    /** Returns the channel snowflake only when [feature] is enabled and the legacy value is a valid id. */
    fun channelIf(feature: Features, value: String?): Snowflake? =
        if (has(feature)) value?.toLongOrNull()?.let(::Snowflake) else null

    fun enabledLogChannelCount(): Int = listOf(
        channelIf(Features.LOG_JOIN, memberJoinLogChannel),
        channelIf(Features.LOG_LEAVE, memberLeaveLogChannel),
        channelIf(Features.LOG_BAN, memberBanLogChannel),
        channelIf(Features.LOG_UNBAN, memberUnbanLogChannel),
        channelIf(Features.LOG_MUTE, memberMuteLogChannel),
    ).count { it != null }
}
