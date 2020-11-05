package pw.modder.answernator.utils.extensions.bot

import com.jessecorbett.diskord.api.model.AuditLogActionType
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.DiskordDsl
import com.jessecorbett.diskord.util.mention
import com.jessecorbett.diskord.util.sendMessage
import kotlinx.coroutines.delay
import pw.modder.answernator.db.Db
import pw.modder.answernator.utils.UTF8Control
import pw.modder.answernator.utils.extensions.getAuditLog
import pw.modder.answernator.utils.extensions.getStringOrKey
import pw.modder.answernator.utils.extensions.toUserMention
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
            val texts = ResourceBundle.getBundle("locale.botGlobal", Locale(guildConfig.lang), UTF8Control())

            when {
                auditLog == null -> client.sendMessage(texts.getStringOrKey("bot.log.ban").format(ban.user.mention))
                auditLog.reason.isNullOrEmpty() -> client.sendMessage(texts.getStringOrKey("bot.log.ban").format(ban.user.mention))
                else -> {
                    val reasonParts = auditLog.reason.split('|', limit = 2)
                    client.sendMessage(
                        texts.getStringOrKey("bot.log.ban.full").format(
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
            val texts = ResourceBundle.getBundle("locale.botGlobal", Locale(guildConfig.lang), UTF8Control())
            clientStore.channels[logConfig.memberUnbanLogChannel].sendMessage(
                if (auditLog == null)
                    texts.getStringOrKey("bot.log.unban").format(unBan.user.mention)
                else
                    texts.getStringOrKey("bot.log.unban.full").format(unBan.user.mention, auditLog.userId.toUserMention())
            )
        }
    }
    userJoinedGuild { memberJoin ->
        val logConfig = Db.logs.get(memberJoin.guildId)

        if (logConfig.memberJoinLogChannel.isNotEmpty()) {
            val guildConfig = Db.guilds.get(memberJoin.guildId)
            val texts = ResourceBundle.getBundle("locale.botGlobal", Locale(guildConfig.lang), UTF8Control())
            clientStore.channels[logConfig.memberJoinLogChannel].sendMessage(
                texts.getStringOrKey("bot.log.member.join").format(memberJoin.user?.mention ?: "??!? O_o")
            )
        }
    }
    userLeftGuild { memberLeave ->
        val logConfig = Db.logs.get(memberLeave.guildId)

        if (logConfig.memberLeaveLogChannel.isNotEmpty()) {
            val guildConfig = Db.guilds.get(memberLeave.guildId)
            val texts = ResourceBundle.getBundle("locale.botGlobal", Locale(guildConfig.lang), UTF8Control())
            clientStore.channels[logConfig.memberLeaveLogChannel].sendMessage(
                texts.getStringOrKey("bot.log.member.leave").format(memberLeave.user.mention)
            )
        }
    }
}