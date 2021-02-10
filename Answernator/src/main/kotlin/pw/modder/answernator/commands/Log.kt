package pw.modder.answernator.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.util.words
import org.jetbrains.exposed.sql.Column
import pw.modder.answernator.db.Db
import pw.modder.answernator.db.LogConfigs
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.LocalizedCommand
import pw.modder.answernator.utils.extensions.toChannelMention
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

        if (message.words.getOrNull(1)?.toLowerCase() == "get") {
            val log = Db.logs.get(guild.guildId)
            return dslmessage {
                title = texts.getString("get.title")

                field(texts.getString("get.memberjoin"), log.memberJoinLogChannel.toChannelMention().ifEmpty { texts.getString("get.disabled") }, true)
                field(texts.getString("get.memberleave"), log.memberLeaveLogChannel.toChannelMention().ifEmpty { texts.getString("get.disabled") }, true)
                field(texts.getString("get.memberban"), log.memberBanLogChannel.toChannelMention().ifEmpty { texts.getString("get.disabled") }, true)
                field(texts.getString("get.memeberunban"), log.memberUnbanLogChannel.toChannelMention().ifEmpty { texts.getString("get.disabled") }, true)
                field(texts.getString("get.membermute"), log.memberMuteLogChannel.toChannelMention().ifEmpty { texts.getString("get.disabled") }, true)
                field(texts.getString("get.memberunmute"), log.memberUnmuteLogChannel.toChannelMention().ifEmpty { texts.getString("get.disabled") }, true)
            }
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