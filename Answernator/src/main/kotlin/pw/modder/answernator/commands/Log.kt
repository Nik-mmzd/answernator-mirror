package pw.modder.answernator.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.util.words
import org.jetbrains.exposed.sql.transactions.transaction
import pw.modder.answernator.db.Db
import pw.modder.answernator.db.guild.Features
import pw.modder.answernator.db.guild.LogConfig
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.LocalizedCommand
import pw.modder.answernator.utils.extensions.channelsIdsMentioned
import pw.modder.answernator.utils.extensions.isChannelMention
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
        val config = Db.getLogConfig(guild.guildId)

        if (message.words.size == 1) return texts.getErrorString().toMessage()

        if (message.words[1].equals("get", true))
            return dslmessage {
                title = texts.getString("get.title")

                field(texts.getString("get.memberjoin"), formatField(texts, config, Features.LOG_JOIN, config.memberJoinLogChannel), true)
                field(texts.getString("get.memberleave"), formatField(texts, config, Features.LOG_LEAVE, config.memberLeaveLogChannel), true)
                field(texts.getString("get.memberban"), formatField(texts, config, Features.LOG_BAN, config.memberBanLogChannel), true)
                field(texts.getString("get.memeberunban"), formatField(texts, config, Features.LOG_UNBAN, config.memberUnbanLogChannel), true)
                field(texts.getString("get.membermute"), formatField(texts, config, Features.LOG_MUTE, config.memberMuteLogChannel), true)
                field(texts.getString("get.memberunmute"), formatField(texts, config, Features.LOG_UNMUTE, config.memberUnmuteLogChannel), true)
            }

        if (message.words.size < 3) return texts.getErrorString().toMessage()

        val action = when {
            message.words[2].equals("enable", true) -> "enable"
            message.words[2].equals("disable", true) -> "disable"
            message.channelsIdsMentioned.size == 1 && message.words[2].isChannelMention() -> message.channelsIdsMentioned.single()
            else -> return texts.getErrorString().toMessage()
        }

        return when(message.words[1].toLowerCase()) {
            "all" -> when(action) {
                "enable" -> {
                    transaction {
                        config.enable(Features.LOG_JOIN)
                        config.enable(Features.LOG_LEAVE)
                        config.enable(Features.LOG_BAN)
                        config.enable(Features.LOG_UNBAN)
                        config.enable(Features.LOG_MUTE)
                        config.enable(Features.LOG_UNMUTE)
                    }
                    return texts.getString("enable.all").toMessage()
                }
                "disable" -> {
                    transaction {
                        config.disable(Features.LOG_JOIN)
                        config.disable(Features.LOG_LEAVE)
                        config.disable(Features.LOG_BAN)
                        config.disable(Features.LOG_UNBAN)
                        config.disable(Features.LOG_MUTE)
                        config.disable(Features.LOG_UNMUTE)
                    }
                    return texts.getString("disable.all").toMessage()
                }
                else -> {
                    transaction {
                        config.memberJoinLogChannel = action
                        config.memberLeaveLogChannel = action
                        config.memberBanLogChannel = action
                        config.memberUnbanLogChannel = action
                        config.memberMuteLogChannel = action
                        config.memberUnmuteLogChannel = action
                    }
                    return texts.formatString("channel.all", action.toChannelMention()).toMessage()
                }
            }
            "join" -> process(action, "join", config, texts, Features.LOG_JOIN) {
                memberJoinLogChannel = action
            }
            "leave" -> process(action, "leave", config, texts, Features.LOG_LEAVE) {
                memberLeaveLogChannel = action
            }
            "ban" -> process(action, "ban", config, texts, Features.LOG_BAN) {
                memberBanLogChannel = action
            }
            "unban" -> process(action, "unban", config, texts, Features.LOG_UNBAN) {
                memberUnbanLogChannel = action
            }
            "mute" -> process(action, "mute", config, texts, Features.LOG_MUTE) {
                memberMuteLogChannel = action
            }
            "unmute" -> process(action, "unmute", config, texts, Features.LOG_UNMUTE) {
                memberUnmuteLogChannel = action
            }

            else -> texts.getErrorString().toMessage()
        }
    }

    private fun formatField(texts: CommandLocaleBundle, config: LogConfig, feature: Features, channel: String?): String {
        return texts.formatString("get.status", texts.getString("get.status.${config.isEnabled(feature)}"), channel?.toChannelMention() ?: texts.getString("get.not.set"))
    }

    private fun process(action: String, name: String, config: LogConfig, texts: CommandLocaleBundle, feature: Features, block: LogConfig.() -> Unit): CombinedMessageEmbed {
        when(action) {
            "enable" -> {
                transaction {
                    config.enable(feature)
                }
                return texts.getString("enable.$name").toMessage()
            }
            "disable" -> {
                transaction {
                    config.disable(feature)
                }
                return texts.getString("disable.$name").toMessage()
            }
            else -> {
                transaction {
                    config.block()
                }
                return texts.formatString("channel.$name", action.toChannelMention()).toMessage()
            }
        }
    }
}