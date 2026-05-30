package pw.modder.answernator4.db.cache

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import dev.kord.common.entity.Snowflake
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.Optional
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

/**
 * Read-through cache over a per-guild configuration table keyed by guild [Snowflake].
 *
 * These tables are configuration: written rarely (admin commands) and read on nearly every request,
 * so we keep them in a Guava cache instead of hitting the database each time. The cache is the single
 * read point — writers must call [invalidate] after mutating a guild's config so the next [get]
 * reloads (write-through invalidation). [expireAfterWrite] is only a safety net for writes that bypass
 * this process (e.g. direct SQL, or a future second instance).
 *
 * Absence is cached too (via [Optional.empty]) so guilds without a config row don't re-query every time.
 */
abstract class CachedConfigRepository<T : Any>(
    private val database: Database,
    maximumSize: Long = 256,
    expireAfterWrite: Duration = 10.minutes,
) {
    private val cache: Cache<Long, Optional<T>> = CacheBuilder.newBuilder()
        .maximumSize(maximumSize)
        .expireAfterWrite(expireAfterWrite.toJavaDuration())
        .build()

    /** Loads a guild's config from the database. Runs inside an Exposed transaction; returns `null` when absent. */
    protected abstract fun load(guildId: Long): T?

    /** Returns the (cached) config for [guild], loading it from the database on a miss. */
    suspend fun get(guild: Snowflake): T? {
        val key = guild.value.toLong()
        cache.getIfPresent(key)?.let { return it.orElse(null) }

        val loaded = withContext(Dispatchers.IO) { transaction(database) { load(key) } }
        cache.put(key, Optional.ofNullable(loaded))
        return loaded
    }

    /** Drops the cached entry for [guild] so the next [get] reloads from the database. Call after writing config. */
    fun invalidate(guild: Snowflake) = cache.invalidate(guild.value.toLong())
}
