package pw.modder.answernator.utils.extensions.bot

import com.jessecorbett.diskord.api.model.AuditLogActionType
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.DiskordDsl
import com.jessecorbett.diskord.util.mention
import com.jessecorbett.diskord.util.sendMessage
import kotlinx.coroutines.delay
import pw.modder.answernator.db.Db
import pw.modder.answernator.db.guild.Features
import pw.modder.answernator.utils.extensions.getAuditLog
import pw.modder.answernator.utils.extensions.toUserMention
import pw.modder.answernator.utils.locale.LocaleBundle
import java.util.*

@DiskordDsl
fun Bot.logService() {
    userBanned { ban ->

        val config = Db.getLogConfig(ban.guildId)
        if (!config.isEnabled(Features.LOG_BAN)) return@userBanned
        if (config.memberBanLogChannel == null) return@userBanned

        val client = clientStore.channels[config.memberBanLogChannel!!]
        val auditLog = clientStore.guilds[ban.guildId].getAuditLog(true).entries
            .firstOrNull { it.targetId == ban.user.id && it.actionType == AuditLogActionType.MEMBER_BAN_ADD.code }
            ?: run {
                delay(5000L)
                clientStore.guilds[ban.guildId].getAuditLog(true).entries
                    .firstOrNull { it.targetId == ban.user.id && it.actionType == AuditLogActionType.MEMBER_BAN_ADD.code }
            }

        val texts = LocaleBundle("botGlobal", Locale(config.lang))

        val reasonParts = auditLog?.reason?.split('|', limit = 2)
        when {
            // if no audit log we do not know who banned the user. "User AA was banned"
            auditLog == null -> client.sendMessage(texts.formatString("bot.log.ban.unknown", ban.user.mention))
            // we have audit log and know who banned a user but have no reason
            reasonParts == null || reasonParts.isEmpty() -> client.sendMessage(texts.formatString("bot.log.ban.noreason", ban.user.mention, auditLog.userId.toUserMention()))
            // seems like it's not a bot format but a reason itself
            reasonParts.size == 1 -> client.sendMessage(texts.formatString("bot.log.ban.reason", ban.user.mention, auditLog.userId.toUserMention(), auditLog.reason))
            // wtf? We have a reason in bot format, but reason is empty.
            reasonParts[1].isEmpty() -> client.sendMessage(texts.formatString("bot.log.ban.noreason", ban.user.mention, reasonParts[0].toUserMention()))
            // bot format, we know all data
            else -> client.sendMessage(texts.formatString("bot.log.ban.reason", ban.user.mention, reasonParts[0].toUserMention(), reasonParts[1]))
        }
    }
    userUnbanned { unBan ->
        val config = Db.getLogConfig(unBan.guildId)
        if (!config.isEnabled(Features.LOG_UNBAN)) return@userUnbanned
        if (config.memberUnbanLogChannel == null) return@userUnbanned

        val auditLog = clientStore.guilds[unBan.guildId].getAuditLog(true).entries
            .firstOrNull { it.targetId == unBan.user.id && it.actionType == AuditLogActionType.MEMBER_BAN_REMOVE.code }
            ?: run {
                delay(5000L)
                clientStore.guilds[unBan.guildId].getAuditLog(true).entries
                    .firstOrNull { it.targetId == unBan.user.id && it.actionType == AuditLogActionType.MEMBER_BAN_REMOVE.code }
            }

        val texts = LocaleBundle("botGlobal", Locale(config.lang))
        clientStore.channels[config.memberUnbanLogChannel!!].sendMessage(
            if (auditLog == null)
                texts.formatString("bot.log.unban", unBan.user.mention)
            else
                texts.formatString("bot.log.unban.full", unBan.user.mention, auditLog.userId.toUserMention())
        )
    }
    userJoinedGuild { memberJoin ->
        val config = Db.getLogConfig(memberJoin.guildId)
        if (!config.isEnabled(Features.LOG_JOIN)) return@userJoinedGuild
        if (config.memberJoinLogChannel == null) return@userJoinedGuild

        val texts = LocaleBundle("botGlobal", Locale(config.lang))
        clientStore.channels[config.memberJoinLogChannel!!].sendMessage(
            texts.formatString("bot.log.member.join", memberJoin.user?.mention ?: "??!? O_o")
        )
    }
    userLeftGuild { memberLeave ->
        val config = Db.getLogConfig(memberLeave.guildId)
        if (!config.isEnabled(Features.LOG_LEAVE)) return@userLeftGuild
        if (config.memberLeaveLogChannel == null) return@userLeftGuild

        val texts = LocaleBundle("botGlobal", Locale(config.lang))
        clientStore.channels[config.memberLeaveLogChannel!!].sendMessage(
            texts.formatString("bot.log.member.leave", memberLeave.user.mention)
        )
    }
}