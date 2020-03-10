package pw.modder.answernator.utils.extensions

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.model.Permission
import com.jessecorbett.diskord.api.model.Permissions
import com.jessecorbett.diskord.api.model.UserStatus
import com.jessecorbett.diskord.api.rest.client.GuildClient
import com.jessecorbett.diskord.api.websocket.model.ActivityType
import com.jessecorbett.diskord.api.websocket.model.UserStatusActivity
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.DiskordDsl
import pw.modder.answernator.cache.RolesCache.getRolesCached
import pw.modder.answernator.cache.GuildMemberRolesCache.getMemberRolesCached
import pw.modder.answernator.cache.GuildOwnerCache.getOwnerCached
import com.jessecorbett.diskord.util.sendMessage
import com.jessecorbett.diskord.util.words
import kotlinx.serialization.UnstableDefault
import mu.KotlinLogging
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

        val locale = message.guildId?.run { Globals.getGuildConfig(this).locale } ?: config.locale
        val texts = ResourceBundle.getBundle("locale.botGlobal", locale,
            UTF8Control()
        )
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
                    val text = if (e.message == null)
                        texts.formatString("bot.notImplemented", "${config.prefix}$name")
                    else
                        texts.formatString("bot.notImplemented.message", "${config.prefix}$name", message)
                    message.reply(
                        text
                    )
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
        val config = Globals.getGuildConfig(it.guildId)
        if (config.greetNewUsers && config.greetingsChannel.isNotEmpty()) {
            clientStore.channels[config.greetingsChannel].sendMessage(String.format(
                config.greetingText,
                it.nickname ?: it.user?.username ?: "new user",
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

suspend fun GuildClient.computePermissions(memberId: String): Permissions {
    if (getOwnerCached() == memberId) return Permissions.ALL


    var permissions = Permissions.NONE
    val memberRoles = getMemberRolesCached(memberId)
    val roles = getRolesCached().filter { it.id in memberRoles }

    roles.forEach { role ->
        if (role.permissions.contains(Permission.ADMINISTRATOR)) return Permissions.ALL
        permissions += permissions + role.permissions
    }

    return permissions
}
