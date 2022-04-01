package pw.modder.answernator.utils.extensions.kord

import dev.kord.common.Color
import dev.kord.common.entity.Permission
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Member
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.sql.transactions.transaction
import pw.modder.answernator.db.Db
import pw.modder.answernator.db.guild.Mute

private val admin_permissions = listOf(
    Permission.Administrator,
    Permission.ManageMessages,
    Permission.BanMembers,
    Permission.KickMembers,
    Permission.ManageGuild,
    Permission.ManageChannels,
    Permission.ManageRoles
)

suspend fun Member.isAdmin(): Boolean {
    return admin_permissions.any(getPermissions()::contains)
}

fun Member.isOwner(guild: Guild): Boolean {
    return guild.ownerId == id
}

suspend fun Member.getColor(): Color? {
    return roles.toList().filter { it.color.rgb != 0 }.maxByOrNull { it.rawPosition }?.color
}

fun Member.isMuted(): Boolean {
    return Db.isMuted(guildId, id)
}

fun Member.mute(): Mute {
    return transaction { Mute.new {
        guild = guildId.toString()
        memberId = this@mute.id.toString()
    } }
}

fun Member.getMute(): Mute? {
    return Db.getMute(guildId, id)
}