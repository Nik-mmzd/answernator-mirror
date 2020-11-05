package pw.modder.answernator.tools.commands.localized

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.model.Permission
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.util.authorId
import com.jessecorbett.diskord.util.mention
import com.jessecorbett.diskord.util.words
import pw.modder.answernator.cache.GuildCache.getCached
import pw.modder.answernator.tools.commandTypes.LocalizedGuildOnlyCommand
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.extensions.bot.isMe
import pw.modder.answernator.utils.extensions.isAdmin
import pw.modder.answernator.utils.extensions.isUserMention
import java.util.*

class Kick: LocalizedGuildOnlyCommand {
    override val name = "kick"
    override val userGroup = Command.UserGroup.PERMISSION
    override val permission = Permission.KICK_MEMBERS
    override val cmdType = Command.CommandGroup.MODER
    override val requiredPermission: Permission? = Permission.KICK_MEMBERS

    override suspend fun action(bot: Bot, message: Message, texts: ResourceBundle): CombinedMessageEmbed {
        if (message.words.getOrNull(1)?.isUserMention() != true || message.usersMentioned.size != 1) return texts.errorMessage()

        val mentionedUser = message.usersMentioned.single()
        if (mentionedUser.id == message.authorId || bot.isMe(mentionedUser)) return texts.message("whitelisted")

        val client = bot.clientStore.guilds[message.guildId ?: return texts.errorMessage()]

        if (client.getMember(mentionedUser.id).isAdmin(client.getCached(), mentionedUser.id)) return texts.message("whitelisted")

        client.removeMember(mentionedUser.id)
        return texts.message("done", mentionedUser.mention)
    }
}