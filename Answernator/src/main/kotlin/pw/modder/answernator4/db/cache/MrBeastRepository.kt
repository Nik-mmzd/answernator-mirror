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
import pw.modder.answernator4.db.tables.MrBeastLovers
import kotlin.time.Clock
import kotlin.time.Duration

/**
 * Persistent store of MrBeast-pattern violations. Escalation is hardcoded to a 90-day window
 * (1st violation → week-long mute, 2nd → ban), driven by [countViolations].
 */
class MrBeastRepository(private val database: Database) {

    /** Records a MrBeast-pattern violation by [userId] in [guildId]. */
    suspend fun insertViolation(
        guildId: Snowflake,
        userId: Snowflake,
        attachmentsCount: Int,
        messageContent: String,
    ): Unit = withContext(Dispatchers.IO) {
        transaction(database) {
            MrBeastLovers.insert {
                it[MrBeastLovers.guildId] = guildId
                it[MrBeastLovers.userId] = userId
                it[MrBeastLovers.attachmentsCount] = attachmentsCount
                it[MrBeastLovers.messageContent] = messageContent
            }
        }
    }

    /** Number of violations by [userId] in [guildId] within the last [within]. */
    suspend fun countViolations(guildId: Snowflake, userId: Snowflake, within: Duration): Long {
        val cutoff = Clock.System.now().minus(within)
        return withContext(Dispatchers.IO) {
            transaction(database) {
                MrBeastLovers.selectAll().where {
                    (MrBeastLovers.guildId eq guildId) and
                        (MrBeastLovers.userId eq userId) and
                        (MrBeastLovers.violationDate greaterEq cutoff)
                }.count()
            }
        }
    }
}
