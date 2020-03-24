package pw.modder.answernator.tools.commands.localized

import com.jessecorbett.diskord.api.exception.DiscordNotFoundException
import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.rest.EmbedField
import com.jessecorbett.diskord.api.rest.EmbedImage
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.util.mention
import com.jessecorbett.diskord.util.words
import pw.modder.answernator.cache.GuildCache.getCached
import kotlinx.serialization.UnstableDefault
import pw.modder.answernator.tools.commandTypes.LocalizedGuildOnlyCommand
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.extensions.toChannelMention
import pw.modder.answernator.utils.extensions.toUserMention
import java.util.*
import com.jessecorbett.diskord.dsl.message as dslmessage

@UnstableDefault
class Guild: LocalizedGuildOnlyCommand {
    override val name = "guild"
    override val userGroup = Command.UserGroup.ADMIN
    override val cmdType = Command.CommandGroup.ADMIN
    override suspend fun action(bot: Bot, message: Message, texts: ResourceBundle): CombinedMessageEmbed {
        if (message.words.getOrNull(1).equals("list", true)) {
            return textMessage(bot.clientStore.discord.getGuilds()
                .joinToString("\n", prefix = texts.getStringOrKey("list.available")) {
                    "${it.name}: `${it.id}`"
                }
            )
        }

        val guild =  try {
            bot.clientStore.guilds[message.words.getOrNull(1) ?: message.guildId ?: return texts.errorMessage()].getCached()
        } catch (e: DiscordNotFoundException) {
            return texts.errorMessage()
        }

        return dslmessage {
            field(texts.getStringOrKey("name"), guild.name, true)
            field(texts.getStringOrKey("owner"), guild.ownerId.toUserMention(), true)
            field(texts.getStringOrKey("emojis"), guild.emojis.size.toString(), true)
            if (guild.roles.size < 50 && guild.id == message.guildId) {
                field(
                    texts.getStringOrKey("roles"),
                    guild.roles.filterNot { it.id == guild.id }.joinToString(" ") { it.mention },
                    false
                )
            } else {
                field(texts.getStringOrKey("roles"), (guild.roles.size - 1).toString(), true)
            }
            field(texts.getStringOrKey("region"), guild.region.capitalize(), true)
            field(texts.getStringOrKey("features"),
                guild.features.joinToString(", ") {
                    texts.getStringOrKey("features.$it")
                }.ifEmpty { texts.getStringOrKey("features.empty") },
                true)
            field(texts.getStringOrKey("verificationLevel"), texts.getStringOrKey("verification.level.${guild.verificationLevel.name}"), true)
            field(texts.getStringOrKey("mfaEnabled"), texts.getStringOrKey("mfa.${guild.mfaLevel.name}"), true)
            field(texts.getStringOrKey("explicitContentFilterLevel"), texts.getStringOrKey("explicitContentFilterLevel.${guild.explicitContentFilterLevel.name}"), true)
            guild.iconHash?.run {
                thumbnail = EmbedImage("https://cdn.discordapp.com/icons/${guild.id}/$this")
            }
            guild.afkChannelId?.run {
                field(texts.getStringOrKey("afkChannel"), this.toChannelMention(), true)
                field(texts.getStringOrKey("afkTimeout"), texts.formatString("afkTimeout.value", this), true)
            }

            field(texts.getStringOrKey("notifications"), texts.getStringOrKey("notifications.level.${guild.defaultMessageNotificationLevel.name}"), true)
            guild.widgetEnabled?.run {
                field(texts.getStringOrKey("widget"), texts.getStringOrKey("widget.$this"), true)
                guild.widgetChannelId?.run {
                    field(texts.getStringOrKey("widget.channel"), this.toChannelMention(), true)
                }
            }

            guild.systemMessageChannelId?.run {
                field(texts.getStringOrKey("system.channel"), this.toChannelMention(), true)
            }
        }
    }
}