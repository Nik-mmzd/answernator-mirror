package pw.modder.answernator.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.util.ClientStore
import com.jessecorbett.diskord.util.mention
import com.jessecorbett.diskord.util.words
import pw.modder.answernator.db.Db
import pw.modder.answernator.db.GuildConfig
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.LocalizedCommand
import pw.modder.answernator.utils.extensions.toChannelMention
import pw.modder.answernator.utils.extensions.toRoleMention
import pw.modder.answernator.utils.locale.CommandLocaleBundle
import java.util.*
import com.jessecorbett.diskord.dsl.message as dslmessage

class Config: LocalizedCommand {
    override val name = "config"

    override val channels = EnumSet.of(Command.ChannelTypes.GUILD)
    override val userGroup = Command.UserGroup.ADMIN
    override val cmdType = Command.CommandGroup.ADMIN

    override suspend fun action(bot: Bot, message: Message, texts: CommandLocaleBundle): CombinedMessageEmbed {
        val guildClient = message.guildId?.run { bot.clientStore.guilds[this] } ?: return texts.getString("noguild").toMessage()
        val guild = guildClient.get()
        val config = Db.guilds.get(guild.id)

        return when(message.words.getOrNull(1)?.toLowerCase()) {
            "get" -> {
                dslmessage {
                    title = guild.name
                    description = texts.getString("description")

                    field(texts.getString("lang"), config.lang, false)
                    field(texts.getString("lang.available"), Globals.config.langs.joinToString(separator = ", ") { "`$it`" }, false)
                    field(texts.getString("greeter"), texts.getString("greeter.${config.greetNewUsers}"), false)
                    field(texts.getString("greeting"), String.format(config.greetingText, "%user%", "%guild%"), false)
                    field(texts.getString("greeting.help"), texts.getString("greeting.help.value"), false)
                    field(texts.getString("greeting.channel"), getGreetingsChannelName(bot.clientStore, config, texts), false)
                    field(texts.getString("muterole"), config.muteRole.toRoleMention().ifEmpty { texts.getString("role.notset") }, true)
                    field(texts.getString("defrole"), config.defaultRole.toRoleMention().ifEmpty { texts.getString("role.notset") }, true)
                }
            }
            "set" -> {
                when(message.words.getOrNull(2)) {
                    "lang" -> {
                        if (message.words[3] in Globals.config.langs) {
                            Db.updateGuildConfig(guild.id) {
                                it[lang] = message.words[3]
                            }
                            texts.formatString("locale.updated", message.words[3]).toMessage()
                        } else texts.formatString("locale.notfound", message.words[3]).toMessage()
                    }
                    "greet" -> {
                        when(message.words.getOrNull(3)) {
                            "set" -> {
                                Db.updateGuildConfig(guild.id) {
                                    it[greetingText] = message.words.drop(4)
                                        .joinToString(separator = " ")
                                        .replace("%user%", "%1\$s")
                                        .replace("%guild%", "%2\$s")
                                }
                                texts.getString("greeting.applied").toMessage()
                            }
                            "enable" -> {
                                Db.updateGuildConfig(guild.id) {
                                    it[greetNewUsers] = true
                                }
                                texts.getString("greeting.enabled").toMessage()
                            }
                            "disable" -> {
                                Db.updateGuildConfig(guild.id) {
                                    it[greetNewUsers] = false
                                }
                                texts.getString("greeting.disabled").toMessage()
                            }
                            "channel" -> {
                                val channel = try {
                                    bot.clientStore.channels[extractChannelId(message.words[4])]
                                } catch (_: Exception) {
                                    return texts.getErrorString().toMessage()
                                }

                                Db.updateGuildConfig(guild.id) {
                                    it[greetingsChannel] = channel.channelId
                                }
                                texts.formatString("greeting.channelset", channel.get().mention).toMessage()
                            }
                            else -> texts.getErrorString().toMessage()
                        }
                    }
                    "defrole" -> {
                        if (message.words.getOrNull(3).equals("remove", true)) {
                            Db.updateGuildConfig(guild.id) {
                                it[defaultRole] = ""
                            }
                            return texts.getString("defrole.removed").toMessage()
                        }

                        val role = message.rolesIdsMentioned.singleOrNull()
                            ?: return texts.getErrorString().toMessage()

                        Db.updateGuildConfig(guild.id) {
                            it[defaultRole] = role
                        }
                        return texts.formatString("defrole.set", role.toRoleMention()).toMessage()
                    }
                    "muterole" -> {
                        if (message.words.getOrNull(3).equals("remove", true)) {
                            Db.updateGuildConfig(guild.id) {
                                it[muteRole] = ""
                            }
                            return texts.getString("muterole.removed").toMessage()
                        }

                        val role = message.rolesIdsMentioned.singleOrNull()
                            ?: return texts.getErrorString().toMessage()

                        Db.updateGuildConfig(guild.id) {
                            it[muteRole] = role
                        }
                        return texts.formatString("muterole.set", role.toRoleMention()).toMessage()
                    }
                    else -> texts.getErrorString().toMessage()
                }
            }
            "blacklist" -> TODO()
            else -> texts.getErrorString().toMessage()

        }
    }

    private fun getGreetingsChannelName(clientStore: ClientStore, config: GuildConfig, texts: CommandLocaleBundle): String {
        if (config.greetingsChannel.isEmpty()) return texts.getString("greeting.channel.notset")

        val channel = try {
            clientStore.channels[config.greetingsChannel]
        } catch (_: Exception) {
            return texts.getString("greeting.channel.error")
        }

        return channel.channelId.toChannelMention()
    }

    private fun extractChannelId(string: String): String {
        if (string.startsWith('#')) return string.drop(1)
        if (string.startsWith('<')) return string.drop(2).dropLast(1)
        throw IllegalArgumentException()
    }
}