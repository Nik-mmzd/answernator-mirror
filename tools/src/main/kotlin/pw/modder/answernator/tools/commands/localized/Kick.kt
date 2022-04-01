package pw.modder.answernator.tools.commands.localized

import dev.kord.common.entity.Permission
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import pw.modder.answernator.db.guild.Config
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.LocalizedGuildCommand
import pw.modder.answernator.utils.extensions.extractMentionedId
import pw.modder.answernator.utils.extensions.kord.isAdmin
import pw.modder.answernator.utils.extensions.kord.reply
import pw.modder.answernator.utils.locale.CommandLocaleBundle

class Kick: LocalizedGuildCommand {
    override val name = "kick"
    override val userGroup = Command.UserGroup.PERMISSION
    override val permission = Permission.KickMembers
    override val cmdType = Command.CommandGroup.MODER
    override val requiredPermission: Permission? = Permission.KickMembers


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

        val mentionedUser = message.mentionedUserBehaviors.firstOrNull() { it.id.toString() == mentionedUserId }?.asMemberOrNull(guild.id)
        if (mentionedUser == null) {
            message.reply(texts.error())
            return
        }

        if (mentionedUser.id == message.author?.id) {
            message.reply(texts["whitelisted.author"].format(mentionedUser.mention))
            return
        }

        if (mentionedUser.id == message.kord.selfId) {
            message.reply(texts["whitelisted.self"].format(mentionedUser.mention))
            return
        }

        if (mentionedUser.isOwner() || mentionedUser.isAdmin()) {
            message.reply(texts["whitelisted"])
            return
        }

        mentionedUser.kick(args.drop(1).joinToString(separator = " "))
        message.reply(texts["done"].format(mentionedUser.mention))
    }
}