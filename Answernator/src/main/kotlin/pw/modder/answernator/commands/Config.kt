package pw.modder.answernator.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.util.toRoleMention
import com.jessecorbett.diskord.util.words
import org.jetbrains.exposed.sql.transactions.transaction
import pw.modder.answernator.cache.GuildCache.getCached
import pw.modder.answernator.db.Db
import pw.modder.answernator.db.guild.BlacklistedCommand
import pw.modder.answernator.db.guild.Features
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.CommandList
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.LocalizedCommand
import pw.modder.answernator.utils.extensions.channelsIdsMentioned
import pw.modder.answernator.utils.extensions.toChannelMention
import pw.modder.answernator.utils.locale.CommandLocaleBundle
import java.util.*
import com.jessecorbett.diskord.dsl.message as dslmessage

class Config: LocalizedCommand {
    override val name = "config"

    override val channels = EnumSet.of(Command.ChannelTypes.GUILD)
    override val userGroup = Command.UserGroup.ADMIN
    override val cmdType = Command.CommandGroup.ADMIN

    override suspend fun action(bot: Bot, message: Message, texts: CommandLocaleBundle): CombinedMessageEmbed {
        val guildClient = bot.clientStore.guilds[message.guildId ?: return texts.getString("noguild").toMessage()]
        val guild = guildClient.getCached()
        val config = Db.getConfig(guild.id)

        if (message.words.size < 2)
            return texts.getErrorString().toMessage()

        return when(message.words[1].toLowerCase()) {
            "get", "show" -> dslmessage {
                    title = guild.name
                    description = texts.getString("description")

                    field(texts.getString("lang"), config.lang, true)
                    field(texts.getString("prefix"), config.cmdPrefix.toString(), true)
                    field(texts.getString("lang.available"), Globals.config.langs.joinToString(separator = ", ") { "`$it`" }.ifEmpty { texts.getString("lang.none") }, true)

                    field(texts.getString("greeter"), texts.formatString("greeter.text",
                        texts.getString("greeter.${config.isEnabled(Features.GREETING)}"),
                        config.greetingChannel?.toChannelMention() ?: texts.getString("channel.notset"),
                        config.greeting.format("%user%", "%guild%")
                    ), false)

                    field(texts.getString("muterole"), config.muteRole?.toRoleMention()?: texts.getString("role.notset"), false)

                    field(texts.getString("defrole"), texts.formatString("defrole.text",
                        texts.getString("defrole.${config.isEnabled(Features.DEFAULT_ROLE)}"),
                        config.defaultRole?.toRoleMention() ?: texts.getString("role.notset")
                    ), false)

                    field(texts.getString("blacklist"), transaction { config.blacklistedCommands.joinToString(separator = ", ") { "`${it.command}`" } }.ifEmpty { texts.getString("blacklist.none") }, false)

                    field(texts.getString("antispam"), texts.formatString("antispam.text",
                        texts.getString("antispam.${config.isEnabled(Features.ANTI_SPAM)}"),
                        texts.getString("antispam.${config.isEnabled(Features.ANTI_SPAM_SILENT)}"),
                        config.antiSpamWarn,
                        config.antiSpamBan
                    ), false)
            }
            "lang" -> {
                if (message.words.getOrNull(2) !in Globals.config.langs)
                    return texts.formatString("locale.notfound", message.words[2]).toMessage()

                transaction { config.lang = message.words[2] }
                return texts.formatString("locale.updated", message.words[2]).toMessage()

            }
            "prefix" -> {
                if (message.words.getOrNull(2)?.length != 1)
                    return texts.formatString("prefix.incorrect", message.words[2]).toMessage()

                transaction { config.cmdPrefix = message.words[2].toCharArray().first() }
                texts.formatString("prefix.updated", message.words[2]).toMessage()
            }
            "greeting", "greet" -> {
                when(message.words.getOrNull(2)) {
                    "set" -> {
                        transaction {
                            config.greeting = message.words.drop(3)
                                .joinToString(separator = " ")
                                .replace("%user%", "%1\$s")
                                .replace("%guild%", "%2\$s")
                        }
                        texts.getString("greeting.set").toMessage()
                    }
                    "enable" -> {
                        transaction {
                            config.enable(Features.GREETING)
                        }
                        texts.getString("greeting.enabled").toMessage()
                    }
                    "disable" -> {
                        transaction {
                            config.disable(Features.GREETING)
                        }
                        texts.getString("greeting.disabled").toMessage()
                    }
                    "channel" -> {
                        if (message.channelsIdsMentioned.size != 1)
                            return texts.getString("greeting.channel.none").toMessage()

                        transaction { config.greetingChannel = message.rolesIdsMentioned.first() }
                        texts.formatString("greeting.channel.set", message.rolesIdsMentioned.first().toRoleMention()).toMessage()
                    }
                    else -> texts.getErrorString().toMessage()
                }
            }
            "defrole" -> {
                when(message.words.getOrNull(2)) {
                    "enable" -> {
                        if (config.defaultRole == null)
                            return texts.getString("defrole.missing").toMessage()

                        transaction {
                            config.enable(Features.DEFAULT_ROLE)
                        }
                        texts.getString("defrole.enabled").toMessage()
                    }
                    "disable" -> {
                        transaction {
                            config.disable(Features.DEFAULT_ROLE)
                        }
                        texts.getString("defrole.disabled").toMessage()
                    }
                    "set" -> {
                        if (message.rolesIdsMentioned.size != 1)
                            return texts.getString("defrole.invalid").toMessage()

                        transaction { config.defaultRole = message.rolesIdsMentioned.first() }
                        texts.formatString("defrole.set", message.rolesIdsMentioned.first().toRoleMention()).toMessage()
                    }
                    "unset" -> {
                        transaction {
                            config.disable(Features.DEFAULT_ROLE)
                            config.defaultRole = null
                        }
                        texts.getString("defrole.unset").toMessage()
                    }
                    else -> texts.getErrorString().toMessage()
                }
            }
            "muterole" -> {
                when(message.words.getOrNull(2)) {
                    "set" -> {
                        if (message.rolesIdsMentioned.size != 1)
                            return texts.getString("muterole.invalid").toMessage()

                        transaction { config.muteRole = message.rolesIdsMentioned.first() }
                        texts.formatString("muterole.set", message.rolesIdsMentioned.first().toRoleMention()).toMessage()
                    }
                    "unset" -> {
                        transaction {
                            config.muteRole = null
                        }
                        texts.getString("muterole.unset").toMessage()
                    }
                    else -> texts.getErrorString().toMessage()
                }
            }
            "blacklist" -> {
                when(message.words.getOrNull(2)) {
                    "show", "get" -> dslmessage {
                        title = texts.getString("blacklist.title")
                        description = transaction { config.blacklistedCommands.joinToString(separator = ", ") { "`${it.command}`" } }
                    }
                    "add" -> {
                        val cmd = message.words.getOrNull(3)
                            ?: return texts.getString("blacklist.nocommand").toMessage()
                        if (transaction { config.blacklistedCommands.any { it.command.equals(cmd, true) }})
                            return texts.getString("blacklist.add.already").toMessage()
                        if (CommandList.findCommand(cmd) == null)
                            return texts.getString("blacklist.add.notfound").toMessage()

                        transaction { BlacklistedCommand.new {
                            this.command = cmd.toLowerCase()
                            this.guild = guild.id
                        } }
                        texts.formatString("blacklist.add", cmd).toMessage()
                    }
                    "remove", "rm", "delete" -> {
                        val cmd = message.words.getOrNull(3)
                            ?: return texts.getString("blacklist.nocommand").toMessage()
                        val blacklistedCommand = transaction { config.blacklistedCommands.find { it.command.equals(cmd, true) } }
                            ?: return texts.getString("blacklist.remove.already").toMessage()

                        transaction { blacklistedCommand.delete() }
                        texts.formatString("blacklist.remove", cmd).toMessage()
                    }
                    else -> texts.getErrorString().toMessage()
                }
            }

            else -> texts.getErrorString().toMessage()
        }
    }
}