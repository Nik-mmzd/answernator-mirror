package pw.modder.answernator.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.CommandList
import java.util.*

class Reload: Command {
    override val name: String = "reload"
    override val userGroup = Command.UserGroup.OWNER
    override val cmdType = Command.CommandGroup.OWNER

    override fun getHelp(locale: Locale): String? {
        return "Reloads bot. Not a restart! Not localized."
    }

    override fun getDescription(locale: Locale): String? {
        return "reload bot"
    }

    override suspend fun action(bot: Bot, message: Message, locale: Locale): CombinedMessageEmbed {
        CommandList.load()
        return textMessage("Reloaded. Loaded ${CommandList.commands.size} commands.")
    }
}
