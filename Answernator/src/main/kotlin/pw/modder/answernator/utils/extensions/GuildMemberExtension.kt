package pw.modder.answernator.utils.extensions

import com.jessecorbett.diskord.api.model.GuildMember
import com.jessecorbett.diskord.api.model.Permission
import com.jessecorbett.diskord.api.model.Permissions
import com.jessecorbett.diskord.api.rest.client.GuildClient
import pw.modder.answernator.cache.GuildCache.getCached

suspend fun GuildMember.computePermissions(client: GuildClient, memberId: String): Permissions {
    val guild = client.getCached()
    if (guild.ownerId == memberId) return Permissions.ALL

    val roles = guild.roles.filter { it.id in roleIds }

    var permissions = Permissions.NONE
    roles.forEach { role ->
        if (role.permissions.contains(Permission.ADMINISTRATOR)) return Permissions.ALL
        permissions += permissions + role.permissions
    }

    return permissions
}