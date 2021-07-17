package pw.modder.answernator.commands

import dev.kord.core.behavior.reply
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import org.jetbrains.exposed.sql.transactions.transaction
import pw.modder.answernator.db.Db
import pw.modder.answernator.db.guild.Features
import pw.modder.answernator.db.guild.LogConfig
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.LocalizedGuildCommand
import pw.modder.answernator.utils.extensions.isChannelMention
import pw.modder.answernator.utils.extensions.kord.reply
import pw.modder.answernator.utils.extensions.toChannelMention
import pw.modder.answernator.utils.locale.CommandLocaleBundle
import java.util.*

class Log: LocalizedGuildCommand {
    override val name = "log"
    override val userGroup = Command.UserGroup.ADMIN
    override val cmdType = Command.CommandGroup.ADMIN
    override val channels: EnumSet<Command.ChannelTypes> = EnumSet.of(Command.ChannelTypes.GUILD)

    override suspend fun action(message: Message, args: List<String>, guild: Guild, texts: CommandLocaleBundle) {
        if (args.isEmpty()) {
            message.reply(texts.getErrorString())
            return
        }

        val config = Db.getLogConfig(guild.id)

        if (args.first().equals("get", true)) {
            message.reply {
                embed {
                    title = texts.getString("get.title")

                    field(texts.getString("get.memberjoin"), true) { formatField(texts, config, Features.LOG_JOIN, config.memberJoinLogChannel) }
                    field(texts.getString("get.memberleave"), true) { formatField(texts, config, Features.LOG_LEAVE, config.memberLeaveLogChannel) }
                    field(texts.getString("get.memberban"), true) { formatField(texts, config, Features.LOG_LEAVE, config.memberLeaveLogChannel) }
                    field(texts.getString("get.memeberunban"), true) { formatField(texts, config, Features.LOG_UNBAN, config.memberUnbanLogChannel) }
                    field(texts.getString("get.membermute"), true) { formatField(texts, config, Features.LOG_MUTE, config.memberMuteLogChannel) }
                    field(texts.getString("get.memberunmute"), true) { formatField(texts, config, Features.LOG_UNMUTE, config.memberUnmuteLogChannel) }
                }
                allowedMentions { repliedUser = false }
            }
            return
        }

        if (args.size == 1) {
            message.reply(texts.getErrorString())
            return
        }

        val action = when {
            args[1].equals("enable", true) -> "enable"
            args[1].equals("disable", true) -> "disable"
            message.mentionedChannelIds.size == 1 && args[1].isChannelMention() -> message.mentionedChannelIds.single().asString
            else -> {
                message.reply(texts.getErrorString())
                return
            }
        }

        message.reply(when(args.first().lowercase()) {
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
                    texts.getString("enable.all")
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
                    texts.getString("disable.all")
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
                    texts.formatString("channel.all", action.toChannelMention())
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

            else -> texts.getErrorString()
        })
    }

    private fun formatField(texts: CommandLocaleBundle, config: LogConfig, feature: Features, channel: String?): String {
        return texts.formatString("get.status", texts.getString("get.status.${config.isEnabled(feature)}"), channel?.toChannelMention() ?: texts.getString("get.not.set"))
    }

    private fun process(action: String, name: String, config: LogConfig, texts: CommandLocaleBundle, feature: Features, block: LogConfig.() -> Unit): String {
        when(action) {
            "enable" -> {
                transaction {
                    config.enable(feature)
                }
                return texts.getString("enable.$name")
            }
            "disable" -> {
                transaction {
                    config.disable(feature)
                }
                return texts.getString("disable.$name")
            }
            else -> {
                transaction {
                    config.block()
                }
                return texts.formatString("channel.$name", action.toChannelMention())
            }
        }
    }
}