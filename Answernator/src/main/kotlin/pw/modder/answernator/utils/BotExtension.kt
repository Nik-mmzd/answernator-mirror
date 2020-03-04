package pw.modder.answernator.utils

import com.jessecorbett.diskord.api.model.GuildMember
import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.model.Permission
import com.jessecorbett.diskord.api.model.Permissions
import com.jessecorbett.diskord.api.rest.client.GuildClient
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.DiskordDsl
import com.jessecorbett.diskord.util.authorId
import com.jessecorbett.diskord.util.sendMessage
import kotlinx.serialization.UnstableDefault
import mu.KotlinLogging
import com.jessecorbett.diskord.dsl.message as dslmessage

private val logger = KotlinLogging.logger {}
@UnstableDefault
@DiskordDsl
fun Bot.loadCommandService() {
    val config = GlobalConfig.get()

    messageCreated { message: Message ->
        if (message.content.isEmpty()) return@messageCreated
        logger.debug { "received message, message text: ${message.content}" }
        if (!message.content.startsWith(config.prefix)) return@messageCreated

        val locale = message.guildId?.run { GuildConfigs.get(this).locale } ?: config.locale
        CommandList.commands.singleOrNull { command ->
            message.content.startsWith(config.prefix + command.name) && locale in command.lang
        }?.run {
            logger.debug { "found command $name, running" }
            if (check(message, message.guildId?.run { clientStore.guilds[this] })) {
                val reply = try {
                    action(clientStore, message, locale)
                } catch (_: NotImplementedError) {
                    dslmessage { text = "Command `${config.prefix}$name` is not implemented yet." }
                } catch (e: Exception) {
                    logger.error(e) { "got error while running command" }
                    dslmessage { text = "Invalid request. Please use `${config.prefix}help` for help" }
                }

                message.reply(reply.text, reply.embed())
            }
        }
    }
}

@UnstableDefault
@DiskordDsl
fun Bot.greetingsService() {
    userJoinedGuild {
        val config = GuildConfigs.get(it.guildId)
        if (config.greetNewUsers && config.greetingsChannel.isNotEmpty()) {
            clientStore.channels[config.greetingsChannel].sendMessage(String.format(
                config.greetingText,
                it.nickname ?: it.user?.username ?: "new user",
                clientStore.guilds[it.guildId].get().name
            ))
        }
    }
}

suspend fun GuildMember.computePermissions(guild: GuildClient): Permissions {
    user?.run {
        if (guild.get().ownerId == id) return Permissions.ALL
    }

    var permissions = Permissions.NONE
    val roles = guild.getRoles().filter { it.id in roleIds }

    roles.forEach { role ->
        if (role.permissions.contains(Permission.ADMINISTRATOR)) return Permissions.ALL
        permissions += permissions + role.permissions
    }

    return permissions
}
