package pw.modder.answernator.db.guild

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass

class BlacklistedCommand(id: EntityID<Int>): IntEntity(id) {
    companion object : IntEntityClass<BlacklistedCommand>(BlacklistedCommands)
    var guild by BlacklistedCommands.guildId
    var command by BlacklistedCommands.command
}

object BlacklistedCommands: IntIdTable() {
    val guildId = varchar("guild_id", 18).index()
    val command = varchar("command", 32).index()
}