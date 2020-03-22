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
import pw.modder.answernator.utils.RelativeTime
import pw.modder.answernator.utils.extensions.*
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@UnstableDefault
class Info: LocalizedGuildOnlyCommand {
    override val name = "info"
    override val userGroup = Command.UserGroup.PERMISSION
    override val permission = Permission.MANAGE_GUILD

    override suspend fun action(bot: Bot, message: Message, texts: ResourceBundle): CombinedMessageEmbed {
        if (message.usersMentioned.size + message.rolesIdsMentioned.size != 1) return texts.errorMessage()
        val guildId = message.guildId ?: return texts.errorMessage()
        val guild = bot.clientStore.guilds[guildId].getCached()

        message.usersMentioned.singleOrNull()?.run {
            val member = bot.clientStore.guilds[guildId].getMember(id)
            val joined = RelativeTime.between(OffsetDateTime.now(), OffsetDateTime.parse(member.joinedAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME))

            return dslmessage {
                title = member.nickname ?: name.ifEmpty { username }

                avatarHash?.run {
                    thumbnail = EmbedImage("https://cdn.discordapp.com/avatars/$id/$this")
                }

                color = member.getColor(guild)

                field(texts.getStringOrKey("user.username"), username, true)
                field(texts.getStringOrKey("user.id"), id, true)
                field(texts.getStringOrKey("user.owner"), texts.getStringOrKey("bool.${id == guild.ownerId}"), true)
                field(texts.getStringOrKey("user.admin"), texts.getStringOrKey("bool.${member.isAdmin(guild, id)}"), true)
                field(texts.getStringOrKey("user.muted"), texts.getStringOrKey("bool.${guild.memberIsMuted(id)}"), true)

                field(texts.getStringOrKey("user.roles"), member.roleIds.joinToString(" ") { it.toRoleMention() }, false)
                field(texts.getStringOrKey("user.rights"), member.computePermissions(guild, id).asList().joinToString(", ") { texts.getStringOrKey("permission.${it.name}") }, false)
                field(texts.getStringOrKey("user.joinedAt"), texts.formatString("user.joinedAt.value", joinedAtFormatter(texts, joined)), false)

                setCurrentTimestamp()
            }
        }

        message.rolesIdsMentioned.singleOrNull()?.let { roleId ->
            guild.roles.single { role -> role.id == roleId }
        }?.run {
            val config = Db.guilds.get(guildId)

            return dslmessage {
                title = name

                color = this@run.color

                field(texts.getStringOrKey("role.id"), id, true)
                field(texts.getStringOrKey("role.default"), texts.getStringOrKey("bool.${config.defaultRole == id}"), true)
                field(texts.getStringOrKey("role.mute"), texts.getStringOrKey("bool.${config.muteRole == id}"), true)
                field(texts.getStringOrKey("role.managed"), texts.getStringOrKey("bool.$isManagedByIntegration"), true)
                field(texts.getStringOrKey("role.mentionable"), texts.getStringOrKey("bool.$isMentionable"), true)
                field(texts.getStringOrKey("role.hoist"), texts.getStringOrKey("bool.$isUserListPinned"), true)
                field(texts.getStringOrKey("role.position"), position.toString(), true)

                field(texts.getStringOrKey("role.rights"), permissions.asList().joinToString(", ") { texts.getStringOrKey("permission.${it.name}") }, false)

            }
        }

        return texts.errorMessage()
    }

    private fun joinedAtFormatter(texts: ResourceBundle, relativeTime: RelativeTime): String {
        val answer = mutableListOf<String>()
        var first = true

        if (relativeTime.years > 0) {
            answer + texts.formatString("user.joinedAt.years", relativeTime.years)
            first = false
        }
        if (relativeTime.months > 0 || !first) {
            answer + texts.formatString("user.joinedAt.months", relativeTime.months)
            first = false
        }
        if (relativeTime.days > 0 || !first) {
            answer + texts.formatString("user.joinedAt.days", relativeTime.days)
            first = false
        }
        if (relativeTime.hours > 0 || !first) {
            answer + texts.formatString("user.joinedAt.hours", relativeTime.hours)
            first = false
        }
        if (relativeTime.minutes > 0 || !first) {
            answer + texts.formatString("user.joinedAt.minutes", relativeTime.minutes)
        }
        answer + texts.formatString("user.joinedAt.seconds", relativeTime.seconds)

        return answer.joinToString(" ")
    }
}