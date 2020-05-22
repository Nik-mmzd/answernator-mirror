package pw.modder.answernator.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.util.words
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.CommandList
import pw.modder.answernator.utils.extensions.removeGraves
import java.util.*

class Sudo: Command {
    override val name = "sudo"
    override val cmdType = Command.CommandGroup.DEBUG
    override val userGroup = Command.UserGroup.OWNER
    override val channels = EnumSet.of(Command.ChannelTypes.GUILD)
    override fun getDescription(locale: Locale): String? {
        return "Run command as guild admin"
    }

    override fun getHelp(locale: Locale): String? {
        return "Usage: `sudo [command] [command params]`"
    }

    override suspend fun action(bot: Bot, message: Message, locale: Locale): CombinedMessageEmbed {
        if (message.words.size < 2) return textMessage("No command specified.\n" + getHelp(locale))
        if (message.words[1].equals(name, true)) return textMessage("No recursion allowed")
        CommandList.commands.singleOrNull {
            it.name.equals(message.words[1], true) && Command.ChannelTypes.GUILD in it.channels
        }?.run {
            return action(bot, message.copy(content = message.words.drop(1).joinToString(" ")), locale)
        } ?: return textMessage("Command `${message.words[1].removeGraves()}` not found")
    }

}