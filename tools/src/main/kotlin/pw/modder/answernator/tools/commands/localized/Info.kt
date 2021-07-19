package pw.modder.answernator.tools.commands.localized

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.datetime.Clock
import org.apache.commons.io.FileUtils
import org.joda.time.Instant
import pw.modder.answernator.db.guild.Config
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.LocalizedGuildCommand
import pw.modder.answernator.utils.Utils
import pw.modder.answernator.utils.extensions.*
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
            message.reply(texts.getErrorString())
            println("Early return")
            return
        }

        when {
            referencedMessage != null -> with(referencedMessage) message@{
                message.replyEmbed {
                    title = texts.formatString("message.title", this@message.author?.tag ?: "Unkwon")
                    description = this@message.content.takeIf { it.length < 501 } ?: (this@message.content.take(499) + "…")

                    field(texts.getString("message.embed"), true) { texts.getString("bool.${data.embeds.isNotEmpty()}") }
                    field(texts.getString("message.pinned"), true) { texts.getString("bool.${data.pinned}") }
                    field(texts.getString("message.webhook"), true) { texts.getString("bool.${data.webhookId.asOptional.hasValue()}")}
                    field(texts.getString("message.type"), true) { texts.getNullableString("message.type.${data.type::class.simpleName}") ?: data.type::class.simpleName ?: "Unkwon" }

                    field(texts.getString("message.mentions"), false) {
                        texts.formatString("message.mentions.text",
                            texts.getString("bool.${data.mentionEveryone}"),
                            data.mentions.takeIf { it.size < 9 }?.joinToString(separator = " ") { it.asString.toUserMention() }?.ifEmpty { texts.getString("empty") } ?: data.mentions.size.toString(),
                            data.mentionRoles.takeIf { it.size < 9 }?.joinToString(separator = " ") { it.asString.toRoleMention() }?.ifEmpty { texts.getString("empty") } ?: data.mentionRoles.size.toString(),
                            data.mentionedChannels.value?.takeIf { it.size < 9 }?.joinToString(separator = " ") { it.asString.toChannelMention() }?.ifEmpty { texts.getString("empty") } ?: data.mentionedChannels.value?.size?.toString() ?: "0"
                        )
                    }
                    field(texts.getString("message.attachments"), false) {
                        data.attachments.joinToString(separator = "\n") {
                            "**[${it.filename}](${it.url})** (${FileUtils.byteCountToDisplaySize(it.size.toLong())})"
                        }.ifEmpty { texts.getString("empty") }
                    }

                    field(texts.getString("message.reactions"), false) {
                        (data.reactions.value?.count() ?: 0).toString()
                    }

                    data.referencedMessage.ifHasValue { ref ->
                        field(texts.getString("message.reference"), false) {
                            "[${ref.content.takeIf { it.length < 129 } ?: (ref.content.take(127) + "…")}](https://discord.com/channels/${ref.guildId.value?.asString ?: "@me"}/${ref.channelId.asString}/${ref.id.asString})"
                        }
                    }

                    data.flags.ifHasValue { flags ->
                        field(texts.getString("message.flags"), false) {
                            flags.flags.joinToString(separator = ", ") { texts.getNullableString("message.flags.${it::class.simpleName}") ?: it::class.simpleName ?: "Unkwon" }.ifEmpty { texts.getString("empty") }
                        }
                    }
                }
            }
            message.mentionedUserIds.isNotEmpty() -> with(message.mentionedUsers.firstOrNull() ?: message.kord.getUser(message.mentionedUserIds.first())!!) {
                val member = asMemberOrNull(guild.id)
                if (member == null) {
                    message.reply(texts.getString("user.not.member"))
                    return
                }

                message.replyEmbed {
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
                            Utils.prettyPrintPeriod(texts.locale, Instant.ofEpochSecond(member.joinedAt.epochSeconds))
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
            }
            mentionedChannels.isNotEmpty() -> with(message.mentionedChannels.firstOrNull() ?: guild.getChannel(mentionedChannels.first())) {
                message.replyEmbed {
                    title = data.icon.orElse("") + data.name.orElse { texts.getString("channel.title") }

                    field(texts.getString("channel.id"), true) { data.id.asString }
                    field(texts.getString("channel.type"), true) { data.type::class.simpleName ?: texts.getString("channel.unknown") }
                    data.parentId?.asOptional?.ifHasValue { parent ->
                        guild.channelBehaviors.find { it.id == parent }?.run {
                            field(texts.getString("channel.parent"), true) { data.icon.orElse("#") + name }
                        }
                    }
                    data.nsfw.asOptional.ifHasValue {
                        field(texts.getString("channel.nsfw"), true) { texts.getString("bool.$it") }
                    }
                    data.position.asOptional.ifHasValue {
                        field(texts.getString("channel.position"), true) { it.toString() }
                    }
                    data.bitrate.asOptional.ifHasValue {
                        field(texts.getString("channel.bitrate"), true) { texts.formatString("channel.bitrate.value", it/1000) }
                    }
                    data.userLimit.asOptional.ifHasValue {
                        field(texts.getString("channel.limit"), true) { it.toString() }
                    }
                    data.rateLimitPerUser.asOptional.ifHasValue {
                        field(texts.getString("channel.rate"), true) { it.toString() }
                    }
                    data.topic.ifHasValue {
                        field(texts.getString("channel.topic"), false) { it }
                    }
                }
            }
            message.mentionedRoleIds.isNotEmpty() -> with(message.mentionedRoles.firstOrNull() ?: message.getGuild().getRole(message.mentionedRoleIds.first())) role@{
                message.replyEmbed {
                    title = texts.formatString("role.title", name)

                    this.color = this@role.color.takeIf { it.rgb != 0 }

                    field(texts.getString("role.id"), true) { this@role.id.asString }
                    field(
                        texts.getString("role.default"),
                        true
                    ) { texts.getString("bool.${config.defaultRole == this@role.id.asString}") }
                    field(
                        texts.getString("role.mute"),
                        true
                    ) { texts.getString("bool.${config.muteRole == this@role.id.asString}") }
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
            }
            else -> message.reply(texts.getErrorString())
        }
    }
}