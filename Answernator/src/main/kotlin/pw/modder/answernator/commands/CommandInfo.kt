package pw.modder.answernator.commands

import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message
import pw.modder.answernator.db.Db
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.CommandList
import pw.modder.answernator.utils.extensions.kord.guildId
import pw.modder.answernator.utils.extensions.kord.reply
import java.util.*

class CommandInfo: Command {
    private fun getCommandTypeString(command: Command): String {
        return when(command.userGroup) {
            Command.UserGroup.OWNER -> "Owner only"
            Command.UserGroup.ADMIN -> "Admin only"
            Command.UserGroup.ALL -> "Public"
            Command.UserGroup.PERMISSION -> "Permission: " + command.permission
        }
    }

    private fun Boolean.toBoolString(): String = when (this) {
        true -> "yes"
        false -> "no"
    }

    private fun getCommandChannelTypeString(command: Command): String {
        return command.channels.toString()
    }

    override val cmdType = Command.CommandGroup.DEBUG

    override val name = "command"
    override val userGroup = Command.UserGroup.OWNER
    override fun getHelp(locale: Locale): String? {
        return "Command info. Usage: `command [command]`"
    }

    override fun getDescription(locale: Locale): String? {
        return "command debug info"
    }

    override suspend fun action(message: Message, args: List<String>, locale: Locale) {
        if (args.isEmpty()) {
            message.reply("No command specified")
            return
        }
        val cmd = CommandList.commands.singleOrNull { it.name == args.first() }
        if (cmd == null) {
            message.reply("Command `${args.first()}` not found")
            return
        }

        message.reply {
            embed {
                title = "Command information"

                field("Command name", true) { cmd.name }
                field("Command publicity", true) { getCommandTypeString(cmd) }
                field("Command channel types", true) { getCommandChannelTypeString(cmd) }

                cmd.requiredPermission?.run cmd@{
                    field("Required bot permission", true) { this.toString() }
                    message.getGuildOrNull()?.getMemberOrNull(message.kord.selfId)?.getPermissions()?.run {
                        field("Bot can run", true) { contains(this@cmd).toBoolString() }
                    }
                }
                message.getAuthorAsMember()?.getPermissions()?.run {
                    field("Member can use", true) { cmd.check(message).toBoolString() }
                }

                message.guildId?.run {
                    field("Is blacklisted", true) { Db.isBlackListed(this, cmd.name).toBoolString() }
                }
            }
            allowedMentions { repliedUser = false }
        }
    }
}