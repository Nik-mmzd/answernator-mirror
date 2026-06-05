package pw.modder.answernator4.kord

import dev.kord.common.Color
import dev.kord.common.asJavaLocale
import dev.kord.common.entity.AuditLogChangeKey
import dev.kord.common.entity.AuditLogEvent
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.getAuditLogEntries
import dev.kord.core.entity.AuditLogEntry
import dev.kord.core.entity.Member
import dev.kord.core.entity.User
import dev.kord.core.event.guild.BanAddEvent
import dev.kord.core.event.guild.BanRemoveEvent
import dev.kord.core.event.guild.GuildAuditLogEntryCreateEvent
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberLeaveEvent
import dev.kord.core.event.guild.MemberUpdateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.embed
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.firstOrNull
import org.kodein.di.DI
import org.kodein.di.instance
import pw.modder.answernator.utils.TimestampFormat
import pw.modder.answernator.utils.mention
import pw.modder.answernator4.db.cache.LogsConfigRepository
import pw.modder.answernator4.interaction.l
import java.util.ResourceBundle
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger("pw.modder.answernator4.kord.GuildLogService")

private fun MessageCreateBuilder.userEmbed(user: User, title: String, builder: EmbedBuilder.() -> Unit = {}) = embed {
    val avatar = (user.avatar ?: user.defaultAvatar)
    author {
        name = title
        icon = avatar.cdnUrl.toUrl()
    }
    description = "${user.mention} ${user.username}"
    builder()
    footer {
        text = "ID: ${user.id.value}"
    }

    thumbnail { url = ((user as? Member)?.memberAvatar ?: avatar).cdnUrl.toUrl() }
    timestamp = Clock.System.now()
}

private fun MessageCreateBuilder.userEmbed(user: User, title: String, bundle: ResourceBundle, auditLogEntry: AuditLogEntry?, builder: EmbedBuilder.() -> Unit = {}) = userEmbed(user, title) {
    builder()

    if (auditLogEntry?.userId != null) {
        field {
            name = bundle.l("audit.log.issuer")
            value = "<@${auditLogEntry.userId!!.value}>"
            inline = true
        }
    }
    if (auditLogEntry?.reason?.isNotBlank() == true) {
        field {
            name = bundle.l("audit.log.reason")
            value = auditLogEntry.reason!!
            inline = false
        }
    }
}

private suspend fun GuildBehavior.getAuditLogEntryOrNull(
    target: Snowflake,
    type: AuditLogEvent,
    filter: (AuditLogEntry) -> Boolean = { it.targetId == target }
): AuditLogEntry? {
    val now = Clock.System.now()
    try {
        return getAuditLogEntries {
            action = AuditLogEvent.MemberBanAdd
            after = Snowflake(now.minus(30.seconds))
            before = Snowflake(now.plus(30.seconds))
        }.firstOrNull(filter)
    } catch (e: Exception) {
        logger.debug(e) { "Error fetching auditLogEntry" }
        return null
    }
}

private val COLOR_GOOD = Color(49, 201, 80)
private val COLOR_BAD = Color(255, 99, 126)
private val COLOR_NEUTRAL = Color(52, 166, 244)

private val ACCEPTED_EVENTS = setOf(
    AuditLogEvent.MemberBanAdd,
    AuditLogEvent.MemberBanRemove,
    AuditLogEvent.MemberUpdate,
)

