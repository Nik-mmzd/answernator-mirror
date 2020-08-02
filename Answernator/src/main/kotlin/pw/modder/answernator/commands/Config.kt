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
import java.util.*
import com.jessecorbett.diskord.dsl.message as dslmessage

class Config: LocalizedCommand {
    override val name = "config"

    override val channels = EnumSet.of(Command.ChannelTypes.GUILD)
    override val userGroup = Command.UserGroup.ADMIN
    override val cmdType = Command.CommandGroup.ADMIN

    override suspend fun action(bot: Bot, message: Message, texts: ResourceBundle): CombinedMessageEmbed {
        val guildClient = message.guildId?.run { bot.clientStore.guilds[this] } ?: return texts.message("noguild")
        val guild = guildClient.get()
        val config = Db.guilds.get(guild.id)

        return when(message.words.getOrNull(1)?.toLowerCase()) {
            "get" -> {
                dslmessage {
                    title = guild.name
                    description = texts.getStringOrKey("description")

                    field(texts.getStringOrKey("lang"), config.lang, false)
                    field(texts.getStringOrKey("lang.available"), Globals.config.langs.joinToString(separator = ", ") { "`$it`" }, false)
                    field(texts.getStringOrKey("greeter"), texts.getStringOrKey("greeter.${config.greetNewUsers}"), false)
                    field(texts.getStringOrKey("greeting"), String.format(config.greetingText, "%user%", "%guild%"), false)
                    field(texts.getStringOrKey("greeting.help"), texts.getStringOrKey("greeting.help.value"), false)
                    field(texts.getStringOrKey("greeting.channel"), getGreetingsChannelName(bot.clientStore, config, texts), false)
                    field(texts.getStringOrKey("muterole"), config.muteRole.toRoleMention().ifEmpty { texts.getStringOrKey("role.notset") }, true)
                    field(texts.getStringOrKey("defrole"), config.defaultRole.toRoleMention().ifEmpty { texts.getStringOrKey("role.notset") }, true)
                }
            }
            "set" -> {
                when(message.words.getOrNull(2)) {
                    "lang" -> {
                        if (message.words[3] in Globals.config.langs) {
                            Db.updateGuildConfig(guild.id) {
                                it[lang] = message.words[3]
                            }
                            texts.message("locale.updated", message.words[3])
                        } else texts.message("locale.notfound", message.words[3])
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
                                texts.message("greeting.applied")
                            }
                            "enable" -> {
                                Db.updateGuildConfig(guild.id) {
                                    it[greetNewUsers] = true
                                }
                                texts.message("greeting.enabled")
                            }
                            "disable" -> {
                                Db.updateGuildConfig(guild.id) {
                                    it[greetNewUsers] = false
                                }
                                texts.message("greeting.disabled")
                            }
                            "channel" -> {
                                val channel = try {
                                    bot.clientStore.channels[extractChannelId(message.words[4])]
                                } catch (_: Exception) {
                                    return texts.errorMessage()
                                }

                                Db.updateGuildConfig(guild.id) {
                                    it[greetingsChannel] = channel.channelId
                                }
                                texts.message("greeting.channelset", channel.get().mention)
                            }
                            else -> texts.errorMessage()
                        }
                    }
                    "defrole" -> {
                        if (message.words.getOrNull(3).equals("remove", true)) {
                            Db.updateGuildConfig(guild.id) {
                                it[defaultRole] = ""
                            }
                            return texts.message("defrole.removed")
                        }

                        val role = message.rolesIdsMentioned.singleOrNull()
                            ?: return texts.errorMessage()

                        Db.updateGuildConfig(guild.id) {
                            it[defaultRole] = role
                        }
                        return texts.message("defrole.set", role.toRoleMention())
                    }
                    "muterole" -> {
                        if (message.words.getOrNull(3).equals("remove", true)) {
                            Db.updateGuildConfig(guild.id) {
                                it[muteRole] = ""
                            }
                            return texts.message("muterole.removed")
                        }

                        val role = message.rolesIdsMentioned.singleOrNull()
                            ?: return texts.errorMessage()

                        Db.updateGuildConfig(guild.id) {
                            it[muteRole] = role
                        }
                        return texts.message("muterole.set", role.toRoleMention())
                    }
                    else -> texts.errorMessage()
                }
            }
            "blacklist" -> TODO()
            else -> texts.errorMessage()

        }
    }

    private fun getGreetingsChannelName(clientStore: ClientStore, config: GuildConfig, texts: ResourceBundle): String {
        if (config.greetingsChannel.isEmpty()) return texts.getStringOrKey("greeting.channel.notset")

        val channel = try {
            clientStore.channels[config.greetingsChannel]
        } catch (_: Exception) {
            return texts.getStringOrKey("greeting.channel.error")
        }

        return channel.channelId.toChannelMention()
    }

    private fun extractChannelId(string: String): String {
        if (string.startsWith('#')) return string.drop(1)
        if (string.startsWith('<')) return string.drop(2).dropLast(1)
        throw IllegalArgumentException()
    }
}