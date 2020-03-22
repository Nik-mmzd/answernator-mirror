package pw.modder.answernator.utils.extensions

import com.jessecorbett.diskord.api.model.*
import com.jessecorbett.diskord.api.rest.client.GuildClient
import pw.modder.answernator.cache.GuildCache.getCached

suspend fun GuildMember.computePermissions(client: GuildClient, memberId: String): Permissions = computePermissions(client.getCached(), memberId)
fun GuildMember.computePermissions(guild: Guild, memberId: String): Permissions {
    if (guild.ownerId == memberId) return Permissions.ALL

    val roles = guild.roles.filter { it.id in roleIds }

    var permissions = Permissions.NONE
    roles.forEach { role ->
        if (role.permissions.contains(Permission.ADMINISTRATOR)) return Permissions.ALL
        permissions += permissions + role.permissions
    }

    return permissions
}

fun GuildMember.isAdmin(guild: Guild, memberId: String): Boolean = computePermissions(guild, memberId).containsAny(
        Permission.ADMINISTRATOR,
        Permission.MANAGE_MESSAGES,
        Permission.BAN_MEMBERS,
        Permission.KICK_MEMBERS,
        Permission.MANAGE_GUILD,
        Permission.MANAGE_CHANNELS,
        Permission.MANAGE_ROLES,
        Permission.MANAGE_NICKNAMES
    )

fun GuildMember.getColor(roles: List<Role>): Int? {
    return roles.filter { it.id in roleIds }.maxBy { it.position }?.color
}

fun GuildMember.getColor(guild: Guild): Int? = getColor(guild.roles)