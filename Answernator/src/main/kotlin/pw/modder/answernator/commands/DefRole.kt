package pw.modder.answernator.commands

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Member
import dev.kord.core.entity.Message
import kotlinx.coroutines.flow.collect
import pw.modder.answernator.db.guild.Config
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.LocalizedGuildCommand
import pw.modder.answernator.utils.extensions.kord.reply
import pw.modder.answernator.utils.locale.CommandLocaleBundle

class DefRole: LocalizedGuildCommand {
    override val name = "defrole"
    override val userGroup = Command.UserGroup.PERMISSION
    override val permission = Permission.ManageMessages
    override val cmdType = Command.CommandGroup.MODER
    override val requiredPermission: Permission? = Permission.ManageRoles

    override suspend fun action(message: Message, args: List<String>, guild: Guild, texts: CommandLocaleBundle, config: Config) {
        if (config.defaultRole == null) {
            message.reply(texts.getString("not.configured"))
            return
        }

        val add = when(args.firstOrNull()) {
            "add", "a", "set", "give" -> true
            "remove", "rm", "r", "delete", "del", "take" -> false
            else -> {
                message.reply(texts.getErrorString())
                return
            }
        }

        val members = mutableListOf<Member>()
        message.mentionedUsers.collect {
            when(add) {
                true -> it.asMemberOrNull(guild.id)?.also { members.add(it) }?.addRole(Snowflake(config.defaultRole!!), "Given with defrole by ${message.data.author.username}")
                false -> it.asMemberOrNull(guild.id)?.also { members.add(it) }?.removeRole(Snowflake(config.defaultRole!!), "Took with defrole by ${message.data.author.username}")
            }
        }

        message.reply(texts.formatString("done.$add", members.joinToString(" ") { it.mention }))
    }
}