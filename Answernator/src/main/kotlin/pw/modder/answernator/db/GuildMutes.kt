package pw.modder.answernator.db

import org.jetbrains.exposed.sql.Table

@Deprecated("Use db.guild.Mutes instead")
object GuildMutes: Table() {
    val id = integer("id").autoIncrement()
    var guildId = varchar("guild_id", 18).index()
    var memberId = varchar("member_id", 18).index()

    override val primaryKey: PrimaryKey = PrimaryKey(id)
}