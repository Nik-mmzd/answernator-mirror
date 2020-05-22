package pw.modder.answernator.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.model.Permissions
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.util.authorId
import com.jessecorbett.diskord.util.words
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.CommandList
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.LocalizedCommand
import pw.modder.answernator.utils.extensions.computePermissions
import pw.modder.answernator.utils.extensions.joinToStrings
import pw.modder.answernator.utils.extensions.removeGraves
import java.util.*
import com.jessecorbett.diskord.dsl.message as dslmessage

class Help: LocalizedCommand {
    override val name: String = "help"
    override val cmdType = Command.CommandGroup.USER

    override suspend fun action(bot: Bot, message: Message, texts: ResourceBundle): CombinedMessageEmbed {
        val guildClient = message.guildId?.run { bot.clientStore.guilds[this] }
        if (message.words.size == 1) {
            val permissions = when (guildClient) {
                null -> Permissions.NONE
                else -> message.partialMember?.computePermissions(guildClient, message.authorId) ?: Permissions.NONE
            }
            val cmds = CommandList.commands.filter { it.check(message, permissions) }.groupBy { it.cmdType }

            return dslmessage {
                title = texts.getStringOrKey("title_cmdlist")
                description = texts.getStringOrKey("cmdlist.usage")

                cmds.forEach { (cmdType: Command.CommandGroup, cmds: List<Command>) ->
                    if (cmds.isEmpty()) return@forEach
                    cmds.map { it.getDescription(texts.locale)?.run { "`${Globals.config.prefix}${it.name}`: $this" }
                        ?: "`${Globals.config.prefix}${it.name}`" }.joinToStrings(1024, "\n").forEach {
                        field(
                            texts.getStringOrKey("cmdlist.${cmdType.name}"),
                            it,
                            inline = false
                        )
                    }
                }
            }
        }

        val cmd = CommandList.commands.singleOrNull { it.name == message.words[1].toLowerCase() }
            ?: return dslmessage {
                title = texts.formatString("title", message.words[1].removeGraves())
                description = texts.formatString("not_found", message.words[1].removeGraves())
            }

        if (message.authorId != Globals.config.author && !cmd.check(message, guildClient)) return dslmessage {
            title = texts.formatString("title", message.words[1])
            description = texts.getStringOrKey("no_permissions")
        }

        val help = cmd.getHelp(texts.locale)
        val desc = cmd.getDescription(texts.locale)
        return dslmessage {
            title = texts.formatString("title", message.words[1])
            description = if (help.isNullOrEmpty() || desc.isNullOrEmpty()) texts.getStringOrKey("not_available") else "$desc\n$help"
        }
    }
}