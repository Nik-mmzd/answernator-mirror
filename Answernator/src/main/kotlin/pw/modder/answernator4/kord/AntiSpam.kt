package pw.modder.answernator4.kord

import dev.kord.common.Color
import dev.kord.common.asJavaLocale
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.ban
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.reply
import dev.kord.core.entity.User
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.embed
import dev.kord.rest.json.request.BulkDeleteRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import org.kodein.di.DI
import org.kodein.di.instance
import pw.modder.answernator4.antispam.AntiSpamDefaults
import pw.modder.answernator4.antispam.AttachmentSignature
import pw.modder.answernator4.antispam.MessageCanonicalizer
import pw.modder.answernator4.antispam.SpamAction
import pw.modder.answernator4.antispam.SpamThresholds
import pw.modder.answernator4.antispam.SpamTracker
import pw.modder.answernator4.antispam.TrackedMessage
import pw.modder.answernator4.db.cache.AntiSpamConfigData
import pw.modder.answernator4.db.cache.AntiSpamConfigRepository
import pw.modder.answernator4.db.cache.SpamMuteRepository
import pw.modder.answernator4.interaction.l
import java.util.ResourceBundle
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

private val logger = KotlinLogging.logger {}

private val COLOR_MUTE = Color(255, 170, 60)
private val COLOR_BAN = Color(255, 99, 126)

/** Discord caps a member timeout at 28 days. */
private val MAX_TIMEOUT = 28.days

/**
 * Normal anti-spam enforcement (Phase 2): detects repeated/similar messages per (guild, user),
 * warns, then mutes or bans based on the guild's repeat-offender configuration.
 *
 * MrBeast detection (Phase 3) and command-spam attribution (Phase 4) are layered on later; for now
 * bot messages are ignored entirely.
 */
suspend fun Kord.antiSpamService(di: DI) {
    val configRepository by di.instance<AntiSpamConfigRepository>()
    val muteRepository by di.instance<SpamMuteRepository>()

    // One ephemeral tracker for the whole process; intentionally not persisted across restarts.
    val tracker = SpamTracker()

    on<MessageCreateEvent> {
        val gid = guildId ?: return@on
        val author = message.author ?: return@on
        if (author.isBot) return@on
        if (message.content.isEmpty() && message.attachments.isEmpty()) return@on

        val config = configRepository.get(gid) ?: return@on
        if (!config.isEnabled) return@on

        val canon = MessageCanonicalizer.canonicalize(
            message.content,
            message.attachments.map { AttachmentSignature(it.filename, it.size.toLong()) },
        )
        val decision = tracker.record(
            guildId = gid.value,
            userId = author.id.value,
            canon = canon,
            message = TrackedMessage(message.channelId.value, message.id.value),
            thresholds = SpamThresholds(config.warningThreshold, config.muteThreshold),
        )

        when (decision.action) {
            SpamAction.NONE -> Unit

            SpamAction.WARN -> reply(message, config.warningText, author)

            SpamAction.ESCALATE -> {
                // Decide mute vs ban from persisted mute history within the validity window.
                val priorMutes = if (config.mutesBeforeBan == 0) 0L
                else muteRepository.countMutes(gid, author.id, config.muteValidity.days)
                val shouldBan = when {
                    config.mutesBeforeBan < 0 -> false // -1: never ban, mute only
                    config.mutesBeforeBan == 0 -> true // 0: ban immediately
                    else -> priorMutes >= config.mutesBeforeBan
                }

                if (shouldBan) {
                    banUser(config, author, priorMutes)
                } else {
                    muteUser(config, author, priorMutes, decision.trackedMessages, muteRepository)
                }
            }
        }
    }
}

/** Replies to the offending message with a guild-configured, user-facing text. */
private suspend fun MessageCreateEvent.reply(
    message: dev.kord.core.entity.Message,
    text: String,
    author: User,
) {
    try {
        message.reply { content = text.safeFormat(author.mention) }
    } catch (e: Exception) {
        logger.warn(e) { "Anti-spam: failed to reply in channel ${message.channelId}" }
    }
}

