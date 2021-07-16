package pw.modder.answernator.commands

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message
import pw.modder.answernator.db.Db
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.CommandList
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.LocalizedCommand
import pw.modder.answernator.utils.extensions.joinToStrings
import pw.modder.answernator.utils.extensions.kord.guildId
import pw.modder.answernator.utils.extensions.removeGraves
import pw.modder.answernator.utils.locale.CommandLocaleBundle

class Help: LocalizedCommand {
    override val name: String = "help"
    override val cmdType = Command.CommandGroup.USER

    override suspend fun action(message: Message, args: List<String>, texts: CommandLocaleBundle) {
        if (args.isEmpty()) {
            val permissions = message.getGuildOrNull()?.permissions ?: Permissions()

            val blacklist = when(message.guildId) {
                null -> listOf()
                else -> Db.getBlacklisted(message.guildId!!)
            }

            val cmds = when(permissions.contains(Permission.Administrator)) {
                true -> CommandList.commands.filter { it.check(message) }.groupBy { it.cmdType }
                false -> CommandList.commands.filterNot { it.name in blacklist }.filter { it.check(message) }.groupBy { it.cmdType }
            }

            message.reply { embed {
                title = texts.getString("title_cmdlist")
                description = texts.getString("cmdlist.usage")

                cmds.forEach { (cmdType: Command.CommandGroup, cmds: List<Command>) ->
                    if (cmds.isEmpty()) return@forEach
                    val prefix = if (message.guildId == null) Globals.config.prefix else Db.getGuildConfig(message.guildId!!).cmdPrefix
                    cmds.map { it.getDescription(texts.locale)?.run { "`${prefix}${it.name}`: $this" }
                        ?: "`${Globals.config.prefix}${it.name}`" }.joinToStrings(1024, "\n").forEach {
                        field( texts.getString("cmdlist.${cmdType.name}"), false) { it }
                    }
                }
            } }
            return
        }

        val cmd = CommandList.commands.singleOrNull { it.name == args.first().toLowerCase() }
        if (cmd == null) {
            message.reply { embed {
                title = texts.formatString("title", args.first().removeGraves())
                description = texts.formatString("not_found", args.first().removeGraves())
            } }
            return
        }

        if (message.data.author.id.asString != Globals.config.author && !cmd.check(message)) {
            message.reply { embed {
                title = texts.formatString("title", args.first())
                description = texts.getString("no_permissions")
            } }
            return
        }

        val help = cmd.getHelp(texts.locale)
        val desc = cmd.getDescription(texts.locale)
        message.reply { embed {
            title = texts.formatString("title", args.first())
            description = if (help.isNullOrEmpty() || desc.isNullOrEmpty()) texts.getString("not_available") else "$desc\n$help"
        } }
    }
}