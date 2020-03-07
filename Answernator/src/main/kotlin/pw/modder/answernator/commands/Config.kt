package pw.modder.answernator.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.util.ClientStore
import com.jessecorbett.diskord.util.mention
import com.jessecorbett.diskord.util.words
import kotlinx.serialization.UnstableDefault
import pw.modder.answernator.utils.*
import com.jessecorbett.diskord.dsl.message as dslmessage
import java.util.*

@UnstableDefault
class Config: LocalizedCommand {
    override val name = "config"

    override val channels = EnumSet.of(Command.ChannelTypes.GUILD)
    override val userGroup = Command.UserGroup.ADMIN

    override suspend fun action(clientStore: ClientStore, message: Message, texts: ResourceBundle): CombinedMessageEmbed {
        val guildClient = message.guildId?.run { clientStore.guilds[this] } ?: return textMessage(texts.getStringOrKey("noguild"))
        val guild = guildClient.get()
        val config = GuildConfigs.get(guild.id)
        var newConfig: GuildConfig? = null

        val answer = when(message.words.getOrNull(1)) {
            "get" -> {
                val chName= getGreetingsChannelName(clientStore, config, texts)
                dslmessage {
                    title = guild.name
                    description = texts.getStringOrKey("description")

                    field(texts.getStringOrKey("lang"), config.lang, false)
                    field(texts.getStringOrKey("lang.available"), GlobalConfig.get().langs.joinToString(separator = ", ") { "`$it`" }, false)
                    field(texts.getStringOrKey("greeter"), texts.getStringOrKey("greeter.${config.greetNewUsers}"), false)
                    field(texts.getStringOrKey("greeting"), String.format(config.greetingText, message.author.username, guild.name), false)
                    field(texts.getStringOrKey("greeting.help"), texts.getStringOrKey("greeting.help.value"), false)
                    field(texts.getStringOrKey("greeting.channel"), chName, false)
                }
            }
            "set" -> {
                when(message.words.getOrNull(2)) {
                    "lang" -> {
                        if (message.words[3] in GlobalConfig.get().langs) {
                            newConfig = config.copy(lang = message.words[3])
                            textMessage(texts.formatString("locale.updated", message.words[3]))
                        } else textMessage(texts.formatString("locale.notfound", message.words[3]))
                    }
                    "greet" -> {
                        when(message.words.getOrNull(3)) {
                            "set" -> {
                                newConfig = config.copy(
                                    greetingText = message.words.drop(4)
                                        .joinToString(separator = " ")
                                        .replace('%', '_')
                                        .replace("%user%", "%1\$s")
                                        .replace("%guild%", "%2\$s")
                                )
                                textMessage(texts.getStringOrKey("greeting.applied"))
                            }
                            "enable" -> {
                                newConfig = config.copy(greetNewUsers = true)
                                textMessage(texts.getStringOrKey("greeting.enabled"))
                            }
                            "disable" -> {
                                newConfig = config.copy(greetNewUsers = false)
                                textMessage(texts.getStringOrKey("greeting.disabled"))
                            }
                            "channel" -> {
                                val channel = try {
                                    clientStore.channels[message.words[4].drop(2).dropLast(1)]
                                } catch (_: Exception) {
                                    return textMessage(texts.getStringOrKey("help"))
                                }

                                newConfig = config.copy(greetingsChannel = channel.channelId)
                                textMessage(texts.formatString("greeting.channelset", channel.get().mention))
                            }
                            else -> textMessage(texts.getStringOrKey("help"))
                        }
                    }

                    else -> textMessage(texts.getStringOrKey("help"))
                }
            }
            else -> textMessage(texts.getStringOrKey("help"))

        }
        newConfig?.run {
            GuildConfigs.replace(guild.id, this)
        }
        return answer
    }

    private suspend fun getGreetingsChannelName(clientStore: ClientStore, config: GuildConfig, texts: ResourceBundle): String {
        val channel = try {
            clientStore.channels[config.greetingsChannel]
        } catch (_: Exception) {
            return texts.getStringOrKey("greeting.channel.error")
        }
        return channel.get().name ?: texts.getStringOrKey("greeting.channel.error")
    }
}