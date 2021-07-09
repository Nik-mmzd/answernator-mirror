package pw.modder.answernator.db.guild

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Table
import pw.modder.answernator.db.GuildMutes
import pw.modder.answernator.db.GuildMutes.autoIncrement
import pw.modder.answernator.db.GuildMutes.index

class Mute(id: EntityID<Int>): IntEntity(id) {
    companion object : IntEntityClass<Mute>(Mutes)
    var guild by Mutes.guildId
    var memberId by Mutes.memberId

}

object Mutes: IntIdTable() {
    val guildId = varchar("guild_id", 18).index()
    val memberId = varchar("member_id", 18).index()
}