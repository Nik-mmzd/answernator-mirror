package pw.modder.answernator.utils

import com.jessecorbett.diskord.api.model.GuildMember
import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.model.Permission
import com.jessecorbett.diskord.api.model.Permissions
import com.jessecorbett.diskord.api.rest.client.GuildClient
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.DiskordDsl
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
        CommandList.commands.single { command ->
            message.content.startsWith(config.prefix + command.name)
        }.run {
            logger.debug { "found command $name, running" }
            if (check(message, message.guildId?.run { clientStore.guilds[this] })) {
                try {
                    action(clientStore, message, config.locale)
                } catch (_: NotImplementedError) {
                    dslmessage { text = "Command `${config.prefix}$name` is not implemented yet." }
                } catch (e: Exception) {
                    logger.error(e) { "got error while running command" }
                    dslmessage { text = "Invalid request. Please use `${config.prefix}help` for help" }
                }.run { message.reply(text, embed()) }
            }
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