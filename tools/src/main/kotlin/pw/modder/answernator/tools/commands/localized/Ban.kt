package pw.modder.answernator.tools.commands.localized

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.model.Permission
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.util.mention
import com.jessecorbett.diskord.util.words
import kotlinx.serialization.UnstableDefault
import pw.modder.answernator.tools.commandTypes.LocalizedGuildOnlyCommand
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.extensions.isUserMention
import pw.modder.answernator.utils.extensions.toUserMention
import pw.modder.answernator.cache.GuildCache.getCached
import pw.modder.answernator.utils.extensions.isAdmin
import java.util.*

@UnstableDefault
class Ban: LocalizedGuildOnlyCommand {
    override val name = "ban"
    override val userGroup = Command.UserGroup.PERMISSION
    override val permission = Permission.BAN_MEMBERS

    override suspend fun action(bot: Bot, message: Message, texts: ResourceBundle): CombinedMessageEmbed {
        if (message.words.getOrNull(1)?.isUserMention() != true || message.usersMentioned.size != 1) return texts.errorMessage()
        val client = bot.clientStore.guilds[message.guildId ?: return texts.errorMessage()]
        val memberId = message.usersMentioned.single().takeIf { !client.getMember(it.id).isAdmin(client.getCached(), it.id) }?.id
            ?: return texts.message("whitelisted")

        val reason = message.words.drop(2).joinToString(" ")
        val realReason = if (reason.isEmpty())
            texts.formatString("banned", message.author.mention)
        else
            texts.formatString("banned.reason", message.author.mention, reason)

        client.createBan(memberId, 0, realReason)
        return texts.message("done", memberId.toUserMention())
    }
}