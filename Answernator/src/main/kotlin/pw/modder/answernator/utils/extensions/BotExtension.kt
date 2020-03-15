package pw.modder.answernator.utils.extensions

import com.jessecorbett.diskord.api.model.*
import com.jessecorbett.diskord.api.websocket.model.ActivityType
import com.jessecorbett.diskord.api.websocket.model.UserStatusActivity
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.DiskordDsl
import com.jessecorbett.diskord.util.mention
import com.jessecorbett.diskord.util.sendMessage
import com.jessecorbett.diskord.util.words
import pw.modder.answernator.db.Db.memberIsMuted
import pw.modder.answernator.db.Db.muteMember
import pw.modder.answernator.db.Db.unmuteMember
import kotlinx.serialization.UnstableDefault
import mu.KotlinLogging
import pw.modder.answernator.db.Db
import pw.modder.answernator.utils.*
import java.util.*

private val logger = KotlinLogging.logger {}
@UnstableDefault
@DiskordDsl
fun Bot.loadCommandService() {
    val config = Globals.config

    messageCreated { message: Message ->
        if (message.content.isEmpty()) return@messageCreated
        logger.debug { "received message, message text: ${message.content}" }
        if (message.content.first() != config.prefix) return@messageCreated

        val locale = message.guildId?.run { Locale(Db.guilds.get(this).lang) } ?: config.locale
        val texts = ResourceBundle.getBundle("locale.botGlobal", locale, UTF8Control())

        val channelType =if (message.guildId == null) Command.ChannelTypes.DIRECT else Command.ChannelTypes.GUILD
        CommandList.commands.singleOrNull { command ->
            logger.debug { "probing command ${command.name}, searching for ${message.words.first()}" }
            message.words.first().equals("${config.prefix}${command.name}", true) && channelType in command.channels
        }?.run {
            logger.debug { "found command $name, running" }
            if (check(message, message.guildId?.run { clientStore.guilds[this] })) {
                val reply = try {
                    action(this@loadCommandService, message, locale)
                } catch (e: NotImplementedError) {
                    val text = e.message?.run { texts.formatString("bot.notImplemented.message", "${config.prefix}$name", this) }
                        ?: texts.formatString("bot.notImplemented", "${config.prefix}$name")
                    message.reply(text)
                    return@run
                } catch (e: Exception) {
                    logger.error(e) { "got error while running command" }
                    message.reply(
                        texts.formatString("bot.error", "${config.prefix}$name")
                    )
                    return@run
                }

                message.reply(reply.text, reply.embed())
                return@run
            }

            message.reply(
                texts.getStringOrKey("bot.noPerms")
            )
        }
    }
}

@UnstableDefault
@DiskordDsl
fun Bot.greetingsService() {
    userJoinedGuild {
        val config = Db.guilds.get(it.guildId)
        if (config.greetNewUsers && config.greetingsChannel.isNotEmpty()) {
            clientStore.channels[config.greetingsChannel].sendMessage(String.format(
                config.greetingText,
                it.user?.mention ?: "??!?? O_o",
                clientStore.guilds[it.guildId].get().name
            ))
        }
    }
}

@UnstableDefault
@DiskordDsl
fun Bot.defaultStatusService() {
    started {
        setStatus(
            status = UserStatus.ONLINE,
            activity = UserStatusActivity(
                name = Globals.config.defaultStatus,
                type = ActivityType.GAME
            )
        )
    }
}

@UnstableDefault
@DiskordDsl
fun Bot.muteChecker() {
    userJoinedGuild {
        val memberId = it.user?.id ?: run {
            logger.error { "Mute Checker: ${it.guildId}, no user object, can't apply mute role!" }
            return@userJoinedGuild
        }
        val roleId = Db.guilds.get(it.guildId).muteRole.takeIf { it.isNotEmpty() } ?: return@userJoinedGuild
        val guildClient = clientStore.guilds[it.guildId]
        if (guildClient.memberIsMuted(memberId)) guildClient.addMemberRole(memberId, roleId)
        logger.debug { "Member muted automatically: Guild ${it.guildId}, User ${it.user?.username} ID ${it.user?.id}" }
    }

    guildMemberUpdated {
        val config = Db.guilds.get(it.guildId)
        val roleId = config.muteRole.takeIf { it.isNotEmpty() } ?: return@guildMemberUpdated
        val logConfig = Db.logs.get(it.guildId)
        val guild = clientStore.guilds[it.guildId]
        val texts = ResourceBundle.getBundle("locale.mute", UTF8Control())

        if (it.roles.any { it == roleId } && !guild.memberIsMuted(it.user.id)) {
            guild.muteMember(it.user.id)
            try {
                logConfig.memberMuteLogChannel.takeIf { it.isNotEmpty() }?.run {
                    clientStore.channels[this].sendMessage(String.format(
                        texts.getStringOrKey("mute.muted"),
                        it.user.mention,
                        texts.getStringOrKey("mute.reason.${Globals.random.nextInt(0, texts.getStringOrKey("mute.reason.count").toIntOrNull() ?: 1)}")
                    ))
                }
            } catch (e: Exception) {
                logger.warn(e) { "Logger: Error while auto-muting by role update" }
            }
            guild.addMemberRole(it.user.id, roleId)

            return@guildMemberUpdated
        }

        if (it.roles.none { it == roleId } && guild.memberIsMuted(it.user.id)) {
            guild.unmuteMember(it.user.id)
            try {
                logConfig.memberUnmuteLogChannel.takeIf { it.isNotEmpty() }?.run {
                    clientStore.channels[this].sendMessage(String.format(
                        texts.getStringOrKey("mute.unmute"),
                        it.user.mention
                    ))
                }
            } catch (e: Exception) {
                logger.warn(e) { "Logger: Error while auto-unmuting by role update" }
            }
            guild.removeMemberRole(it.user.id, roleId)
        }
    }
}

@UnstableDefault
@DiskordDsl
fun Bot.defaultRoleService() {
    userJoinedGuild {
        val config = Db.guilds.get(it.guildId)
        val guild = clientStore.guilds[it.guildId]
        val id = it.user?.id ?: return@userJoinedGuild
        config.defaultRole.takeIf { it.isNotEmpty() }?.run {
            guild.addMemberRole(id, this)
        }
    }
}