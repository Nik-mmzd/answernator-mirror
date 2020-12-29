package pw.modder.answernator.tools.helper

import com.jessecorbett.diskord.api.model.Guild
import com.jessecorbett.diskord.api.model.GuildMember
import com.jessecorbett.diskord.api.model.Permissions

fun GuildMember.computeRealPermissions(guild: Guild, memberId: String): Permissions {
    if (guild.ownerId == memberId) return Permissions.ALL

    var permissions = guild.roles.single { it.id == guild.id }.permissions
    guild.roles.filter { it.id in roleIds }.forEach { role ->
        permissions += role.permissions
    }

    return permissions
}