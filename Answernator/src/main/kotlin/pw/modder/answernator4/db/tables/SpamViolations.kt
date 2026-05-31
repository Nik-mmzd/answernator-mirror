package pw.modder.answernator4.db.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import pw.modder.answernator4.db.asSnowflake

object SpamViolations : Table() {
    val userId = long("user_id").asSnowflake()
    val guildId = long("guild_id").asSnowflake()
    val messageContent = text("message_content")
    val violationDate = timestamp("violation_date").defaultExpression(CurrentTimestamp)

    init {
        index(false, userId)
        index(false, userId, guildId)
    }
}
