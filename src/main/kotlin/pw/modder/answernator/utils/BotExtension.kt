package pw.modder.answernator.utils

import com.jessecorbett.diskord.api.model.GuildMember
import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.model.Permission
import com.jessecorbett.diskord.api.model.Permissions
import com.jessecorbett.diskord.api.rest.client.GuildClient
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.DiskordDsl
import kotlinx.serialization.UnstableDefault

@UnstableDefault
@DiskordDsl
fun Bot.loadCommandService() {
    CommandList.load()
    val config = GlobalConfig.get()

    messageCreated {message: Message ->
        if (message.content.isEmpty()) return@messageCreated
        if (!message.content.startsWith(config.prefix)) return@messageCreated
        CommandList.commands.single { command ->
            message.content.startsWith(config.prefix + command.command)
        }.run {
            if (check(message, message.guildId?.run { clientStore.guilds[this] })) action(clientStore, message)
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