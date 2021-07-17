package pw.modder.answernator.tools.commands.localized

import dev.kord.common.entity.Permission
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import pw.modder.answernator.db.Db
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.LocalizedGuildCommand
import pw.modder.answernator.utils.Utils
import pw.modder.answernator.utils.extensions.kord.*
import pw.modder.answernator.utils.locale.CommandLocaleBundle

class Info: LocalizedGuildCommand {
    override val name = "info"
    override val userGroup = Command.UserGroup.PERMISSION
    override val permission = Permission.ManageGuild
    override val cmdType = Command.CommandGroup.ADMIN

    private fun CommandLocaleBundle.getPerm(perm: Permission): String {
        return getNullableString("permission.${perm.name}") ?: perm.name
    }

    override suspend fun action(message: Message, args: List<String>, guild: Guild, texts: CommandLocaleBundle) {
        val msgRef = when (message.referencedMessage) {
            null -> 0
            else -> 1
        }
        if (message.mentionedUserIds.size + message.mentionedRoleIds.size + message.mentionedChannelIds.size + msgRef != 1) {
            message.reply(texts.getErrorString())
            return
        }

        when {
            message.mentionedUserIds.isNotEmpty() -> with(message.mentionedUsers.first()) {
                val member = asMemberOrNull(guild.id)
                if (member == null) {
                    message.reply(texts.getString("user.not.member"))
                    return
                }

                message.reply {
                    embed {
                        title = texts.formatString(
                            "user.title.${if (isBot) "bot" else "user"}",
                            member.nickname ?: username
                        )
                        thumbnail { url = avatar.url }
                        color = member.getColor()

                        field(texts.getString("user.username"), true) { tag }
                        field(texts.getString("user.id"), true) { id.asString }
                        field(texts.getString("user.owner"), true) { texts.getString("bool.${id == guild.ownerId}") }
                        field(texts.getString("user.admin"), true) { texts.getString("bool.${member.isAdmin()}") }
                        field(texts.getString("user.muted"), true) {
                            texts.getString("bool.${member.isMuted()}")
                        }
                        field(texts.getString("user.superuser"), true) {
                            texts.getString("bool.${id.asString == Globals.config.author}")
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
                            texts.formatString(
                                "user.joinedAt.value",
                                Utils.prettyPrintPeriod(texts.locale, member.joinedAt)
                            )
                        }
                        field(texts.getString("user.createdAt"), false) {
                            texts.formatString(
                                "user.createdAt.value",
                                Utils.prettyPrintPeriodSnowflake(texts.locale, id)
                            )
                        }
                        timestamp = Clock.System.now()
                    }
                    allowedMentions { repliedUser = false }
                }
            }
            message.mentionedChannelIds.isNotEmpty() -> with(message.mentionedChannels.first()) {
                TODO("Channel info")
                message.reply {
                    embed {

                    }
                    allowedMentions { repliedUser = false }
                }
            }
            message.mentionedRoleIds.isNotEmpty() -> with(message.mentionedRoles.first()) role@{
                message.reply {
                    embed {
                        val config = Db.getGuildConfig(guildId)

                        title = texts.formatString("role.title", name)

                        this.color = this@role.color.takeIf { it.rgb != 0 }

                        field(texts.getString("role.id"), true) { this@role.id.asString }
                        field(texts.getString("role.default"), true) { texts.getString("bool.${config.defaultRole == this@role.id.asString}") }
                        field(texts.getString("role.mute"), true) { texts.getString("bool.${config.muteRole == this@role.id.asString}") }
                        field(texts.getString("role.managed"), true) { texts.getString("bool.$managed") }
                        field(texts.getString("role.mentionable"), true) { texts.getString("bool.$mentionable") }
                        field(texts.getString("role.hoist"), true) { texts.getString("bool.$hoisted") }
                        field(texts.getString("role.position"), true) { rawPosition.toString() }
                        field(texts.getString("role.createdAt"), false) {
                            texts.formatString("role.createdAt.value", Utils.prettyPrintPeriodSnowflake(texts.locale, id))
                        }
                        field(texts.getString("role.rights"), false) {
                            permissions.values.joinToString(", ") { texts.getPerm(it) }.ifEmpty { texts.getString("empty") }
                        }
                    }
                    allowedMentions { repliedUser = false }
                }
            }
            message.referencedMessage != null -> with(message.referencedMessage!!) {
                TODO("Message info")
                message.reply {
                    embed {

                    }
                    allowedMentions { repliedUser = false }
                }
            }
            else -> message.reply(texts.getErrorString())
        }
    }
}