package pw.modder.answernator.commands

import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import pw.modder.answernator.db.guild.Config
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.CommandList
import pw.modder.answernator.utils.GuildCommand
import pw.modder.answernator.utils.extensions.kord.reply
import pw.modder.answernator.utils.extensions.removeGraves
import java.util.*

class Sudo: GuildCommand {
    override val name = "sudo"
    override val cmdType = Command.CommandGroup.DEBUG
    override val userGroup = Command.UserGroup.OWNER

    override fun getDescription(locale: Locale): String? {
        return "Run command as guild admin"
    }

    override fun getHelp(locale: Locale): String? {
        return "Usage: `sudo [command] [command params]`"
    }

    override suspend fun action(message: Message, args: List<String>, guild: Guild, locale: Locale, config: Config) {
        if (args.isEmpty()) {
            message.reply("No command specified.\n" + getHelp(locale))
            return
        }

        if (args.first().equals(name, true)) {
            message.reply("No recursion allowed")
            return
        }

        CommandList.findCommand(args.first(), true, Command.ChannelTypes.GUILD)?.action(message, args.drop(1), locale, config)
            ?: message.reply("Command `${args.first().removeGraves()}` not found")
    }
}