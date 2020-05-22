package pw.modder.answernator.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.model.Permission
import com.jessecorbett.diskord.api.model.Permissions
import com.jessecorbett.diskord.api.rest.client.GuildClient
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.util.mention
import com.jessecorbett.diskord.util.words
import pw.modder.answernator.cache.GuildCache.getCached
import pw.modder.answernator.db.Db
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.LocalizedCommand
import pw.modder.answernator.utils.extensions.isAdmin
import java.util.*

class DefRole: LocalizedCommand {
    override val name = "defrole"
    override val channels = EnumSet.of(Command.ChannelTypes.GUILD)
    override val userGroup = Command.UserGroup.PERMISSION
    override val permission = Permission.MANAGE_MESSAGES
    override val cmdType = Command.CommandGroup.MODER
    override val requiredPermission: Permission? = Permission.MANAGE_ROLES

    override suspend fun check(message: Message, guildClient: GuildClient?): Boolean {
        val cfg = Db.guilds.get(message.guildId ?: return false) ?: return false
        return cfg.greetingsChannel.isNotEmpty() && super.check(message, guildClient)
    }

    override fun check(message: Message, permissions: Permissions): Boolean {
        val cfg = Db.guilds.get(message.guildId ?: return false) ?: return false
        return cfg.greetingsChannel.isNotEmpty() && super.check(message, permissions)
    }

    override suspend fun action(bot: Bot, message: Message, texts: ResourceBundle): CombinedMessageEmbed {
        val gid = message.guildId ?: return textMessage(texts.getStringOrKey("error"))
        val guild = bot.clientStore.guilds[gid]
        val config = Db.guilds.get(gid)
        val add = when(message.words.getOrNull(1)) {
            "add" -> true
            "remove" -> false
            else -> return textMessage(texts.getStringOrKey("error"))
        }

        val guildObject = guild.getCached()
        val mentionedUsers = message.usersMentioned.filterNot { guild.getMember(it.id).isAdmin(guildObject, it.id) }

        mentionedUsers.forEach {
            when(add) {
                true -> guild.addMemberRole(it.id, config.defaultRole)
                false -> guild.removeMemberRole(it.id, config.defaultRole)
            }
        }
        return textMessage(texts.formatString("done.$add", mentionedUsers.joinToString(" ") { it.mention }))
    }
}