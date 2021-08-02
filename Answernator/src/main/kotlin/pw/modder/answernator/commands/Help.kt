package pw.modder.answernator.commands

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.entity.Message
import pw.modder.answernator.db.Db
import pw.modder.answernator.db.guild.Config
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.CommandList
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.LocalizedCommand
import pw.modder.answernator.utils.extensions.joinToStrings
import pw.modder.answernator.utils.extensions.kord.guildId
import pw.modder.answernator.utils.extensions.kord.replyEmbed
import pw.modder.answernator.utils.extensions.removeGraves
import pw.modder.answernator.utils.locale.CommandLocaleBundle

class Help: LocalizedCommand {
    override val name: String = "help"
    override val cmdType = Command.CommandGroup.USER

    override suspend fun action(message: Message, args: List<String>, texts: CommandLocaleBundle, config: Config?) {
        if (args.isEmpty()) {
            val permissions = message.getGuildOrNull()?.permissions ?: Permissions()

            val blacklist = when(message.guildId) {
                null -> listOf()
                else -> Db.getBlacklisted(message.guildId!!)
            }

            val cmds = when(permissions.contains(Permission.Administrator)) {
                true -> CommandList.commands
                false -> CommandList.commands.filterNot { it.name in blacklist }
            }.filter { it.check(message, texts.locale) }.filterNot { it.localesWhitelist?.contains(texts.locale) == false }.groupBy { it.cmdType }

            message.replyEmbed {
                title = texts["title_cmdlist"]
                description = texts["cmdlist.usage"]

                cmds.forEach { (cmdType: Command.CommandGroup, cmds: List<Command>) ->
                    if (cmds.isEmpty()) return@forEach

                    cmds.map { it.getDescription(texts.locale)?.run { "`${it.name}`: $this" }
                        ?: "`${it.name}`" }.joinToStrings(1024, "\n").forEach {
                        field(texts["cmdlist.${cmdType.name}"], false) { it }
                    }
                }
            }
            return
        }

        val cmd = CommandList.commands.singleOrNull { it.name == args.first().lowercase() }
        if (cmd == null) {
            message.replyEmbed {
                title = texts["title"].format(args.first().removeGraves())
                description = texts["not_found"].format(args.first().removeGraves())

            }
            return
        }

        if (message.data.author.id.asString != Globals.config.author && !cmd.check(message, texts.locale)) {
            message.replyEmbed {
                title = texts["title"].format(args.first())
                description = texts["no_permissions"]
            }
            return
        }

        val help = cmd.getHelp(texts.locale)
        val desc = cmd.getDescription(texts.locale)
        message.replyEmbed {
            title = texts["title"].format(args.first())
            description = if (help.isNullOrEmpty() || desc.isNullOrEmpty()) texts["not_available"] else "$desc\n$help"
        }
    }
}