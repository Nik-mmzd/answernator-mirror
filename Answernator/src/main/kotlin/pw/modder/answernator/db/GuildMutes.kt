package pw.modder.answernator.db

import org.jetbrains.exposed.sql.Table

object GuildMutes: Table() {
    var guildId = varchar("guild_id", 18).index()
    var memberId = varchar("member_id", 18).index()
}