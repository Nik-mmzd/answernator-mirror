package pw.modder.answernator.utils.extensions.bot

import com.jessecorbett.diskord.api.model.AuditLogActionType
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.DiskordDsl
import com.jessecorbett.diskord.util.mention
import com.jessecorbett.diskord.util.sendMessage
import kotlinx.coroutines.delay
import pw.modder.answernator.db.Db
import pw.modder.answernator.utils.extensions.getAuditLog
import pw.modder.answernator.utils.extensions.toUserMention
import pw.modder.answernator.utils.locale.LocaleBundle
import java.util.*

@DiskordDsl
fun Bot.logService() {
    userBanned { ban ->
        val logConfig = Db.logs.get(ban.guildId)
        val client = clientStore.channels[logConfig.memberBanLogChannel]
        val auditLog = clientStore.guilds[ban.guildId].getAuditLog(true).entries
            .firstOrNull { it.targetId == ban.user.id && it.actionType == AuditLogActionType.MEMBER_BAN_ADD.code }
            ?: run {
                delay(5000L)
                clientStore.guilds[ban.guildId].getAuditLog(true).entries
                    .firstOrNull { it.targetId == ban.user.id && it.actionType == AuditLogActionType.MEMBER_BAN_ADD.code }
            }

        if (logConfig.memberBanLogChannel.isNotEmpty()) {
            val guildConfig = Db.guilds.get(ban.guildId)
            val texts = LocaleBundle("botGlobal", Locale(guildConfig.lang))

            when {
                auditLog == null -> client.sendMessage(texts.formatString("bot.log.ban", ban.user.mention))
                auditLog.reason.isNullOrEmpty() -> client.sendMessage(texts.formatString("bot.log.ban", ban.user.mention))
                else -> {
                    val reasonParts = auditLog.reason.split('|', limit = 2)
                    client.sendMessage(
                        texts.formatString(
                            "bot.log.ban.full",
                            ban.user.mention,
                            if (reasonParts.size == 1) auditLog.userId.toUserMention() else reasonParts.first().toUserMention(),
                            reasonParts.last()
                        )
                    )
                }
            }
        }
    }
    userUnbanned { unBan ->
        val logConfig = Db.logs.get(unBan.guildId)
        val auditLog = clientStore.guilds[unBan.guildId].getAuditLog(true).entries
            .firstOrNull { it.targetId == unBan.user.id && it.actionType == AuditLogActionType.MEMBER_BAN_REMOVE.code }
            ?: run {
                delay(5000L)
                clientStore.guilds[unBan.guildId].getAuditLog(true).entries
                    .firstOrNull { it.targetId == unBan.user.id && it.actionType == AuditLogActionType.MEMBER_BAN_REMOVE.code }
            }

        if (logConfig.memberUnbanLogChannel.isNotEmpty()) {
            val guildConfig = Db.guilds.get(unBan.guildId)
            val texts = LocaleBundle("botGlobal", Locale(guildConfig.lang))
            clientStore.channels[logConfig.memberUnbanLogChannel].sendMessage(
                if (auditLog == null)
                    texts.formatString("bot.log.unban", unBan.user.mention)
                else
                    texts.formatString("bot.log.unban.full", unBan.user.mention, auditLog.userId.toUserMention())
            )
        }
    }
    userJoinedGuild { memberJoin ->
        val logConfig = Db.logs.get(memberJoin.guildId)

        if (logConfig.memberJoinLogChannel.isNotEmpty()) {
            val guildConfig = Db.guilds.get(memberJoin.guildId)
            val texts = LocaleBundle("botGlobal", Locale(guildConfig.lang))
            clientStore.channels[logConfig.memberJoinLogChannel].sendMessage(
                texts.formatString("bot.log.member.join", memberJoin.user?.mention ?: "??!? O_o")
            )
        }
    }
    userLeftGuild { memberLeave ->
        val logConfig = Db.logs.get(memberLeave.guildId)

        if (logConfig.memberLeaveLogChannel.isNotEmpty()) {
            val guildConfig = Db.guilds.get(memberLeave.guildId)
            val texts = LocaleBundle("botGlobal", Locale(guildConfig.lang))
            clientStore.channels[logConfig.memberLeaveLogChannel].sendMessage(
                texts.formatString("bot.log.member.leave", memberLeave.user.mention)
            )
        }
    }
}