package pw.modder.answernator.tools.commands.localized

import dev.kord.common.entity.Permission
import dev.kord.core.behavior.ban
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import pw.modder.answernator.db.guild.Config
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.LocalizedGuildCommand
import pw.modder.answernator.utils.extensions.extractMentionedId
import pw.modder.answernator.utils.extensions.kord.isAdmin
import pw.modder.answernator.utils.extensions.kord.reply
import pw.modder.answernator.utils.locale.CommandLocaleBundle

class Ban: LocalizedGuildCommand {
    override val name = "ban"
    override val userGroup = Command.UserGroup.PERMISSION
    override val permission = Permission.BanMembers
    override val cmdType = Command.CommandGroup.MODER
    override val requiredPermission: Permission? = Permission.BanMembers

    override suspend fun action(message: Message, args: List<String>, guild: Guild, texts: CommandLocaleBundle, config: Config) {
        if (args.isEmpty()) {
            message.reply(texts.error())
            return
        }

        val mentionedUserId = args.first().extractMentionedId()
        if (mentionedUserId == null) {
            message.reply(texts.error())
            return
        }

        val member = message.mentionedUserBehaviors.first { it.id.toString() == mentionedUserId }.asMemberOrNull(guild.id)
        if (member == null) {
            message.reply(texts.error())
            return
        }

        if (member.id == guild.ownerId) {
            message.reply(texts["whitelisted.author"].format(member.mention))
            return
        }

        if (member.id == message.kord.selfId) {
            message.reply(texts["whitelisted.self"].format(member.mention))
            return
        }
        if (member.isAdmin()) {
            message.reply(texts["whitelisted"])
            return
        }

        member.ban {
            deleteMessagesDays = 0
            reason = args.drop(1).joinToString(" ", prefix = "${message.data.author.id}|")
        }

        message.reply(texts["done"].format(member.mention))
    }
}