package pw.modder.answernator4.kord

import dev.kord.common.Color
import dev.kord.common.asJavaLocale
import dev.kord.core.Kord
import dev.kord.core.entity.Member
import dev.kord.core.entity.User
import dev.kord.core.event.guild.BanAddEvent
import dev.kord.core.event.guild.BanRemoveEvent
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberLeaveEvent
import dev.kord.core.event.guild.MemberUpdateEvent
import dev.kord.core.on
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.embed
import org.kodein.di.DI
import org.kodein.di.instance
import pw.modder.answernator.utils.TimestampFormat
import pw.modder.answernator.utils.mention
import pw.modder.answernator4.db.cache.LogsConfigRepository
import pw.modder.answernator4.interaction.l
import java.util.ResourceBundle
import kotlin.time.Clock

private fun MessageCreateBuilder.userEmbed(user: User, title: String, builder: EmbedBuilder.() -> Unit = {}) = embed {
    val avatar = (user.avatar ?: user.defaultAvatar)
    author {
        name = title
        icon = avatar.cdnUrl.toUrl()
    }
    description = "${user.mention} ${user.username}"
    builder()
    footer {
        text = "ID: ${user.id.value}"
    }
    image = ((user as? Member)?.memberAvatar ?: avatar).cdnUrl.toUrl()
    timestamp = Clock.System.now()
}

private val COLOR_GOOD = Color(49, 201, 80)
private val COLOR_BAD = Color(255, 99, 126)
private val COLOR_NEUTRAL = Color(52, 166, 244)

suspend fun Kord.guildLogService(di: DI) {
    val configRepository by di.instance<LogsConfigRepository>()
    on<MemberJoinEvent> {
        val config = configRepository.get(guildId) ?: return@on
        val channel = config.memberJoinChannel ?: return@on
        val bundle = ResourceBundle.getBundle("locale.v4.guild_log", config.locale.asJavaLocale())
        rest.channel.createMessage(channel) {
            userEmbed(member, bundle.l("member.join.title")) {
                field {
                    name = bundle.l("member.join.registered")
                    value = member.id.timestamp.mention(TimestampFormat.RELATIVE)
                    inline = true
                }
                color = COLOR_GOOD
            }
        }
    }

    on<MemberLeaveEvent> {
        val config = configRepository.get(guildId) ?: return@on
        val channel = config.memberLeaveChannel ?: return@on
        val bundle = ResourceBundle.getBundle("locale.v4.guild_log", config.locale.asJavaLocale())
        rest.channel.createMessage(channel) {
            userEmbed(user, bundle.l("member.leave.title")) { color = COLOR_BAD }
        }
    }

    on<BanAddEvent> {
        val config = configRepository.get(guildId) ?: return@on
        val channel = config.memberBanLogChannel ?: return@on
        val bundle = ResourceBundle.getBundle("locale.v4.guild_log", config.locale.asJavaLocale())
        rest.channel.createMessage(channel) {
            userEmbed(user, bundle.l("member.ban.title")) { color = COLOR_BAD }
        }
    }

    on<BanRemoveEvent> {
        val config = configRepository.get(guildId) ?: return@on
        val channel = config.memberUnbanLogChannel ?: return@on
        val bundle = ResourceBundle.getBundle("locale.v4.guild_log", config.locale.asJavaLocale())
        rest.channel.createMessage(channel) {
            userEmbed(user, bundle.l("member.unban.title")) { color = COLOR_GOOD }
        }
    }

    on<MemberUpdateEvent> {
        val old = old ?: return@on // we do not have any data to compare with
        val config = configRepository.get(guildId) ?: return@on
        val bundle = ResourceBundle.getBundle("locale.v4.guild_log", config.locale.asJavaLocale())
        if (member.communicationDisabledUntil != old.communicationDisabledUntil) {
            val channel = config.memberMuteLogChannel ?: return@on
            // timeout update
            rest.channel.createMessage(channel) {
                if (member.communicationDisabledUntil == null)
                    userEmbed(member, bundle.l("member.timeout.gone")) { color = COLOR_GOOD }
                else
                    userEmbed(member, bundle.l("member.timeout.set")) {
                        field {
                            name = bundle.l("member.timeout.until")
                            value = member.communicationDisabledUntil!!.mention(TimestampFormat.LONG_DATETIME)
                            inline = true
                        }
                        color = COLOR_BAD
                    }
            }
        }

        val channel = config.memberUpdateLogChannel ?: return@on
        if (member.memberAvatarHash != old.memberAvatarHash) {
            // member avatar update
            rest.channel.createMessage(channel) {
                userEmbed(member, bundle.l("member.avatar.title")) { color = COLOR_NEUTRAL }
            }
        }

        if (member.avatarHash != old.avatarHash) {
            // avatar update
            rest.channel.createMessage(channel) {
                userEmbed(member, bundle.l("user.avatar.title")) { color = COLOR_NEUTRAL }
            }
        }

        if (member.effectiveName != old.effectiveName) {
            // name update
            rest.channel.createMessage(channel) {
                userEmbed(member, bundle.l("member.name.title")) {
                    field {
                        name = bundle.l("member.name.old")
                        value = old.effectiveName
                        inline = true
                    }
                    field {
                        name = bundle.l("member.name.new")
                        value = member.effectiveName
                        inline = true
                    }
                    color = COLOR_NEUTRAL
                }
            }
        }

        if (member.username != old.username) {
            // name update
            rest.channel.createMessage(channel) {
                userEmbed(member, bundle.l("member.username.title")) {
                    field {
                        name = bundle.l("member.name.old")
                        value = old.username
                        inline = true
                    }
                    field {
                        name = bundle.l("member.name.new")
                        value = member.username
                        inline = true
                    }
                    color = COLOR_NEUTRAL
                }
            }
        }
    }
}
