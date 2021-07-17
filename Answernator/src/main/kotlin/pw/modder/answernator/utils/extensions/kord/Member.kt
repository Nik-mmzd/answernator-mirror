package pw.modder.answernator.utils.extensions.kord

import dev.kord.common.Color
import dev.kord.common.entity.Permission
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Member
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import pw.modder.answernator.db.Db
import pw.modder.answernator.db.guild.Mute
import pw.modder.answernator.db.guild.Mutes

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
    return roles.toList().filter { it.color.rgb != 0 }.sortedBy { it.rawPosition }.firstOrNull()?.color
}

fun Member.isMuted(): Boolean {
    return Db.isMuted(guildId, id)
}

fun Member.mute(): Mute {
    return transaction { Mute.new {
        guild = guildId.asString
        memberId = this@mute.id.asString
    } }
}

fun Member.getMute(): Mute? {
    return transaction {
        Mute.find { Mutes.guildId eq guildId.asString and(Mutes.memberId eq this@getMute.id.asString) }.firstOrNull()
    }
}