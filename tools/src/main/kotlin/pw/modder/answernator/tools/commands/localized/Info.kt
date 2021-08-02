package pw.modder.answernator.tools.commands.localized

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.apache.commons.io.FileUtils
import pw.modder.answernator.db.guild.Config
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.LocalizedGuildCommand
import pw.modder.answernator.utils.extensions.*
import pw.modder.answernator.utils.extensions.kord.*
import pw.modder.answernator.utils.locale.CommandLocaleBundle

class Info: LocalizedGuildCommand {
    override val name = "info"
    override val userGroup = Command.UserGroup.PERMISSION
    override val permission = Permission.ManageGuild
    override val cmdType = Command.CommandGroup.ADMIN

    private fun CommandLocaleBundle.getPerm(perm: Permission): String {
        return getOrNull("permission.${perm.name}") ?: perm.name
    }

    override suspend fun action(message: Message, args: List<String>, guild: Guild, texts: CommandLocaleBundle, config: Config) {
        val referencedMessage = message.referencedMessage ?: message.messageReference?.message?.asMessage()
        val msgRef = when {
            referencedMessage == null -> 0
            message.mentionedUserIds.size == 1 -> 0
            else -> 1
        }

        val mentionedChannels = when {
            message.mentionedChannelIds.isNotEmpty() -> message.mentionedChannelIds
            args.isEmpty() -> setOf()
            args.first().isChannelMention() -> setOf(Snowflake(args.first().extractMentionedId()!!))
            else -> setOf()
        }

        if (message.mentionedUserIds.size + message.mentionedRoleIds.size + mentionedChannels.size + msgRef != 1) {
            message.reply(texts.error())
            println("Early return")
            return
        }

        when {
            referencedMessage != null -> with(referencedMessage) message@{
                message.replyEmbed {
                    title = texts["message.title"].format(this@message.author?.tag ?: "Unkwon")
                    description = this@message.content.takeIf { it.length < 501 } ?: (this@message.content.take(499) + "…")

                    field(texts["message.created"], false) { "${this@message.id.timestampMention} (${this@message.id.relTimestampMention})" }
                    field(texts["message.embed"], true) { texts["bool.${data.embeds.isNotEmpty()}"] }
                    field(texts["message.pinned"], true) { texts["bool.${data.pinned}"] }
                    field(texts["message.webhook"], true) { texts["bool.${data.webhookId.asOptional.hasValue()}"] }
                    field(texts["message.type"], true) { texts.getOrNull("message.type.${data.type::class.simpleName}") ?: data.type::class.simpleName ?: "Unkwon" }

                    field(texts["message.mentions"], false) {
                        texts["message.mentions.text"].format(
                            texts["bool.${data.mentionEveryone}"],
                            data.mentions.takeIf { it.size < 9 }?.joinToString(separator = " ") { it.asString.toUserMention() }?.ifEmpty { texts["empty"] } ?: data.mentions.size.toString(),
                            data.mentionRoles.takeIf { it.size < 9 }?.joinToString(separator = " ") { it.asString.toRoleMention() }?.ifEmpty { texts["empty"] } ?: data.mentionRoles.size.toString(),
                            data.mentionedChannels.value?.takeIf { it.size < 9 }?.joinToString(separator = " ") { it.asString.toChannelMention() }?.ifEmpty { texts["empty"] } ?: data.mentionedChannels.value?.size?.toString() ?: "0"
                        )
                    }
                    field(texts["message.attachments"], false) {
                        data.attachments.joinToString(separator = "\n") {
                            "**[${it.filename}](${it.url})** (${FileUtils.byteCountToDisplaySize(it.size.toLong())})"
                        }.ifEmpty { texts["empty"] }
                    }

                    field(texts["message.reactions"], false) {
                        (data.reactions.value?.count() ?: 0).toString()
                    }

                    data.referencedMessage.ifHasValue { ref ->
                        field(texts["message.reference"], false) {
                            "[${ref.content.takeIf { it.length < 129 } ?: (ref.content.take(127) + "…")}](https://discord.com/channels/${ref.guildId.value?.asString ?: "@me"}/${ref.channelId.asString}/${ref.id.asString})"
                        }
                    }

                    data.flags.ifHasValue { flags ->
                        field(texts["message.flags"], false) {
                            flags.flags.joinToString(separator = ", ") { texts.getOrNull("message.flags.${it::class.simpleName}") ?: it::class.simpleName ?: "Unkwon" }.ifEmpty { texts["empty"] }
                        }
                    }

                    timestampNow()
                }
            }
            message.mentionedUserIds.isNotEmpty() -> with(message.mentionedUsers.firstOrNull() ?: message.kord.getUser(message.mentionedUserIds.first())!!) {
                val member = asMemberOrNull(guild.id)
                if (member == null) {
                    message.reply(texts["user.not.member"])
                    return
                }

                message.replyEmbed {
                    title = texts["user.title.${if (isBot) "bot" else "user"}"]
                        .format(member.nickname ?: username)
                    thumbnail { url = avatar.url }
                    color = member.getColor()

                    field(texts["user.username"], true) { tag }
                    field(texts["user.id"], true) { id.asString }
                    field(texts["user.owner"], true) { texts["bool.${id == guild.ownerId}"] }
                    field(texts["user.admin"], true) { texts["bool.${member.isAdmin()}"] }
                    field(texts["user.muted"], true) { texts["bool.${member.isMuted()}"] }
                    field(texts["user.superuser"], true) {
                        texts["bool.${id.asString == Globals.config.author}"]
                    }
                    field(texts["user.roles"], false) {
                        member.roles.toList().sortedByDescending { it.rawPosition }.joinToString(" ") { it.mention }
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
                        "${id.timestampMention} (${id.relTimestampMention})"
                    }

                    timestampNow()
                }
            }
            mentionedChannels.isNotEmpty() -> with(message.mentionedChannels.firstOrNull() ?: guild.getChannel(mentionedChannels.first())) {
                message.replyEmbed {
                    title = data.icon.orElse("") + data.name.orElse { texts["channel.title"] }

                    field(texts["channel.created"], false) { "${data.id.timestampMention} (${data.id.relTimestampMention})" }
                    field(texts["channel.id"], true) { data.id.asString }
                    field(texts["channel.type"], true) { data.type::class.simpleName ?: texts["channel.unknown"] }
                    data.parentId?.asOptional?.ifHasValue { parent ->
                        guild.channels.firstOrNull { it.id == parent }?.run {
                            field(texts["channel.parent"], true) { data.icon.orElse("#") + name }
                        }
                    }
                    data.nsfw.asOptional.ifHasValue {
                        field(texts["channel.nsfw"], true) { texts["bool.$it"] }
                    }
                    data.position.asOptional.ifHasValue {
                        field(texts["channel.position"], true) { it.toString() }
                    }
                    data.bitrate.asOptional.ifHasValue {
                        field(texts["channel.bitrate"], true) { texts["channel.bitrate.value"].format(it/1000) }
                    }
                    data.userLimit.asOptional.ifHasValue {
                        field(texts["channel.limit"], true) { it.toString() }
                    }
                    data.rateLimitPerUser.asOptional.ifHasValue {
                        field(texts["channel.rate"], true) { it.toString() }
                    }
                    data.topic.ifHasValue {
                        field(texts["channel.topic"], false) { it }
                    }

                    timestampNow()
                }
            }
            message.mentionedRoleIds.isNotEmpty() -> with(message.mentionedRoles.firstOrNull() ?: message.getGuild().getRole(message.mentionedRoleIds.first())) role@{
                message.replyEmbed {
                    title = texts["role.title"].format(name)

                    this.color = this@role.color.takeIf { it.rgb != 0 }

                    field(texts["role.id"], true) { this@role.id.asString }
                    field(texts["role.default"], true) {
                        texts["bool.${config.defaultRole == this@role.id.asString}"]
                    }
                    field(texts["role.mute"],true) {
                        texts["bool.${config.muteRole == this@role.id.asString}"]
                    }
                    field(texts["role.managed"], true) { texts["bool.$managed"] }
                    field(texts["role.mentionable"], true) { texts["bool.$mentionable"] }
                    field(texts["role.hoist"], true) { texts["bool.$hoisted"] }
                    field(texts["role.position"], true) { rawPosition.toString() }
                    field(texts["role.createdAt"], false) {
                        "${this@role.id.timestampMention} (${this@role.id.relTimestampMention})"
                    }
                    field(texts["role.rights"], false) {
                        permissions.values.joinToString(", ") { texts.getPerm(it) }.ifEmpty { texts["empty"] }
                    }

                    timestampNow()
                }
            }
            else -> message.reply(texts.error())
        }
    }
}