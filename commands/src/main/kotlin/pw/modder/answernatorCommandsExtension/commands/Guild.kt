package pw.modder.answernatorCommandsExtension.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.rest.EmbedImage
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.util.ClientStore
import com.jessecorbett.diskord.util.mention
import kotlinx.serialization.UnstableDefault
import pw.modder.answernatorCommandsExtension.commandTypes.LocalizedGuildOnlyCommand
import pw.modder.answernator.utils.Command
import java.util.*
import com.jessecorbett.diskord.dsl.message as dslmessage

@UnstableDefault
class Guild: LocalizedGuildOnlyCommand {
    override val name = "guild"
    override val userGroup = Command.UserGroup.ADMIN
    override suspend fun action(clientStore: ClientStore, message: Message, texts: ResourceBundle): CombinedMessageEmbed {
        if (message.guildId == null) return textMessage(texts.getStringOrKey("error"))
        val guildClient = clientStore.guilds[message.guildId!!]
        val guild = guildClient.get()

        return dslmessage {
            field(texts.getStringOrKey("name"), guild.name, true)
            field(texts.getStringOrKey("owner"), "<@${guild.ownerId}>", true)
            field(texts.getStringOrKey("emojis"), guild.emojis.size.toString(), true)
            field(texts.getStringOrKey("roles"), guild.roles.joinToString("\n") { it.mention }, true)
            field(texts.getStringOrKey("region"), guild.region, true)
            field(texts.getStringOrKey("features"), guild.features.joinToString(", "), true)
            field(texts.getStringOrKey("verificationLevel"), guild.verificationLevel.name, true)
            field(texts.getStringOrKey("mfaEnabled"), guild.mfaLevel.name, true)
            field(texts.getStringOrKey("explicitContentFilterLevel"), guild.explicitContentFilterLevel.name, true)
            guild.iconHash?.run {
                thumbnail = EmbedImage("https://cdn.discordapp.com/icons/${guild.id}/$this")
            }
            guild.afkChannelId?.run {
                field(texts.getStringOrKey("afkChannel"), "<#$this>", true)
            }
        }
    }
}