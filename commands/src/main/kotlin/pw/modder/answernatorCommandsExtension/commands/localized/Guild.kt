package pw.modder.answernatorCommandsExtension.commands.localized

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.rest.EmbedField
import com.jessecorbett.diskord.api.rest.EmbedImage
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.util.mention
import pw.modder.answernator.cache.GuildCache.getCached
import kotlinx.serialization.UnstableDefault
import pw.modder.answernatorCommandsExtension.commandTypes.LocalizedGuildOnlyCommand
import pw.modder.answernator.utils.Command
import java.util.*
import com.jessecorbett.diskord.dsl.message as dslmessage

@UnstableDefault
class Guild: LocalizedGuildOnlyCommand {
    override val name = "guild"
    override val userGroup = Command.UserGroup.ADMIN
    override suspend fun action(bot: Bot, message: Message, texts: ResourceBundle): CombinedMessageEmbed {
        val guild = bot.clientStore.guilds[message.guildId ?: return textMessage(texts.getStringOrKey("error"))].getCached()

        return dslmessage {
            field(texts.getStringOrKey("name"), guild.name, true)
            field(texts.getStringOrKey("owner"), "<@${guild.ownerId}>", true)
            field(texts.getStringOrKey("emojis"), guild.emojis.size.toString(), true)
            if (guild.roles.size < 50) {
                field(
                    texts.getStringOrKey("roles"),
                    guild.roles.filterNot { it.name == "@everyone" }.joinToString(" ") { it.mention },
                    false
                )
            } else {
                field(texts.getStringOrKey("roles"), (guild.roles.size - 1).toString(), true)
            }
            field(texts.getStringOrKey("region"), guild.region.capitalize(), true)
            field(texts.getStringOrKey("features"),
                guild.features.joinToString(", ") {
                    texts.getStringOrKey("features.${it}")
                }.ifEmpty { texts.getStringOrKey("features.empty") },
                true)
            field(texts.getStringOrKey("verificationLevel"), texts.getStringOrKey("verification.level.${guild.verificationLevel.name}"), true)
            field(texts.getStringOrKey("mfaEnabled"), texts.getStringOrKey("mfa.${guild.mfaLevel.name}"), true)
            field(texts.getStringOrKey("explicitContentFilterLevel"), texts.getStringOrKey("explicitContentFilterLevel.${guild.explicitContentFilterLevel.name}"), true)
            guild.iconHash?.run {
                thumbnail = EmbedImage("https://cdn.discordapp.com/icons/${guild.id}/$this")
            }
            guild.afkChannelId?.run {
                field(texts.getStringOrKey("afkChannel"), "<#$this>", true)
                field(texts.getStringOrKey("afkTimeout"), texts.formatString("afkTimeout.value", this), true)
            }

            field(texts.getStringOrKey("notifications"), texts.getStringOrKey("notifications.level.${guild.defaultMessageNotificationLevel.name}"), true)
            guild.widgetEnabled?.run {
                field(texts.getStringOrKey("widget"), texts.getStringOrKey("widget.$this"), true)
                guild.widgetChannelId?.run {
                    field(texts.getStringOrKey("widget.channel"), "<#$this>", true)
                }
            }

            guild.systemMessageChannelId?.run {
                field(texts.getStringOrKey("system.channel"), "<#$this>", true)
            }
        }
    }
}