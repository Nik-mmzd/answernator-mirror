package pw.modder.answernator.commands

import dev.kord.core.entity.Message
import dev.kord.core.firstOrNull
import kotlinx.coroutines.flow.toList
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.extensions.kord.reply
import java.util.*

class Leave: Command {
    override val name = "leave"
    override val userGroup = Command.UserGroup.OWNER
    override val cmdType = Command.CommandGroup.OWNER
    override fun getHelp(locale: Locale): String? {
        return "Usage: `leave` to get guild ids or `leave guild-id` to leave guild"
    }

    override fun getDescription(locale: Locale): String? {
        return "leave any server"
    }

    override suspend fun action(message: Message, args: List<String>, locale: Locale) {
        if (args.isEmpty()) {
            message.reply(message.kord.guilds.toList().joinToString("\n", prefix = "${getHelp(locale)}\nAvailable guilds:\n") {
                "${it.name}: `${it.id.asString}`"
            })
            return
        }
        val guild = message.kord.guilds.firstOrNull { it.id.asString == args.first() }
        if (guild == null) {
            message.reply("Guild not found")
            return
        }

        guild.leave()
        message.reply("Done. Left guild ${guild.name}")
    }
}