private suspend fun MessageCreateEvent.muteUser(
    config: AntiSpamConfigData,
    author: User,
    priorMutes: Long,
    tracked: List<TrackedMessage>,
    muteRepository: SpamMuteRepository,
) {
    val duration = minOf(config.muteDuration.minutes, MAX_TIMEOUT)
    val until = Clock.System.now() + duration

    val member = member ?: message.getGuildOrNull()?.getMemberOrNull(author.id)
    if (member == null) {
        logger.warn { "Anti-spam: cannot mute ${author.id} in $guildId — member unavailable" }
        return
    }

    try {
        member.edit {
            communicationDisabledUntil = until
            reason = config.muteText
        }
    } catch (e: Exception) {
        logger.warn(e) { "Anti-spam: failed to mute ${author.id} in $guildId" }
        return
    }

    reply(message, config.muteText, author)
    try {
        // Persist the mute as both audit and escalation state.
        muteRepository.insertMute(config.guildId, author.id, message.content)
    } catch (e: Exception) {
        logger.warn(e) { "Anti-spam: failed to persist mute for ${author.id} in $guildId" }
    }
    cleanupTracked(tracked, config.muteText)

    config.logChannel?.let { channel ->
        sendLog(channel, author, "antispam.log.mute.title", COLOR_MUTE) { bundle ->
            field {
                name = bundle.l("antispam.log.field.channel")
                value = "<#${message.channelId.value}>"
                inline = true
            }
            field {
                name = bundle.l("antispam.log.field.duration")
                value = bundle.l("antispam.log.minutes").safeFormat(duration.inWholeMinutes)
                inline = true
            }
            field {
                name = bundle.l("antispam.log.field.recent_mutes")
                value = (priorMutes + 1).toString()
                inline = true
            }
        }
    }
}

private suspend fun MessageCreateEvent.banUser(
    config: AntiSpamConfigData,
    author: User,
    priorMutes: Long,
) {
    val guild = message.getGuildOrNull()
    if (guild == null) {
        logger.warn { "Anti-spam: cannot ban ${author.id} — guild $guildId unavailable" }
        return
    }

    try {
        guild.ban(author.id) {
            // Discord cleans messages itself for the detection window; no manual tracking needed.
            deleteMessageDuration = AntiSpamDefaults.DETECTION_WINDOW
            reason = config.banText
        }
    } catch (e: Exception) {
        logger.warn(e) { "Anti-spam: failed to ban ${author.id} in $guildId" }
        return
    }

    // No channel reply — the user is gone. Report to the log channel only.
    config.logChannel?.let { channel ->
        sendLog(channel, author, "antispam.log.ban.title", COLOR_BAN) { bundle ->
            field {
                name = bundle.l("antispam.log.field.channel")
                value = "<#${message.channelId.value}>"
                inline = true
            }
            field {
                name = bundle.l("antispam.log.field.recent_mutes")
                value = priorMutes.toString()
                inline = true
            }
        }
    }
}

/** Deletes the offending group's tracked messages, grouped per channel (bulk where possible). */
private suspend fun MessageCreateEvent.cleanupTracked(tracked: List<TrackedMessage>, reason: String) {
    tracked.groupBy { it.channelId }.forEach { (channelId, messages) ->
        val channel = Snowflake(channelId)
        val ids = messages.map { Snowflake(it.messageId) }
        try {
            if (ids.size >= 2) {
                kord.rest.channel.bulkDelete(channel, BulkDeleteRequest(ids), reason)
            } else {
                kord.rest.channel.deleteMessage(channel, ids.first(), reason)
            }
        } catch (e: Exception) {
            logger.warn(e) { "Anti-spam: failed to clean up messages in $channel" }
        }
    }
}

/**
 * Builds and sends an anti-spam log embed to [channelId], localized by the guild's preferred locale.
 * Shared by mute/ban (and, later, MrBeast) reporting.
 */
private suspend fun MessageCreateEvent.sendLog(
    channelId: Snowflake,
    user: User,
    titleKey: String,
    color: Color,
    fields: EmbedBuilder.(ResourceBundle) -> Unit,
) {
    val locale = (message.getGuildOrNull()?.preferredLocale)?.asJavaLocale()
    val bundle = if (locale != null) ResourceBundle.getBundle("locale.v4.anti_spam", locale)
    else ResourceBundle.getBundle("locale.v4.anti_spam")
    try {
        kord.rest.channel.createMessage(channelId) {
            antiSpamEmbed(user, bundle.l(titleKey), color) { fields(bundle) }
        }
    } catch (e: Exception) {
        logger.warn(e) { "Anti-spam: failed to write to log channel $channelId" }
    }
}

private fun MessageCreateBuilder.antiSpamEmbed(
    user: User,
    title: String,
    color: Color,
    build: EmbedBuilder.() -> Unit,
) = embed {
    val avatar = user.avatar ?: user.defaultAvatar
    author {
        name = title
        icon = avatar.cdnUrl.toUrl()
    }
    description = "${user.mention} ${user.username}"
    this.color = color
    build()
    footer { text = "ID: ${user.id.value}" }
    timestamp = Clock.System.now()
}

private fun String.safeFormat(vararg args: Any?): String =
    try {
        format(*args)
    } catch (e: Exception) {
        this
    }
