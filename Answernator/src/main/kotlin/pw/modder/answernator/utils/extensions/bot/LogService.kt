package pw.modder.answernator.utils.extensions.bot

import dev.kord.common.entity.AuditLogEvent
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.getAuditLogEntries
import dev.kord.core.event.guild.BanAddEvent
import dev.kord.core.event.guild.BanRemoveEvent
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberLeaveEvent
import dev.kord.core.on
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import pw.modder.answernator.db.Db
import pw.modder.answernator.db.guild.Features
import pw.modder.answernator.utils.extensions.toUserMention
import pw.modder.answernator.utils.locale.LocaleBundle
import java.util.*

suspend fun Kord.logService() {
    on<BanAddEvent> {
        val config = Db.getLogConfig(guildId)
        if (!config.isEnabled(Features.LOG_BAN)) return@on
        val banChannel = Snowflake(config.memberBanLogChannel ?: return@on)
        val texts = LocaleBundle("botGlobal", Locale(config.lang))
        val reasonParts = getBanOrNull()?.reason?.split('|', limit = 2)

        if (reasonParts?.size == 2 && reasonParts.last().isNotEmpty()) {
            rest.channel.createMessage(banChannel) {
                content = texts.formatString("bot.log.ban.reason", user.mention, reasonParts[0].toUserMention(), reasonParts[1])
            }
            return@on
        }

        delay(1000L)
        val auditLog = guild.getAuditLogEntries {
            this.action = AuditLogEvent.MemberBanAdd
        }.firstOrNull { it.targetId == user.id }

        when {
            // if no audit log we do not know who banned the user. "User AA was banned"
            auditLog == null -> rest.channel.createMessage(banChannel) {
                content = texts.formatString("bot.log.ban.unknown", user.mention)
            }
            // we have audit log and know who banned a user but have no reason
            reasonParts == null || reasonParts.isEmpty() -> rest.channel.createMessage(banChannel) {
                content = texts.formatString("bot.log.ban.noreason", user.mention, auditLog.userId.asString.toUserMention())
            }
            // seems like it's not a bot format but a reason itself
            reasonParts.size == 1 -> rest.channel.createMessage(banChannel) {
                content = texts.formatString("bot.log.ban.reason", user.mention, auditLog.userId.asString.toUserMention(), auditLog.reason!!)
            }
            // wtf? We have a reason in bot format, but reason is empty.
            else -> rest.channel.createMessage(banChannel) {
                content = texts.formatString("bot.log.ban.noreason", user.mention, reasonParts[0].toUserMention())
            }
        }
    }

    on<BanRemoveEvent> {
        val config = Db.getLogConfig(guildId)
        if (!config.isEnabled(Features.LOG_UNBAN)) return@on
        val channel = Snowflake(config.memberUnbanLogChannel ?: return@on)

        delay(1000L)
        val auditLog = guild.getAuditLogEntries {
            action = AuditLogEvent.MemberBanRemove
        }.firstOrNull { it.targetId == user.id }
        val texts = LocaleBundle("botGlobal", Locale(config.lang))

        when(auditLog) {
            null -> rest.channel.createMessage(channel) {
                content = texts.formatString("bot.log.unban", user.mention)
            }
            else -> rest.channel.createMessage(channel) {
                content = texts.formatString("bot.log.unban.full", user.mention, auditLog.userId.asString.toUserMention())
            }
        }
    }

    on<MemberJoinEvent> {
        val config = Db.getLogConfig(guildId)
        if (!config.isEnabled(Features.LOG_JOIN)) return@on
        val channel = Snowflake(config.memberJoinLogChannel ?: return@on)
        val texts = LocaleBundle("botGlobal", Locale(config.lang))

        rest.channel.createMessage(channel) {
            content = texts.formatString("bot.log.member.join", member.mention)
        }
    }

    on<MemberLeaveEvent> {
        val config = Db.getLogConfig(guildId)
        if (!config.isEnabled(Features.LOG_LEAVE)) return@on
        val channel = Snowflake(config.memberJoinLogChannel ?: return@on)
        val texts = LocaleBundle("botGlobal", Locale(config.lang))

        rest.channel.createMessage(channel) {
            content = texts.formatString("bot.log.member.leave", user.mention)
        }
    }
}