suspend fun Kord.guildLogService(di: DI) {
    val configRepository by di.instance<LogsConfigRepository>()

    on<GuildAuditLogEntryCreateEvent> {
        logger.trace { with(auditLogEntry) { "Got GuildAuditLogEntryCreateEvent id $id type $actionType from guild $guildId, user: $userId, target: $targetId" } }
        val guildId = auditLogEntry.guildId ?: return@on
        val targetId = auditLogEntry.targetId ?: return@on
        if (auditLogEntry.actionType !in ACCEPTED_EVENTS) return@on
        val config = configRepository.get(guildId) ?: return@on
        logger.trace { "GuildAuditLogEntryCreateEvent ${auditLogEntry.id} got config $config" }
        val (channel, title) = when (auditLogEntry.actionType) {
            AuditLogEvent.MemberBanAdd -> config.memberBanLogChannel to "member.ban.title"
            AuditLogEvent.MemberBanRemove -> config.memberUnbanLogChannel to "member.unban.title"
            AuditLogEvent.MemberUpdate -> config.memberMuteLogChannel to "member.timeout.set"
            else -> return@on
        }
        if (channel == null) return@on
        val user = getUser(targetId, EntitySupplyStrategy.cacheWithRestFallback) ?: return@on
        logger.trace { "GuildAuditLogEntryCreateEvent ${auditLogEntry.id} got user" }
        val bundle = ResourceBundle.getBundle("locale.v4.guild_log", config.locale.asJavaLocale())
        logger.info { "Got Audit Log: Guild $guildId, action ${auditLogEntry.actionType}, performed by ${auditLogEntry.userId}, target ${user.username} (id ${user.id}), report to $channel" }
        rest.channel.createMessage(channel) {
            userEmbed(user, bundle.l(title), bundle, auditLogEntry) {
                color = if (auditLogEntry.actionType == AuditLogEvent.MemberBanRemove) COLOR_GOOD else COLOR_BAD

                auditLogEntry[AuditLogChangeKey.CommunicationDisabledUntil]?.new?.let { timestamp ->
                    field {
                        name = bundle.l("member.timeout.until")
                        value = timestamp.mention(TimestampFormat.LONG_DATETIME)
                        inline = true
                    }
                }
            }
        }
    }

    on<MemberJoinEvent> {
        val config = configRepository.get(guildId) ?: return@on
        val channel = config.memberJoinChannel ?: return@on
        val bundle = ResourceBundle.getBundle("locale.v4.guild_log", config.locale.asJavaLocale())
        rest.channel.createMessage(channel) {
            userEmbed(member, bundle.l("member.join.title")) {
                field {
                    name = bundle.l("member.join.registered")
                    value = member.id.timestamp.mention(TimestampFormat.RELATIVE)
                    inline = true
                }
                color = COLOR_GOOD
            }
        }
    }

    on<MemberLeaveEvent> {
        val config = configRepository.get(guildId) ?: return@on
        val channel = config.memberLeaveChannel ?: return@on
        val bundle = ResourceBundle.getBundle("locale.v4.guild_log", config.locale.asJavaLocale())
        rest.channel.createMessage(channel) {
            userEmbed(user, bundle.l("member.leave.title")) { color = COLOR_BAD }
        }
    }

    on<MemberUpdateEvent> {
        val old = old ?: return@on // we do not have any data to compare with
        val config = configRepository.get(guildId) ?: return@on
        val channel = config.memberUpdateLogChannel ?: return@on
        val bundle = ResourceBundle.getBundle("locale.v4.guild_log", config.locale.asJavaLocale())

        if (member.memberAvatarHash != old.memberAvatarHash) {
            // member avatar update
            rest.channel.createMessage(channel) {
                userEmbed(member, bundle.l("member.avatar.title")) { color = COLOR_NEUTRAL }
            }
        }

        if (member.avatarHash != old.avatarHash) {
            // avatar update
            rest.channel.createMessage(channel) {
                userEmbed(member, bundle.l("user.avatar.title")) { color = COLOR_NEUTRAL }
            }
        }

        if (member.effectiveName != old.effectiveName) {
            // name update
            rest.channel.createMessage(channel) {
                userEmbed(member, bundle.l("member.name.title")) {
                    field {
                        name = bundle.l("member.name.old")
                        value = old.effectiveName
                        inline = true
                    }
                    field {
                        name = bundle.l("member.name.new")
                        value = member.effectiveName
                        inline = true
                    }
                    color = COLOR_NEUTRAL
                }
            }
        }

        if (member.username != old.username) {
            // name update
            rest.channel.createMessage(channel) {
                userEmbed(member, bundle.l("member.username.title")) {
                    field {
                        name = bundle.l("member.name.old")
                        value = old.username
                        inline = true
                    }
                    field {
                        name = bundle.l("member.name.new")
                        value = member.username
                        inline = true
                    }
                    color = COLOR_NEUTRAL
                }
            }
        }
    }
}
