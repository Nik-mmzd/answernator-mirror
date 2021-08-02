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
            message.reply(texts.error())
            return
        }

        when(args.first()) {
            "get", "show" -> message.replyEmbed {
                title = guild.name
                description = texts["description"]

                field(texts["lang"], true) { "`${config.lang}`" }
                field(texts["prefix"], true) { "`${config.cmdPrefix}`" }
                field(texts["lang.available"], true) {
                    Globals.config.langs.joinToString(separator = ", ") { "`$it`" }
                        .ifEmpty { texts["lang.none"] }
                }

                field(texts["greeter"], false) {
                    texts["greeter.text"].format(
                        texts["greeter.${config.isEnabled(Features.GREETING)}"],
                        config.greetingChannel?.toChannelMention()
                            ?: texts["channel.notset"],
                        config.greeting.format("%user%", "%guild%")
                    )
                }

                field(texts["mute"], false) {
                    texts["mute.text"].format(
                        config.muteRole?.toRoleMention() ?: texts["role.notset"],
                        texts["mute.${config.isEnabled(Features.MUTE_RANDOM_REASON)}"]
                    )
                }

                field(texts["defrole"], false) {
                    texts["defrole.text"].format(
                        texts.get("defrole.${config.isEnabled(Features.DEFAULT_ROLE)}"),
                        config.defaultRole?.toRoleMention()
                            ?: texts["role.notset"]
                    )
                }

                field(texts["blacklist"], false) {
                    transaction {
                        Db.getBlacklisted(guild.id).joinToString(separator = ", ") { "`$it`" }
                    }.ifEmpty { texts["blacklist.none"] }
                }

                field(texts["antispam"], false) {
                    texts["antispam.text"].format(
                        texts["antispam.${config.isEnabled(Features.ANTI_SPAM)}"],
                        texts["antispam.${config.isEnabled(Features.ANTI_SPAM_SILENT)}"],
                        config.antiSpamWarn,
                        config.antiSpamBan
                    )
                }
            }
            "lang" -> {
                if (args.getOrNull(1) !in Globals.config.langs) {
                    message.reply(texts["locale.notfound"].format(args[1]))
                    return
                }

                transaction { config.lang = args[1] }
                message.reply(texts["locale.updated"].format(args[1]))

            }
            "prefix" -> {
                if (args.getOrNull(1)?.length != 1) {
                    message.reply(texts["prefix.incorrect"].format(args[1]))
                    return
                }

                transaction { config.cmdPrefix = args[1].first() }
                message.reply(texts["prefix.updated"].format(args[1]))
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
                        message.reply(texts["greeting.set"])
                    }
                    "enable" -> {
                        transaction {
                            config.enable(Features.GREETING)
                        }
                        message.reply(texts["greeting.enabled"])
                    }
                    "disable" -> {
                        transaction {
                            config.disable(Features.GREETING)
                        }
                        message.reply(texts["greeting.disabled"])
                    }
                    "channel" -> {
                        val channels = message.data.mentionedChannels.value
                        if (channels?.size != 1) {
                            message.reply(texts["greeting.channel.none"])
                            return
                        }

                        transaction { config.greetingChannel = channels.first().asString }
                        message.reply(texts["greeting.channel.set"].format(channels.first().asString.toChannelMention()))
                    }
                    else -> message.reply(texts.error())
                }
            }
            "defrole" -> {
                when(args.getOrNull(1)) {
                    "enable" -> {
                        if (config.defaultRole == null) {
                            message.reply(texts["defrole.missing"])
                            return
                        }

                        transaction {
                            config.enable(Features.DEFAULT_ROLE)
                        }
                        message.reply(texts["defrole.enabled"])
                    }
                    "disable" -> {
                        transaction {
                            config.disable(Features.DEFAULT_ROLE)
                        }
                        message.reply(texts["defrole.disabled"])
                    }
                    "set" -> {
                        if (message.data.mentionRoles.size != 1) {
                            message.reply(texts["defrole.invalid"])
                            return
                        }

                        transaction { config.defaultRole = message.data.mentionRoles.first().asString }
                        message.reply(texts["defrole.set"].format(message.data.mentionRoles.first().asString.toRoleMention()))
                    }
                    "unset" -> {
                        transaction {
                            config.disable(Features.DEFAULT_ROLE)
                            config.defaultRole = null
                        }
                        message.reply(texts["defrole.unset"])
                    }
                    else -> message.reply(texts.error())
                }
            }
            "mute", "muterole" -> {
                when(args.getOrNull(1)) {
                    "set", "setrole", "role" -> {
                        if (message.data.mentionRoles.size != 1) {
                            message.reply(texts["muterole.invalid"])
                            return
                        }

                        transaction { config.muteRole = message.data.mentionRoles.first().asString }
                        message.reply(texts["muterole.set"].format(message.data.mentionRoles.first().asString.toRoleMention()))
                    }
                    "unset" -> {
                        transaction {
                            config.muteRole = null
                        }
                        message.reply(texts["muterole.unset"])
                    }
                    "reasons", "random", "randomreasons" -> {
                        when(args.getOrNull(2)) {
                            "random", "enable" -> {
                                config.enable(Features.MUTE_RANDOM_REASON)
                                message.reply(texts["mute.reason.enabled"])
                            }
                            "manual", "disable" -> {
                                config.disable(Features.MUTE_RANDOM_REASON)
                                message.reply(texts["mute.reason.disabled"])
                            }
                            else -> message.reply(texts.error())
                        }
                    }
                    else -> message.reply(texts.error())
                }
            }
            "blacklist" -> {
                when(args.getOrNull(1)) {
                    "show", "get" -> message.replyEmbed {
                        title = texts["blacklist.title"]
                        description = Db.getBlacklisted(guild.id).joinToString(separator = ", ") { "`$it`" }
                    }
                    "add" -> {
                        val cmd = args.getOrNull(2)
                        if (cmd == null) {
                            message.reply(texts["blacklist.nocommand"])
                            return
                        }

                        if (Db.isBlackListed(guild.id, cmd)) {
                            message.reply(texts["blacklist.add.already"])
                            return
                        }

                        if (CommandList.findCommand(cmd) == null) {
                            message.reply(texts["blacklist.add.notfound"])
                            return
                        }

                        transaction { BlacklistedCommand.new {
                            this.command = cmd.lowercase()
                            this.guild = guild.id.asString
                        } }

                        message.reply(texts["blacklist.add"].format(cmd))
                    }
                    "remove", "rm", "delete" -> {
                        val cmd = args.getOrNull(2)
                        if (cmd == null) {
                            message.reply(texts["blacklist.nocommand"])
                            return
                        }

                        val blacklistedCommand = Db.getBlackListedCommand(guild.id, cmd)
                        if (blacklistedCommand == null) {
                            message.reply(texts["blacklist.remove.already"])
                            return
                        }

                        transaction { blacklistedCommand.delete() }
                        message.reply(texts["blacklist.remove"].format(cmd))
                    }
                    else -> message.reply(texts.error())
                }
            }
            else -> message.reply(texts.error())
        }
    }
}