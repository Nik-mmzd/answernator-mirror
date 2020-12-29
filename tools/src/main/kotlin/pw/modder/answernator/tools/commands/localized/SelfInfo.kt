package pw.modder.answernator.tools.commands.localized

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.model.Permission
import com.jessecorbett.diskord.api.rest.EmbedImage
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.dsl.message
import com.jessecorbett.diskord.util.toRoleMention
import pw.modder.answernator.cache.GuildCache.getCached
import pw.modder.answernator.db.Db.memberIsMuted
import pw.modder.answernator.tools.helper.computeRealPermissions
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.LocalizedGuildCommand
import pw.modder.answernator.utils.UTF8Control
import pw.modder.answernator.utils.Utils
import pw.modder.answernator.utils.extensions.*
import java.util.*

class SelfInfo: LocalizedGuildCommand {
    override fun getTexts(locale: Locale): ResourceBundle {
        return ResourceBundle.getBundle("locale.info", locale, javaClass.classLoader, UTF8Control())
    }

    override fun ResourceBundle.getStringOrKey(key: String): String {
        return getStringSafe("info.$key") ?: "info.$key"
    }

    private fun ResourceBundle.getPermissionString(perm: Permission): String {
        return getStringSafe("info.${perm.name}") ?: "[${perm.name}]"
    }

    override suspend fun action(
        bot: Bot,
        message: Message,
        texts: ResourceBundle,
        guildId: String
    ): CombinedMessageEmbed {
        val member = message.partialMember
            ?: return texts.message("error.nomember")
        val guild = bot.clientStore.guilds[guildId].getCached()

        return message {
            title = texts.formatString(
                "user.title.user",
                member.nickname ?: message.author.username
            )

            message.author.avatarHash?.run {
                thumbnail = EmbedImage("https://cdn.discordapp.com/avatars/${message.author.id}/$this")
            }

            member.getColor(guild).takeUnless { it == 0 }?.run { color = this }

            field(texts.getStringOrKey("user.username"), message.author.username, true)
            field(texts.getStringOrKey("user.id"), message.author.id, true)
            field(texts.getStringOrKey("user.owner"), texts.getStringOrKey("bool.${message.author.id == guild.ownerId}"), true)
            field(texts.getStringOrKey("user.admin"), texts.getStringOrKey("bool.${member.isAdmin(guild, message.author.id)}"), true)
            field(texts.getStringOrKey("user.muted"), texts.getStringOrKey("bool.${guild.memberIsMuted(message.author.id)}"), true)
            field(texts.getStringOrKey("user.superuser"), texts.getStringOrKey("bool.${message.author.id == Globals.config.author}"), true)

            field(texts.getStringOrKey("user.roles"), member.roleIds.joinToString(" ") { it.toRoleMention() }.ifEmpty { texts.getStringOrKey("empty") }, false)
            field(texts.getStringOrKey("user.rights"), member.computeRealPermissions(guild, message.author.id).asList().joinToString(", ") { texts.getPermissionString(it) }.ifEmpty { texts.getStringOrKey("empty") }, false)
            field(texts.getStringOrKey("user.joinedAt"), texts.formatString("user.joinedAt.value", Utils.prettyPrintPeriod(texts.locale, member.joinedAt)), false)
            field(texts.getStringOrKey("user.createdAt"), texts.formatString("user.createdAt.value", Utils.prettyPrintPeriodSnowflake(texts.locale, message.author.id)), false)

            setCurrentTimestamp()
        }
    }

    override val name = "selfinfo"
}