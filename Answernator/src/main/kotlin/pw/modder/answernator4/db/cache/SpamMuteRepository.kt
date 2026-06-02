package pw.modder.answernator4.db.cache

import dev.kord.common.entity.Snowflake
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import pw.modder.answernator4.db.tables.SpamMutes
import kotlin.time.Clock
import kotlin.time.Duration

/**
 * Persistent store of applied spam mutes. Each insert records one mute; [countMutes] is the
 * escalation input for the repeat-offender logic (how many mutes a user accrued recently).
 *
 * Not cached: writes are rare but reads must reflect the just-written mute, and counts are cheap
 * point queries on indexed columns.
 */
class SpamMuteRepository(private val database: Database) {

    /** Records a mute that was applied to [userId] in [guildId]. */
    suspend fun insertMute(guildId: Snowflake, userId: Snowflake, messageContent: String): Unit =
        withContext(Dispatchers.IO) {
            transaction(database) {
                SpamMutes.insert {
                    it[SpamMutes.guildId] = guildId
                    it[SpamMutes.userId] = userId
                    it[SpamMutes.messageContent] = messageContent
                }
            }
        }

    /** Number of mutes applied to [userId] in [guildId] within the last [within]. */
    suspend fun countMutes(guildId: Snowflake, userId: Snowflake, within: Duration): Long {
        val cutoff = Clock.System.now().minus(within)
        return withContext(Dispatchers.IO) {
            transaction(database) {
                SpamMutes.selectAll().where {
                    (SpamMutes.guildId eq guildId) and
                        (SpamMutes.userId eq userId) and
                        (SpamMutes.violationDate greaterEq cutoff)
                }.count()
            }
        }
    }
}
