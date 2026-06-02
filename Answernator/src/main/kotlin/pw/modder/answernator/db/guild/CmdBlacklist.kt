package pw.modder.answernator.db.guild

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

@Deprecated("Deprecated. Use for migrations only")
object BlacklistedCommands: IntIdTable() {
    val guildId = varchar("guild_id", 18).index()
    val command = varchar("command", 32).index()
}
