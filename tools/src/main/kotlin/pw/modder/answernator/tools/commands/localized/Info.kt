package pw.modder.answernator.tools.commands.localized

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.model.Permission
import com.jessecorbett.diskord.api.rest.EmbedImage
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.util.toRoleMention
import com.jessecorbett.diskord.dsl.message as dslmessage
import kotlinx.serialization.UnstableDefault
import pw.modder.answernator.tools.commandTypes.LocalizedGuildOnlyCommand
import pw.modder.answernator.utils.Command
import pw.modder.answernator.cache.GuildCache.getCached
import pw.modder.answernator.db.Db
import pw.modder.answernator.db.Db.memberIsMuted
import pw.modder.answernator.utils.Utils
import pw.modder.answernator.utils.extensions.*
import java.util.*

@UnstableDefault
class Info: LocalizedGuildOnlyCommand {
    override val name = "info"
    override val userGroup = Command.UserGroup.PERMISSION
    override val permission = Permission.MANAGE_GUILD
    override val cmdType = Command.CommandGroup.ADMIN

    override suspend fun action(bot: Bot, message: Message, texts: ResourceBundle): CombinedMessageEmbed {
        if (message.usersMentioned.size + message.rolesIdsMentioned.size != 1) return texts.errorMessage()
        val guildId = message.guildId ?: return texts.errorMessage()
        val guild = bot.clientStore.guilds[guildId].getCached()

        message.usersMentioned.singleOrNull()?.run {
            val member = bot.clientStore.guilds[guildId].getMember(id)
            return dslmessage {
                title = member.nickname ?: username

                avatarHash?.run {
                    thumbnail = EmbedImage("https://cdn.discordapp.com/avatars/$id/$this")
                }

                member.getColor(guild).takeUnless { it == 0 }?.run { color = this }

                field(texts.getStringOrKey("user.username"), username, true)
                field(texts.getStringOrKey("user.id"), id, true)
                field(texts.getStringOrKey("user.owner"), texts.getStringOrKey("bool.${id == guild.ownerId}"), true)
                field(texts.getStringOrKey("user.admin"), texts.getStringOrKey("bool.${member.isAdmin(guild, id)}"), true)
                field(texts.getStringOrKey("user.muted"), texts.getStringOrKey("bool.${guild.memberIsMuted(id)}"), true)

                field(texts.getStringOrKey("user.roles"), member.roleIds.joinToString(" ") { it.toRoleMention() }.ifEmpty { texts.getStringOrKey("empty") }, false)
                field(texts.getStringOrKey("user.rights"), member.computePermissions(guild, id).asList().joinToString(", ") { texts.getStringOrKey("permission.${it.name}") }.ifEmpty { texts.getStringOrKey("empty") }, false)
                field(texts.getStringOrKey("user.joinedAt"), texts.formatString("user.joinedAt.value", Utils.prettyPrintTime(texts.locale, member.joinedAt)), false)
                field(texts.getStringOrKey("user.createdAt"), texts.formatString("user.createdAt.value", Utils.prettyPrintTime(texts.locale, createdAt)), false)

                setCurrentTimestamp()
            }
        }

        message.rolesIdsMentioned.singleOrNull()?.let { roleId ->
            guild.roles.single { role -> role.id == roleId }
        }?.run {
            val config = Db.guilds.get(guildId)

            return dslmessage {
                title = name

                if (this@run.color != 0) color = this@run.color

                field(texts.getStringOrKey("role.id"), id, true)
                field(texts.getStringOrKey("role.default"), texts.getStringOrKey("bool.${config.defaultRole == id}"), true)
                field(texts.getStringOrKey("role.mute"), texts.getStringOrKey("bool.${config.muteRole == id}"), true)
                field(texts.getStringOrKey("role.managed"), texts.getStringOrKey("bool.$isManagedByIntegration"), true)
                field(texts.getStringOrKey("role.mentionable"), texts.getStringOrKey("bool.$isMentionable"), true)
                field(texts.getStringOrKey("role.hoist"), texts.getStringOrKey("bool.$isUserListPinned"), true)
                field(texts.getStringOrKey("role.position"), position.toString(), true)

                field(texts.getStringOrKey("role.rights"), permissions.asList().joinToString(", ") { texts.getStringOrKey("permission.${it.name}") }.ifEmpty { texts.getStringOrKey("empty") }, false)

            }
        }

        return texts.errorMessage()
    }
}