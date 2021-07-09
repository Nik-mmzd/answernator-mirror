package pw.modder.answernator.db.guild

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Table

class BlacklistedCommand(id: EntityID<Int>): IntEntity(id) {
    companion object : IntEntityClass<BlacklistedCommand>(BlacklistedCommands)
    var guild by BlacklistedCommands.guildId
    var command by BlacklistedCommands.command
}

object BlacklistedCommands: IntIdTable() {
    val guildId = varchar("guild_id", 18).index()
    val command = varchar("command", 32).index()
}

object BlacklistedCommandsRef: Table() {
    val guild = reference("config", Configs)
    val command = reference("command", BlacklistedCommands)

    override val primaryKey: PrimaryKey = PrimaryKey(guild, command)
}