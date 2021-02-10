package pw.modder.answernator.tools.commands.localized

import com.jessecorbett.diskord.api.exception.DiscordNotFoundException
import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.rest.EmbedImage
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.util.mention
import com.jessecorbett.diskord.util.words
import pw.modder.answernator.cache.GuildCache.getCached
import pw.modder.answernator.tools.commandTypes.LocalizedGuildOnlyCommand
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.Utils
import pw.modder.answernator.utils.extensions.toChannelMention
import pw.modder.answernator.utils.extensions.toUserMention
import pw.modder.answernator.utils.locale.CommandLocaleBundle
import java.util.*
import com.jessecorbett.diskord.dsl.message as dslmessage

class Guild: LocalizedGuildOnlyCommand {
    override val name = "guild"
    override val userGroup = Command.UserGroup.ADMIN
    override val cmdType = Command.CommandGroup.ADMIN
    override suspend fun action(bot: Bot, message: Message, texts: CommandLocaleBundle): CombinedMessageEmbed {
        if (message.words.getOrNull(1).equals("list", true)) {
            return textMessage(bot.clientStore.discord.getGuilds()
                .joinToString("\n", prefix = texts.getString("list.available")) {
                    "${it.name}: `${it.id}`"
                }
            )
        }

        val guild =  try {
            bot.clientStore.guilds[message.words.getOrNull(1) ?: message.guildId ?: return texts.getErrorString().toMessage()].getCached()
        } catch (e: DiscordNotFoundException) {
            return texts.getErrorString().toMessage()
        }

        return dslmessage {
            title = texts.formatString("title", guild.name)

            field(texts.getString("owner"), guild.ownerId.toUserMention(), true)
            field(texts.getString("emojis"), guild.emojis.size.toString(), true)
            if (guild.roles.size < 50 && guild.id == message.guildId) {
                field(
                    texts.getString("roles"),
                    guild.roles.filterNot { it.id == guild.id }.joinToString(" ") { it.mention },
                    false
                )
            } else {
                field(texts.getString("roles"), (guild.roles.size - 1).toString(), true)
            }
            field(texts.getString("created_at"), texts.formatString("created_at.value", Utils.prettyPrintPeriod(texts.locale, Utils.snowflakeCreatedAt(guild.id))), false)
            field(texts.getString("region"), guild.region.capitalize(), true)
            field(texts.getString("features"),
                guild.features.joinToString(", ") {
                    texts.getString("features.$it")
                }.ifEmpty { texts.getString("features.empty") },
                true)
            field(texts.getString("verificationLevel"), texts.getString("verification.level.${guild.verificationLevel.name}"), true)
            field(texts.getString("mfaEnabled"), texts.getString("mfa.${guild.mfaLevel.name}"), true)
            field(texts.getString("explicitContentFilterLevel"), texts.getString("explicitContentFilterLevel.${guild.explicitContentFilterLevel.name}"), true)
            guild.iconHash?.run {
                thumbnail = EmbedImage("https://cdn.discordapp.com/icons/${guild.id}/$this")
            }
            guild.afkChannelId?.run {
                field(texts.getString("afkChannel"), this.toChannelMention(), true)
                field(texts.getString("afkTimeout"), texts.formatString("afkTimeout.value", guild.afkTimeoutSeconds), true)
            }

            field(texts.getString("notifications"), texts.getString("notifications.level.${guild.defaultMessageNotificationLevel.name}"), true)
            guild.widgetEnabled?.run {
                field(texts.getString("widget"), texts.getString("widget.$this"), true)
                guild.widgetChannelId?.run {
                    field(texts.getString("widget.channel"), this.toChannelMention(), true)
                }
            }

            guild.systemMessageChannelId?.run {
                field(texts.getString("system.channel"), this.toChannelMention(), true)
            }
        }
    }
}