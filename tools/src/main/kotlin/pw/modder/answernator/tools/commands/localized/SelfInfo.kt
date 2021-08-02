package pw.modder.answernator.tools.commands.localized

import dev.kord.common.entity.Permission
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import pw.modder.answernator.db.guild.Config
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.LocalizedGuildCommand
import pw.modder.answernator.utils.TimestampFormat
import pw.modder.answernator.utils.extensions.kord.*
import pw.modder.answernator.utils.locale.CommandLocaleBundle
import java.util.*

class SelfInfo: LocalizedGuildCommand {
    override fun getTexts(locale: Locale): CommandLocaleBundle {
        return CommandLocaleBundle("info", locale, javaClass.classLoader)
    }

    private fun CommandLocaleBundle.getPerm(perm: Permission): String {
        return getOrNull("permission.${perm.name}") ?: perm.name
    }

    override suspend fun action(message: Message, args: List<String>, guild: Guild, texts: CommandLocaleBundle, config: Config) {
        val member = message.getAuthorAsMember()
        if (member == null) {
            message.reply(texts.error())
            return
        }

        message.replyEmbed {
            title = texts["user.title.user"].format(member.nickname ?: member.username)

            thumbnail { url = member.avatar.url }

            color = member.getColor()

            field(texts["user.username"], true) { member.tag }
            field(texts["user.id"], true) { member.id.asString }
            field(texts["user.owner"], true) { texts["bool.${member.id == guild.ownerId}"] }
            field(texts["user.admin"], true) { texts["bool.${member.isAdmin()}"] }
            field(texts["user.muted"], true) {
                texts["bool.${member.isMuted()}"]
            }
            field(texts["user.superuser"], true) {
                texts["bool.${member.id.asString == Globals.config.author}"]
            }
            field(texts["user.roles"], false) {
                member.roleBehaviors.joinToString(" ") { it.mention }
                    .ifEmpty { texts["empty"] }
            }
            field(texts["user.rights"], false) {
                member.getPermissions().values.joinToString(", ") { texts.getPerm(it) }
                    .ifEmpty { texts["empty"] }
            }
            field(texts["user.joinedAt"], false) {
                "<t:${member.joinedAt.epochSeconds}:f> (<t:${member.joinedAt.epochSeconds}:R>)"
            }
            field(texts["user.createdAt"], false) {
                "${member.id.timestampMention} (${member.id.timestampMention(TimestampFormat.RELATIVE)})"
            }

            timestampNow()
        }
    }

    override val name = "selfinfo"
}