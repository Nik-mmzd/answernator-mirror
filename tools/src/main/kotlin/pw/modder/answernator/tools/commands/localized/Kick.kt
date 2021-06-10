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
import pw.modder.answernator.utils.extensions.extractMentionedId
import pw.modder.answernator.utils.extensions.isAdmin
import pw.modder.answernator.utils.extensions.isUserMention
import pw.modder.answernator.utils.locale.CommandLocaleBundle
import java.util.*

class Kick: LocalizedGuildOnlyCommand {
    override val name = "kick"
    override val userGroup = Command.UserGroup.PERMISSION
    override val permission = Permission.KICK_MEMBERS
    override val cmdType = Command.CommandGroup.MODER
    override val requiredPermission: Permission? = Permission.KICK_MEMBERS

    override suspend fun action(bot: Bot, message: Message, texts: CommandLocaleBundle): CombinedMessageEmbed {
        if (message.words.size < 2) return texts.getErrorString().toMessage()
        val mentionedUserId = message.words[1].extractMentionedId()
            ?: return texts.getErrorString().toMessage()

        val mentionedUser = message.usersMentioned.find { it.id == mentionedUserId }
            ?: return texts.getErrorString().toMessage()

        if (mentionedUser.id == message.authorId)
            return texts.formatString("whitelisted.author", mentionedUser.mention).toMessage()
        if (bot.isMe(mentionedUser.id))
            return texts.formatString("whitelisted.self", mentionedUser.mention).toMessage()

        val client = bot.clientStore.guilds[message.guildId ?: return texts.getErrorString().toMessage()]

        if (client.getMember(mentionedUser.id).isAdmin(client.getCached(), mentionedUser.id)) return texts.getString("whitelisted").toMessage()

        client.removeMember(mentionedUser.id)
        return texts.formatString("done", mentionedUser.mention).toMessage()
    }
}