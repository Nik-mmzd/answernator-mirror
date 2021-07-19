package pw.modder.answernator.tools.commands.localized

import dev.kord.common.entity.Permission
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import pw.modder.answernator.db.guild.Config
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.LocalizedGuildCommand
import pw.modder.answernator.utils.extensions.kord.*
import pw.modder.answernator.utils.locale.CommandLocaleBundle
import java.util.*

class SelfInfo: LocalizedGuildCommand {
    override fun getTexts(locale: Locale): CommandLocaleBundle {
        return CommandLocaleBundle("info", locale, javaClass.classLoader)
    }

    private fun CommandLocaleBundle.getPerm(perm: Permission): String {
        return getNullableString("permission.${perm.name}") ?: perm.name
    }

    override suspend fun action(message: Message, args: List<String>, guild: Guild, texts: CommandLocaleBundle, config: Config) {
        val member = message.getAuthorAsMember()
        if (member == null) {
            message.reply(texts.getErrorString())
            return
        }

        message.replyEmbed {
            title = texts.formatString("user.title.user", member.nickname ?: member.username)

            thumbnail { url = member.avatar.url }

            color = member.getColor()

            field(texts.getString("user.username"), true) { member.tag }
            field(texts.getString("user.id"), true) { member.id.asString }
            field(texts.getString("user.owner"), true) { texts.getString("bool.${member.id == guild.ownerId}") }
            field(texts.getString("user.admin"), true) { texts.getString("bool.${member.isAdmin()}") }
            field(texts.getString("user.muted"), true) {
                texts.getString("bool.${member.isMuted()}")
            }
            field(texts.getString("user.superuser"), true) {
                texts.getString("bool.${member.id.asString == Globals.config.author}")
            }
            field(texts.getString("user.roles"), false) {
                member.roleBehaviors.joinToString(" ") { it.mention }
                    .ifEmpty { texts.getString("empty") }
            }
            field(texts.getString("user.rights"), false) {
                member.getPermissions().values.joinToString(", ") { texts.getPerm(it) }
                    .ifEmpty { texts.getString("empty") }
            }
            field(texts.getString("user.joinedAt"), false) {
                "<t:${member.joinedAt.epochSeconds}:f> (<t:${member.joinedAt.epochSeconds}:R>)"
            }
            field(texts.getString("user.createdAt"), false) {
                "${member.id.timestampMention} (${member.id.relTimestampMention})"
            }

            timestampNow()
        }
    }

    override val name = "selfinfo"
}