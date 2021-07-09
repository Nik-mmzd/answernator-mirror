package pw.modder.answernator.tools.commands.localized

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.model.Permission
import com.jessecorbett.diskord.api.rest.EmbedImage
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.util.toRoleMention
import org.jetbrains.exposed.sql.transactions.transaction
import pw.modder.answernator.cache.GuildCache.getCached
import pw.modder.answernator.db.Db
import pw.modder.answernator.tools.commandTypes.LocalizedGuildOnlyCommand
import pw.modder.answernator.tools.helper.computeRealPermissions
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.Utils
import pw.modder.answernator.utils.extensions.*
import pw.modder.answernator.utils.locale.CommandLocaleBundle
import com.jessecorbett.diskord.dsl.message as dslmessage

class Info: LocalizedGuildOnlyCommand {
    override val name = "info"
    override val userGroup = Command.UserGroup.PERMISSION
    override val permission = Permission.MANAGE_GUILD
    override val cmdType = Command.CommandGroup.ADMIN

    private fun CommandLocaleBundle.getPerm(perm: Permission): String {
        return getNullableString("permission.${perm.name}") ?: perm.name.toUpperCase()
    }

    override suspend fun action(bot: Bot, message: Message, texts: CommandLocaleBundle): CombinedMessageEmbed {
        if (message.usersMentioned.size + message.rolesIdsMentioned.size != 1) return texts.getErrorString().toMessage()
        val guildId = message.guildId ?: return texts.getErrorString().toMessage()
        val guild = bot.clientStore.guilds[guildId].getCached()

        message.usersMentioned.singleOrNull()?.run {
            val member = bot.clientStore.guilds[guildId].getMember(id)
            return dslmessage {
                title = texts.formatString(
                    "user.title.${if (isBot) "bot" else "user"}",
                    member.nickname ?: username
                )

                avatarHash?.run {
                    thumbnail = EmbedImage("https://cdn.discordapp.com/avatars/$id/$this")
                }

                member.getColor(guild).takeUnless { it == 0 }?.run { color = this }

                field(texts.getString("user.username"), username, true)
                field(texts.getString("user.id"), id, true)
                field(texts.getString("user.owner"), texts.getString("bool.${id == guild.ownerId}"), true)
                field(texts.getString("user.admin"), texts.getString("bool.${member.isAdmin(guild, id)}"), true)
                field(texts.getString("user.muted"), texts.getString("bool.${Db.isMuted(guild.id, id)}"), true)
                field(texts.getString("user.superuser"), texts.getString("bool.${id == Globals.config.author}"), true)

                field(texts.getString("user.roles"), member.roleIds.joinToString(" ") { it.toRoleMention() }.ifEmpty { texts.getString("empty") }, false)
                field(texts.getString("user.rights"), member.computeRealPermissions(guild, id).asList().joinToString(", ") { texts.getPerm(it) }.ifEmpty { texts.getString("empty") }, false)
                field(texts.getString("user.joinedAt"), texts.formatString("user.joinedAt.value", Utils.prettyPrintPeriod(texts.locale, member.joinedAt)), false)
                field(texts.getString("user.createdAt"), texts.formatString("user.createdAt.value", Utils.prettyPrintPeriodSnowflake(texts.locale, id)), false)

                setCurrentTimestamp()
            }
        }

        message.rolesIdsMentioned.singleOrNull()?.let { roleId ->
            guild.roles.singleOrNull { role -> role.id == roleId }
        }?.run {
            val config = Db.getGuildConfig(guildId)

            return dslmessage {
                title = texts.formatString("role.title", name)

                if (this@run.color != 0) color = this@run.color

                field(texts.getString("role.id"), id, true)
                field(texts.getString("role.default"), texts.getString("bool.${config.defaultRole == id}"), true)
                field(texts.getString("role.mute"), texts.getString("bool.${config.muteRole == id}"), true)
                field(texts.getString("role.managed"), texts.getString("bool.$isManagedByIntegration"), true)
                field(texts.getString("role.mentionable"), texts.getString("bool.$isMentionable"), true)
                field(texts.getString("role.hoist"), texts.getString("bool.$isUserListPinned"), true)
                field(texts.getString("role.position"), position.toString(), true)
                field(texts.getString("role.createdAt"), texts.formatString("role.createdAt.value", Utils.prettyPrintPeriodSnowflake(texts.locale, id)), false)

                field(texts.getString("role.rights"), permissions.asList().joinToString(", ") { texts.getPerm(it) }.ifEmpty { texts.getString("empty") }, false)

            }
        }

        return texts.getErrorString().toMessage()
    }
}