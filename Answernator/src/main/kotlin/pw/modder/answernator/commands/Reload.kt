package pw.modder.answernator.commands

import dev.kord.core.entity.Message
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.CommandList
import pw.modder.answernator.utils.extensions.kord.reply
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

    override suspend fun action(message: Message, args: List<String>, locale: Locale) {
        CommandList.load()
        message.reply("Reloaded. Loaded ${CommandList.commands.size} commands.")
    }
}
