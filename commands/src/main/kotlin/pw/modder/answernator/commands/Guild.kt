package pw.modder.answernator.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.rest.EmbedImage
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.util.ClientStore
import com.jessecorbett.diskord.util.mention
import kotlinx.serialization.UnstableDefault
import pw.modder.answernator.commands.commandTypes.LocalizedGuildOnlyCommand
import pw.modder.answernator.utils.Command
import java.util.*
import com.jessecorbett.diskord.dsl.message as dslmessage

@UnstableDefault
class Guild: LocalizedGuildOnlyCommand {
    override val name = "guild"
    override val userGroup = Command.UserGroup.ADMIN
    override suspend fun action(clientStore: ClientStore, message: Message, locale: Locale): CombinedMessageEmbed {
        if (message.guildId == null) return textMessage(getString(locale, "error"))
        val guildClient = clientStore.guilds[message.guildId!!]
        val guild = guildClient.get()

        return dslmessage {
            field(getString(locale, "name"), guild.name, true)
            field(getString(locale, "owner"), "<@${guild.ownerId}>", true)
            field(getString(locale, "emojis"), guild.emojis.size.toString(), true)
            field(getString(locale, "roles"), guild.roles.joinToString("\n") { it.mention }, true)
            field(getString(locale, "region"), guild.region, true)
            field(getString(locale, "features"), guild.features.joinToString(", "), true)
            field(getString(locale, "verificationLevel"), guild.verificationLevel.name, true)
            field(getString(locale, "mfaEnabled"), guild.mfaLevel.name, true)
            field(getString(locale, "explicitContentFilterLevel"), guild.explicitContentFilterLevel.name, true)
            guild.iconHash?.run {
                thumbnail = EmbedImage("https://cdn.discordapp.com/icons/${guild.id}/$this")
            }
            guild.afkChannelId?.run {
                field("afkChannel", "<#$this>", true)
            }
        }
    }
}