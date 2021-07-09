package pw.modder.answernator.tools.commands.localized

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.model.Permission
import com.jessecorbett.diskord.api.rest.EmbedImage
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.dsl.message
import com.jessecorbett.diskord.util.authorId
import com.jessecorbett.diskord.util.toRoleMention
import org.jetbrains.exposed.sql.transactions.transaction
import pw.modder.answernator.cache.GuildCache.getCached
import pw.modder.answernator.db.Db
import pw.modder.answernator.tools.helper.computeRealPermissions
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.LocalizedGuildCommand
import pw.modder.answernator.utils.Utils
import pw.modder.answernator.utils.extensions.*
import pw.modder.answernator.utils.locale.CommandLocaleBundle
import java.util.*

class SelfInfo: LocalizedGuildCommand {
    override fun getTexts(locale: Locale): CommandLocaleBundle {
        return CommandLocaleBundle("info", locale, javaClass.classLoader)
    }

    private fun CommandLocaleBundle.getPerm(perm: Permission): String {
        return getNullableString("permission.${perm.name}") ?: perm.name.toUpperCase()
    }

    override suspend fun action(
        bot: Bot,
        message: Message,
        texts: CommandLocaleBundle,
        guildId: String
    ): CombinedMessageEmbed {
        val member = message.partialMember
            ?: return texts.getString("error.nomember").toMessage()
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

            field(texts.getString("user.username"), message.author.username, true)
            field(texts.getString("user.id"), message.author.id, true)
            field(texts.getString("user.owner"), texts.getString("bool.${message.author.id == guild.ownerId}"), true)
            field(texts.getString("user.admin"), texts.getString("bool.${member.isAdmin(guild, message.author.id)}"), true)
            field(texts.getString("user.muted"), texts.getString("bool.${transaction { Db.getGuildConfig(guildId).mutes.any { it.memberId == message.authorId }}}"), true)
            field(texts.getString("user.superuser"), texts.getString("bool.${message.author.id == Globals.config.author}"), true)

            field(texts.getString("user.roles"), member.roleIds.joinToString(" ") { it.toRoleMention() }.ifEmpty { texts.getString("empty") }, false)
            field(texts.getString("user.rights"), member.computeRealPermissions(guild, message.author.id).asList().joinToString(", ") { texts.getPerm(it) }.ifEmpty { texts.getString("empty") }, false)
            field(texts.getString("user.joinedAt"), texts.formatString("user.joinedAt.value", Utils.prettyPrintPeriod(texts.locale, member.joinedAt)), false)
            field(texts.getString("user.createdAt"), texts.formatString("user.createdAt.value", Utils.prettyPrintPeriodSnowflake(texts.locale, message.author.id)), false)

            setCurrentTimestamp()
        }
    }

    override val name = "selfinfo"
}