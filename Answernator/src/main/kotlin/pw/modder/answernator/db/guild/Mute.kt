package pw.modder.answernator.db.guild

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

@Deprecated("Deprecated. Use for migrations only")
object Mutes: IntIdTable() {
    val guildId = varchar("guild_id", 18).index()
    val memberId = varchar("member_id", 18).index()
}
