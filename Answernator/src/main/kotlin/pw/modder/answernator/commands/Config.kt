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

    private suspend fun getGreetingsChannelName(clientStore: ClientStore, config: GuildConfig, locale: Locale): String {
        val channel = try {
            clientStore.channels[config.greetingsChannel]
        } catch (_: Exception) {
            return getString(locale, "greeting.channel.error")
        }
        return channel.get().name ?: getString(locale, "greeting.channel.error")
    }

    override suspend fun action(clientStore: ClientStore, message: Message, locale: Locale): CombinedMessageEmbed {
        val guildClient = message.guildId?.run { clientStore.guilds[this] } ?: return textMessage(getString(locale, "noguild"))
        val guild = guildClient.get()
        val config = GuildConfigs.get(guild.id)
        var newConfig: GuildConfig? = null

        val answer = when(message.words.getOrNull(1)) {
            "get" -> {
                val chName= getGreetingsChannelName(clientStore, config, locale)
                dslmessage {
                    title = guild.name
                    description = getString(locale, "description")

                    field(getString(locale, "lang"), config.lang, false)
                    field(getString(locale, "lang.available"), GlobalConfig.get().langs.joinToString(separator = ", ") { "`$it`" }, false)
                    field(getString(locale, "greeter"), getString(locale, "greeter.${config.greetNewUsers}"), false)
                    field(getString(locale, "greeting"), String.format(config.greetingText, message.author.username, guild.name), false)
                    field(getString(locale, "greeting.help"), getString(locale, "greeting.help.value"), false)
                    field(getString(locale, "greeting.channel"), chName, false)
                }
            }
            "set" -> {
                when(message.words.getOrNull(2)) {
                    "lang" -> {
                        if (message.words[3] in GlobalConfig.get().langs) {
                            newConfig = config.copy(lang = message.words[3])
                            textMessage(formatString(locale, "locale.updated", message.words[3]))
                        } else textMessage(formatString(locale, "locale.notfound", message.words[3]))
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
                                textMessage(getString(locale, "greeting.applied"))
                            }
                            "enable" -> {
                                newConfig = config.copy(greetNewUsers = true)
                                textMessage(getString(locale, "greeting.enabled"))
                            }
                            "disable" -> {
                                newConfig = config.copy(greetNewUsers = false)
                                textMessage(getString(locale, "greeting.disabled"))
                            }
                            "channel" -> {
                                val channel = try {
                                    clientStore.channels[message.words[4].drop(2).dropLast(1)]
                                } catch (_: Exception) {
                                    return textMessage(getString(locale, "help"))
                                }

                                newConfig = config.copy(greetingsChannel = channel.channelId)
                                textMessage(formatString(locale, "greeting.channelset", channel.get().mention))
                            }
                            else -> textMessage(getString(locale, "help"))
                        }
                    }

                    else -> textMessage(getString(locale, "help"))
                }
            }
            else -> textMessage(getString(locale, "help"))

        }
        newConfig?.run {
            GuildConfigs.replace(guild.id, this)
        }
        return answer
    }

}