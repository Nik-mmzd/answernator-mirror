package pw.modder.answernator.commands

import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import org.jetbrains.exposed.sql.transactions.transaction
import pw.modder.answernator.db.guild.Config
import pw.modder.answernator.db.guild.Features
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.LocalizedGuildCommand
import pw.modder.answernator.utils.extensions.isChannelMention
import pw.modder.answernator.utils.extensions.kord.reply
import pw.modder.answernator.utils.extensions.kord.replyEmbed
import pw.modder.answernator.utils.extensions.toChannelMention
import pw.modder.answernator.utils.locale.CommandLocaleBundle
import java.util.*

class Log: LocalizedGuildCommand {
    override val name = "log"
    override val userGroup = Command.UserGroup.ADMIN
    override val cmdType = Command.CommandGroup.ADMIN
    override val channels: EnumSet<Command.ChannelTypes> = EnumSet.of(Command.ChannelTypes.GUILD)

    override suspend fun action(message: Message, args: List<String>, guild: Guild, texts: CommandLocaleBundle, config: Config) {
        if (args.isEmpty()) {
            message.reply(texts.error())
            return
        }

        if (args.first().equals("get", true)) {
            message.replyEmbed {
                title = texts["get.title"]

                field(texts["get.memberjoin"], true) { formatField(texts, config, Features.LOG_JOIN, config.memberJoinLogChannel) }
                field(texts["get.memberleave"], true) { formatField(texts, config, Features.LOG_LEAVE, config.memberLeaveLogChannel) }
                field(texts["get.memberban"], true) { formatField(texts, config, Features.LOG_LEAVE, config.memberBanLogChannel) }
                field(texts["get.memeberunban"], true) { formatField(texts, config, Features.LOG_UNBAN, config.memberUnbanLogChannel) }
                field(texts["get.membermute"], true) { formatField(texts, config, Features.LOG_MUTE, config.memberMuteLogChannel) }
                field(texts["get.memberunmute"], true) { formatField(texts, config, Features.LOG_UNMUTE, config.memberUnmuteLogChannel) }

            }
            return
        }

        if (args.size == 1) {
            message.reply(texts.error())
            return
        }

        val action = when {
            args[1].equals("enable", true) -> "enable"
            args[1].equals("disable", true) -> "disable"
            message.mentionedChannelIds.size == 1 && args[1].isChannelMention() -> message.mentionedChannelIds.single().asString
            else -> {
                message.reply(texts.error())
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
                    texts["enable.all"]
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
                    texts["disable.all"]
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
                    texts["channel.all"].format(action.toChannelMention())
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

            else -> texts.error()
        })
    }

    private fun formatField(texts: CommandLocaleBundle, config: Config, feature: Features, channel: String?): String {
        return texts["get.status"].format(texts["get.status.${config.isEnabled(feature)}"], channel?.toChannelMention() ?: texts["get.not.set"])
    }

    private fun process(action: String, name: String, config: Config, texts: CommandLocaleBundle, feature: Features, block: Config.() -> Unit): String {
        when(action) {
            "enable" -> {
                transaction {
                    config.enable(feature)
                }
                return texts["enable.$name"]
            }
            "disable" -> {
                transaction {
                    config.disable(feature)
                }
                return texts["disable.$name"]
            }
            else -> {
                transaction {
                    config.block()
                }
                return texts["channel.$name"].format(action.toChannelMention())
            }
        }
    }
}