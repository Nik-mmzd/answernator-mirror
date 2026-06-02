package pw.modder.answernator4.db.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import pw.modder.answernator4.db.asSnowflake

/**
 * One row per spam mute actually applied. This is both an audit log and the escalation state for
 * the repeat-offender logic: the number of rows for a user within `muteValidity`
 * decides whether the next offence is another mute or a ban.
 */
object SpamMutes : Table() {
    val userId = long("user_id").asSnowflake()
    val guildId = long("guild_id").asSnowflake()
    val messageContent = text("message_content")
    val violationDate = timestamp("violation_date").defaultExpression(CurrentTimestamp)

    init {
        index(false, userId)
        index(false, userId, guildId)
    }
}
