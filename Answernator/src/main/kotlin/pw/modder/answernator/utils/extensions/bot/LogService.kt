package pw.modder.answernator.utils.extensions.bot

import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.DiskordDsl
import com.jessecorbett.diskord.util.mention
import com.jessecorbett.diskord.util.sendMessage
import kotlinx.serialization.UnstableDefault
import pw.modder.answernator.db.Db
import pw.modder.answernator.utils.UTF8Control
import pw.modder.answernator.utils.extensions.getStringOrKey
import java.util.*

@UnstableDefault
@DiskordDsl
fun Bot.logService() {
    userBanned { ban ->
        val logConfig = Db.logs.get(ban.guildId)

        if (logConfig.memberBanLogChannel.isNotEmpty()) {
            val guildConfig = Db.guilds.get(ban.guildId)
            val texts = ResourceBundle.getBundle("locale.botGlobal", guildConfig.locale, UTF8Control())
            clientStore.channels[logConfig.memberBanLogChannel].sendMessage(
                texts.getStringOrKey("bot.log.ban").format(ban.user.mention)
            )
        }
    }
    userUnbanned { unban ->
        val logConfig = Db.logs.get(unban.guildId)

        if (logConfig.memberUnbanLogChannel.isNotEmpty()) {
            val guildConfig = Db.guilds.get(unban.guildId)
            val texts = ResourceBundle.getBundle("locale.botGlobal", guildConfig.locale, UTF8Control())
            clientStore.channels[logConfig.memberUnbanLogChannel].sendMessage(
                texts.getStringOrKey("bot.log.unban").format(unban.user.mention)
            )
        }
    }
    userJoinedGuild { memberJoin ->
        val logConfig = Db.logs.get(memberJoin.guildId)

        if (logConfig.memberJoinLogChannel.isNotEmpty()) {
            val guildConfig = Db.guilds.get(memberJoin.guildId)
            val texts = ResourceBundle.getBundle("locale.botGlobal", guildConfig.locale, UTF8Control())
            clientStore.channels[logConfig.memberJoinLogChannel].sendMessage(
                texts.getStringOrKey("bot.log.member.join").format(memberJoin.user?.mention ?: "??!? O_o")
            )
        }
    }
    userLeftGuild { memberLeave ->
        val logConfig = Db.logs.get(memberLeave.guildId)

        if (logConfig.memberLeaveLogChannel.isNotEmpty()) {
            val guildConfig = Db.guilds.get(memberLeave.guildId)
            val texts = ResourceBundle.getBundle("locale.botGlobal", guildConfig.locale, UTF8Control())
            clientStore.channels[logConfig.memberLeaveLogChannel].sendMessage(
                texts.getStringOrKey("bot.log.member.leave").format(memberLeave.user.mention)
            )
        }
    }
}