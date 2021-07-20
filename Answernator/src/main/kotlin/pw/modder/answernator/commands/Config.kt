package pw.modder.answernator.commands

import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import org.jetbrains.exposed.sql.transactions.transaction
import pw.modder.answernator.db.Db
import pw.modder.answernator.db.guild.BlacklistedCommand
import pw.modder.answernator.db.guild.Config
import pw.modder.answernator.db.guild.Features
import pw.modder.answernator.utils.*
import pw.modder.answernator.utils.extensions.kord.reply
import pw.modder.answernator.utils.extensions.kord.replyEmbed
import pw.modder.answernator.utils.extensions.toChannelMention
import pw.modder.answernator.utils.extensions.toRoleMention
import pw.modder.answernator.utils.locale.CommandLocaleBundle

class Config: LocalizedGuildCommand {
    override val name = "config"

    override val userGroup = Command.UserGroup.ADMIN
    override val cmdType = Command.CommandGroup.ADMIN

    override suspend fun action(message: Message, args: List<String>, guild: Guild, texts: CommandLocaleBundle, config: Config) {
        if (args.isEmpty()) {
            message.reply(texts.getErrorString())
            return
        }

        when(args.first()) {
            "get", "show" -> message.replyEmbed {
                title = guild.name
                description = texts.getString("description")

                field(texts.getString("lang"), true) { "`${config.lang}`" }
                field(texts.getString("prefix"), true) { "`${config.cmdPrefix}`" }
                field(texts.getString("lang.available"), true) {
                    Globals.config.langs.joinToString(separator = ", ") { "`$it`" }
                        .ifEmpty { texts.getString("lang.none") }
                }

                field(texts.getString("greeter"), false) {
                    texts.formatString(
                        "greeter.text",
                        texts.getString("greeter.${config.isEnabled(Features.GREETING)}"),
                        config.greetingChannel?.toChannelMention()
                            ?: texts.getString("channel.notset"),
                        config.greeting.format("%user%", "%guild%")
                    )
                }

                field(texts.getString("mute"), false) {
                    texts.formatString("mute.text",
                        config.muteRole?.toRoleMention() ?: texts.getString("role.notset"),
                        texts.getString("mute.${config.isEnabled(Features.MUTE_RANDOM_REASON)}")
                    )
                }

                field(texts.getString("defrole"), false) {
                    texts.formatString(
                        "defrole.text",
                        texts.getString("defrole.${config.isEnabled(Features.DEFAULT_ROLE)}"),
                        config.defaultRole?.toRoleMention()
                            ?: texts.getString("role.notset")
                    )
                }

                field(texts.getString("blacklist"), false) {
                    transaction {
                        Db.getBlacklisted(guild.id).joinToString(separator = ", ") { "`$it`" }
                    }.ifEmpty { texts.getString("blacklist.none") }
                }

                field(texts.getString("antispam"), false) {
                    texts.formatString(
                        "antispam.text",
                        texts.getString("antispam.${config.isEnabled(Features.ANTI_SPAM)}"),
                        texts.getString("antispam.${config.isEnabled(Features.ANTI_SPAM_SILENT)}"),
                        config.antiSpamWarn,
                        config.antiSpamBan
                    )
                }
            }
            "lang" -> {
                if (args.getOrNull(1) !in Globals.config.langs) {
                    message.reply(texts.formatString("locale.notfound", args[1]))
                    return
                }

                transaction { config.lang = args[1] }
                message.reply(texts.formatString("locale.updated", args[1]))

            }
            "prefix" -> {
                if (args.getOrNull(1)?.length != 1) {
                    message.reply(texts.formatString("prefix.incorrect", args[1]))
                    return
                }

                transaction { config.cmdPrefix = args[1].first() }
                message.reply(texts.formatString("prefix.updated", args[1]))
            }
            "greeting", "greet" -> {
                when(args.getOrNull(1)) {
                    "set" -> {
                        transaction {
                            config.greeting = args.drop(2)
                                .joinToString(separator = " ")
                                .replace("%user%", "%1\$s")
                                .replace("%guild%", "%2\$s")
                        }
                        message.reply(texts.getString("greeting.set"))
                    }
                    "enable" -> {
                        transaction {
                            config.enable(Features.GREETING)
                        }
                        message.reply(texts.getString("greeting.enabled"))
                    }
                    "disable" -> {
                        transaction {
                            config.disable(Features.GREETING)
                        }
                        message.reply(texts.getString("greeting.disabled"))
                    }
                    "channel" -> {
                        val channels = message.data.mentionedChannels.value
                        if (channels?.size != 1) {
                            message.reply(texts.getString("greeting.channel.none"))
                            return
                        }

                        transaction { config.greetingChannel = channels.first().asString }
                        message.reply(texts.formatString("greeting.channel.set", channels.first().asString.toChannelMention()))
                    }
                    else -> message.reply(texts.getErrorString())
                }
            }
            "defrole" -> {
                when(args.getOrNull(1)) {
                    "enable" -> {
                        if (config.defaultRole == null) {
                            message.reply(texts.getString("defrole.missing"))
                            return
                        }

                        transaction {
                            config.enable(Features.DEFAULT_ROLE)
                        }
                        message.reply(texts.getString("defrole.enabled"))
                    }
                    "disable" -> {
                        transaction {
                            config.disable(Features.DEFAULT_ROLE)
                        }
                        message.reply(texts.getString("defrole.disabled"))
                    }
                    "set" -> {
                        if (message.data.mentionRoles.size != 1) {
                            message.reply(texts.getString("defrole.invalid"))
                            return
                        }

                        transaction { config.defaultRole = message.data.mentionRoles.first().asString }
                        message.reply(texts.formatString("defrole.set", message.data.mentionRoles.first().asString.toRoleMention()))
                    }
                    "unset" -> {
                        transaction {
                            config.disable(Features.DEFAULT_ROLE)
                            config.defaultRole = null
                        }
                        message.reply(texts.getString("defrole.unset"))
                    }
                    else -> message.reply(texts.getErrorString())
                }
            }
            "mute", "muterole" -> {
                when(args.getOrNull(1)) {
                    "set", "setrole", "role" -> {
                        if (message.data.mentionRoles.size != 1) {
                            message.reply(texts.getString("muterole.invalid"))
                            return
                        }

                        transaction { config.muteRole = message.data.mentionRoles.first().asString }
                        message.reply(texts.formatString("muterole.set", message.data.mentionRoles.first().asString.toRoleMention()))
                    }
                    "unset" -> {
                        transaction {
                            config.muteRole = null
                        }
                        message.reply(texts.getString("muterole.unset"))
                    }
                    "reasons", "random", "randomreasons" -> {
                        when(args.getOrNull(2)) {
                            "random", "enable" -> {
                                config.enable(Features.MUTE_RANDOM_REASON)
                                message.reply(texts.getString("mute.reason.enabled"))
                            }
                            "manual", "disable" -> {
                                config.disable(Features.MUTE_RANDOM_REASON)
                                message.reply(texts.getString("mute.reason.disabled"))
                            }
                            else -> message.reply(texts.getErrorString())
                        }
                    }
                    else -> message.reply(texts.getErrorString())
                }
            }
            "blacklist" -> {
                when(args.getOrNull(1)) {
                    "show", "get" -> message.replyEmbed {
                        title = texts.getString("blacklist.title")
                        description = Db.getBlacklisted(guild.id).joinToString(separator = ", ") { "`$it`" }
                    }
                    "add" -> {
                        val cmd = args.getOrNull(2)
                        if (cmd == null) {
                            message.reply(texts.getString("blacklist.nocommand"))
                            return
                        }

                        if (Db.isBlackListed(guild.id, cmd)) {
                            message.reply(texts.getString("blacklist.add.already"))
                            return
                        }

                        if (CommandList.findCommand(cmd) == null) {
                            message.reply(texts.getString("blacklist.add.notfound"))
                            return
                        }

                        transaction { BlacklistedCommand.new {
                            this.command = cmd.lowercase()
                            this.guild = guild.id.asString
                        } }

                        message.reply(texts.formatString("blacklist.add", cmd))
                    }
                    "remove", "rm", "delete" -> {
                        val cmd = args.getOrNull(2)
                        if (cmd == null) {
                            message.reply(texts.getString("blacklist.nocommand"))
                            return
                        }

                        val blacklistedCommand = Db.getBlackListedCommand(guild.id, cmd)
                        if (blacklistedCommand == null) {
                            message.reply(texts.getString("blacklist.remove.already"))
                            return
                        }

                        transaction { blacklistedCommand.delete() }
                        message.reply(texts.formatString("blacklist.remove", cmd))
                    }
                    else -> message.reply(texts.getErrorString())
                }
            }
            else -> message.reply(texts.getErrorString())
        }
    }
}