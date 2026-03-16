package pw.modder.answernator.db.guild

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

class Mute(id: EntityID<Int>): IntEntity(id) {
    companion object : IntEntityClass<Mute>(Mutes)
    var guild by Mutes.guildId
    var memberId by Mutes.memberId

}

object Mutes: IntIdTable() {
    val guildId = varchar("guild_id", 18).index()
    val memberId = varchar("member_id", 18).index()
}