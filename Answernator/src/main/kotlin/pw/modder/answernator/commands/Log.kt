package pw.modder.answernator.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.util.words
import kotlinx.serialization.UnstableDefault
import org.jetbrains.exposed.sql.Column
import pw.modder.answernator.db.Db
import pw.modder.answernator.db.LogConfigs
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.LocalizedCommand
import com.jessecorbett.diskord.dsl.message as dslmessage
import java.util.*

/*     var memberJoinLogChannel by LogConfigs.memberJoinLogChannel
    var memberLeaveLogChannel by LogConfigs.memberLeaveLogChannel
    var memberBanLogChannel by LogConfigs.memberBanLogChannel
    var memberUnbanLogChannel by LogConfigs.memberUnbanLogChannel
    var memberMuteLogChannel by LogConfigs.memberMuteLogChannel
    var memberUnmuteLogChannel by LogConfigs.memberUnmuteLogChannel

    var messageDeleteLogChannel by LogConfigs.messageDeleteLogChannel
    var messageBulkDeleteLogChannel by LogConfigs.messageBulkDeleteLogChannel
    var messageChangedLogChannel by LogConfigs.messageChangedLogChannel*/


@UnstableDefault
class Log: LocalizedCommand {
    override val name = "log"
    override val userGroup = Command.UserGroup.ADMIN
    override val channels: EnumSet<Command.ChannelTypes> = EnumSet.of(Command.ChannelTypes.GUILD)

    override suspend fun action(bot: Bot, message: Message, texts: ResourceBundle): CombinedMessageEmbed {

        val guild = message.guildId?.run { bot.clientStore.guilds[this] }
            ?: return texts.errorMessage()

        if (message.words.getOrNull(1)?.toLowerCase() == "get") {
            val log = Db.logs.get(guild.guildId)
            return dslmessage {
                title = texts.getStringOrKey("get.title")

                field(texts.getStringOrKey("get.memberjoin"), log.memberJoinLogChannel.toChannelMention().ifEmpty { texts.getStringOrKey("get.disabled") }, true)
                field(texts.getStringOrKey("get.memberleave"), log.memberLeaveLogChannel.toChannelMention().ifEmpty { texts.getStringOrKey("get.disabled") }, true)
                field(texts.getStringOrKey("get.memberban"), log.memberBanLogChannel.toChannelMention().ifEmpty { texts.getStringOrKey("get.disabled") }, true)
                field(texts.getStringOrKey("get.memeberunban"), log.memberUnbanLogChannel.toChannelMention().ifEmpty { texts.getStringOrKey("get.disabled") }, true)
                field(texts.getStringOrKey("get.membermute"), log.memberMuteLogChannel.toChannelMention().ifEmpty { texts.getStringOrKey("get.disabled") }, true)
                field(texts.getStringOrKey("get.memberunmute"), log.memberUnmuteLogChannel.toChannelMention().ifEmpty { texts.getStringOrKey("get.disabled") }, true)
            }
        }
        if (message.words.size < 3) return texts.errorMessage()

        return when(message.words[1].toLowerCase()) {
            "memberjoin" -> process(bot, guild.guildId, message.words[2], LogConfigs.memberJoinLogChannel, texts)
            "memberleave" -> process(bot, guild.guildId, message.words[2], LogConfigs.memberLeaveLogChannel, texts)
            "memberban" -> process(bot, guild.guildId, message.words[2], LogConfigs.memberBanLogChannel, texts)
            "memeberunban" -> process(bot, guild.guildId, message.words[2], LogConfigs.memberUnbanLogChannel, texts)
            "membermute" -> process(bot, guild.guildId, message.words[2], LogConfigs.memberMuteLogChannel, texts)
            "memberunmute" -> process(bot, guild.guildId, message.words[2], LogConfigs.memberUnmuteLogChannel, texts)
//            "messagedelete" -> process(bot, guild.guildId, message.words[2], LogConfigs.messageDeleteLogChannel, texts)
//            "messagebulkdelete" -> process(bot, guild.guildId, message.words[2], LogConfigs.messageBulkDeleteLogChannel, texts)
//            "messagechange" -> process(bot, guild.guildId, message.words[2], LogConfigs.messageChangedLogChannel, texts)
            "messagedelete" -> TODO("No messages cache")
            "messagebulkdelete" -> TODO("No messages cache")
            "messagechange" -> TODO("No messages cache")

            else -> texts.errorMessage()
        }
    }

    private fun extractChannelId(string: String): String {
        if (string.startsWith('#')) return string.drop(1)
        if (string.startsWith('<')) return string.drop(2).dropLast(1)
        throw IllegalArgumentException()
    }

    private fun process(bot: Bot, gid: String, channelId: String, column: Column<String>, texts: ResourceBundle): CombinedMessageEmbed {
        val channel = channelId.takeUnless { it.equals("disable", true) }?.run { bot.clientStore.channels[extractChannelId(this)] }

        Db.updateLogConfig(gid) {
            it[column] = channel?.channelId ?: ""
        }
        if (channel == null) return texts.message("${column.name}.disabled")
        return texts.message(column.name, "<#${channel.channelId}>")
    }

    private fun String.toChannelMention(): String {
        if (isEmpty()) return this
        return "<#$this>"
    }
}