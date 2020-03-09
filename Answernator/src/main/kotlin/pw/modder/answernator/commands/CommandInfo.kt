package pw.modder.answernator.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.util.ClientStore
import com.jessecorbett.diskord.util.words
import kotlinx.serialization.UnstableDefault
import pw.modder.answernator.utils.CommandList
import java.util.*
import pw.modder.answernator.utils.Command
import com.jessecorbett.diskord.dsl.message as dslmessage

@UnstableDefault
class CommandInfo: Command {
    private fun getCommandTypeString(command: Command): String {
        return when(command.userGroup) {
            Command.UserGroup.OWNER -> "Owner only"
            Command.UserGroup.ADMIN -> "Admin only"
            Command.UserGroup.ALL -> "Public"
            Command.UserGroup.PERMISSION -> "Permission: " + command.permission
        }
    }

    private fun getCommandChannelTypeString(command: Command): String {
        return command.channels.toString()
    }

    override val name = "command"
    override val userGroup = Command.UserGroup.OWNER
    override fun getHelp(locale: Locale): String? {
        return "Command info. Usage: `command [command]`"
    }
    override suspend fun action(bot: Bot, message: Message, locale: Locale): CombinedMessageEmbed {
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