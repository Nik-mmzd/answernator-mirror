package pw.modder.answernator.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.util.ClientStore
import com.jessecorbett.diskord.util.words
import kotlinx.serialization.UnstableDefault
import pw.modder.answernator.utils.ChannelTypes
import pw.modder.answernator.utils.CommandList
import pw.modder.answernator.utils.UserGroup
import java.util.*
import pw.modder.answernator.utils.Command
import com.jessecorbett.diskord.dsl.message as dslmessage

@UnstableDefault
class CommandInfo: Command {
    private fun getCommandTypeString(command: Command): String {
        return when(command.userGroup) {
            UserGroup.OWNER -> "Owner only"
            UserGroup.ADMIN -> "Admin only"
            UserGroup.ALL -> "Public"
            UserGroup.PERMISSION -> "Permission: " + command.permission
        }
    }

    private fun getCommandChannelTypeString(command: Command): String {
        return when(command.channels) {
            ChannelTypes.ALL -> "Guild/PM"
            ChannelTypes.GUILD -> "Guild only"
            ChannelTypes.DIRECT -> "PM only"
        }
    }

    override val name = "command"
    override val userGroup = UserGroup.OWNER
    override fun getHelp(locale: Locale): String? {
        return "Command info. Usage: `command [command]`"
    }
    override suspend fun action(clientStore: ClientStore, message: Message, locale: Locale): CombinedMessageEmbed {
        if (message.words.size == 1) return textMessage("No command specified")
        val cmd = CommandList.commands.singleOrNull { it.name == message.words[1] }
            ?: return textMessage("Command `${message.words[1]}` not found")
        return dslmessage {
            title = "Command information"

            field("Command name", cmd.name, true)
            field("Command publicity", getCommandTypeString(cmd), true)
            field("Command channel types", getCommandChannelTypeString(cmd), true)
        }
    }
}