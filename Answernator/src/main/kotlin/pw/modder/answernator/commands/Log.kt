package pw.modder.answernator.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.util.words
import org.jetbrains.exposed.sql.Column
import pw.modder.answernator.db.Db
import pw.modder.answernator.db.guild.Features
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.LocalizedCommand
import pw.modder.answernator.utils.locale.CommandLocaleBundle
import java.util.*
import com.jessecorbett.diskord.dsl.message as dslmessage

class Log: LocalizedCommand {
    override val name = "log"
    override val userGroup = Command.UserGroup.ADMIN
    override val cmdType = Command.CommandGroup.ADMIN
    override val channels: EnumSet<Command.ChannelTypes> = EnumSet.of(Command.ChannelTypes.GUILD)

    override suspend fun action(bot: Bot, message: Message, texts: CommandLocaleBundle): CombinedMessageEmbed {

        val guild = message.guildId?.run { bot.clientStore.guilds[this] }
            ?: return texts.getErrorString().toMessage()
        val config = Db.getLogConfig(guild.guildId)

        if (message.words.size == 1) return texts.getErrorString().toMessage()

        if (message.words[1].equals("get", true))
            return dslmessage {
                title = texts.getString("get.title")

                field(texts.getString("get.memberjoin"), texts.formatString("get.status", texts.getString("get.status.${config.isEnabled(Features.LOG_JOIN)}"), config.memberJoinLogChannel ?: texts.getString("get.not.set")), true)
                field(texts.getString("get.memberleave"), texts.formatString("get.status", texts.getString("get.status.${config.isEnabled(Features.LOG_LEAVE)}"), config.memberLeaveLogChannel ?: texts.getString("get.not.set")), true)
                field(texts.getString("get.memberban"), texts.formatString("get.status", texts.getString("get.status.${config.isEnabled(Features.LOG_BAN)}"), config.memberBanLogChannel ?: texts.getString("get.not.set")), true)
                field(texts.getString("get.memeberunban"), texts.formatString("get.status", texts.getString("get.status.${config.isEnabled(Features.LOG_UNBAN)}"), config.memberUnbanLogChannel ?: texts.getString("get.not.set")), true)
                field(texts.getString("get.membermute"), texts.formatString("get.status", texts.getString("get.status.${config.isEnabled(Features.LOG_MUTE)}"), config.memberMuteLogChannel ?: texts.getString("get.not.set")), true)
                field(texts.getString("get.memberunmute"), texts.formatString("get.status", texts.getString("get.status.${config.isEnabled(Features.LOG_UNMUTE)}"), config.memberUnmuteLogChannel ?: texts.getString("get.not.set")), true)
            }

        if (message.words.size < 3) return texts.getErrorString().toMessage()

        return when(message.words[1].toLowerCase()) {
            "memberjoin" -> process(bot, guild.guildId, message.words[2], LogConfigs.memberJoinLogChannel, texts)
            "memberleave" -> process(bot, guild.guildId, message.words[2], LogConfigs.memberLeaveLogChannel, texts)
            "memberban" -> process(bot, guild.guildId, message.words[2], LogConfigs.memberBanLogChannel, texts)
            "memberunban" -> process(bot, guild.guildId, message.words[2], LogConfigs.memberUnbanLogChannel, texts)
            "membermute" -> process(bot, guild.guildId, message.words[2], LogConfigs.memberMuteLogChannel, texts)
            "memberunmute" -> process(bot, guild.guildId, message.words[2], LogConfigs.memberUnmuteLogChannel, texts)
//            "messagedelete" -> process(bot, guild.guildId, message.words[2], LogConfigs.messageDeleteLogChannel, texts)
//            "messagebulkdelete" -> process(bot, guild.guildId, message.words[2], LogConfigs.messageBulkDeleteLogChannel, texts)
//            "messagechange" -> process(bot, guild.guildId, message.words[2], LogConfigs.messageChangedLogChannel, texts)
            "messagedelete" -> TODO("No messages cache")
            "messagebulkdelete" -> TODO("No messages cache")
            "messagechange" -> TODO("No messages cache")

            else -> texts.getErrorString().toMessage()
        }
    }

    private fun extractChannelId(string: String): String {
        if (string.startsWith('#')) return string.drop(1)
        if (string.startsWith('<')) return string.drop(2).dropLast(1)
        throw IllegalArgumentException()
    }

    private fun process(bot: Bot, gid: String, channelId: String, column: Column<String>, texts: CommandLocaleBundle): CombinedMessageEmbed {
        val channel = channelId.takeUnless { it.equals("disable", true) }?.run { bot.clientStore.channels[extractChannelId(this)] }

        Db.updateLogConfig(gid) {
            it[column] = channel?.channelId ?: ""
        }
        if (channel == null) return texts.getString("${column.name}.disabled").toMessage()
        return texts.formatString(column.name, "<#${channel.channelId}>").toMessage()
    }
}