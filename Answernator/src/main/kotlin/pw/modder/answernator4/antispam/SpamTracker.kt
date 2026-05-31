package pw.modder.answernator4.antispam

import kotlin.time.Duration

/** A message location worth remembering for later cleanup (raw snowflake values). */
data class TrackedMessage(val channelId: ULong, val messageId: ULong)

/** Per-guild detection knobs resolved from a guild's config (counts) plus engine defaults. */
data class SpamThresholds(
    /** Group count at which a single warning fires (once). 0 disables warnings. */
    val warningThreshold: Int,
    /** Group count at which enforcement (mute/ban) is triggered. */
    val muteThreshold: Int,
    val window: Duration = AntiSpamDefaults.DETECTION_WINDOW,
    val similarityThreshold: Double = AntiSpamDefaults.SIMILARITY_THRESHOLD,
    val shingleSize: Int = AntiSpamDefaults.SHINGLE_SIZE,
    val maxGroupsPerUser: Int = AntiSpamDefaults.MAX_GROUPS_PER_USER,
    val maxTrackedPerGroup: Int = AntiSpamDefaults.MAX_TRACKED_PER_GROUP,
)

enum class SpamAction { NONE, WARN, ESCALATE }

/**
 * Outcome of recording one message.
 *
 * [trackedMessages] is populated only on [SpamAction.ESCALATE] (the messages of the offending
 * group, captured atomically before the user's ephemeral state is reset) — the caller uses them
 * for cleanup. The mute-vs-ban split is decided by the caller from persisted mute history.
 */
data class SpamDecision(
    val action: SpamAction,
    val groupCount: Int,
    val trackedMessages: List<TrackedMessage> = emptyList(),
)

/**
 * Ephemeral, thread-safe spam state, counted per (guild, user) over a sliding [SpamThresholds.window].
 *
 * Each distinct message (fuzzy-matched via [Similarity]) forms its own group with its own counter,
 * so a user cycling through several different spam messages is tracked per message. State is held
 * in memory only and is intentionally lost on restart; durable escalation lives in the DB.
 *
 * The decision is computed under the lock and, on escalation, the offending user's whole state is
 * captured and cleared atomically so concurrent triggers cannot double-fire. REST side effects are
 * the caller's responsibility, outside the lock.
 *
 * @param clock millisecond time source, injected for testing.
 */
class SpamTracker(private val clock: () -> Long = System::currentTimeMillis) {

    private class Group(var canon: String, var shingles: Set<String>) {
        val events = ArrayDeque<Long>()
        val messages = ArrayDeque<Pair<Long, TrackedMessage>>()
    }

    private val lock = Any()
    private val users = HashMap<Pair<ULong, ULong>, MutableList<Group>>()

    /** Record a message and return the resulting decision. */
    fun record(
        guildId: ULong,
        userId: ULong,
        canon: String,
        message: TrackedMessage,
        thresholds: SpamThresholds,
    ): SpamDecision {
        val now = clock()
        val cutoff = now - thresholds.window.inWholeMilliseconds

        synchronized(lock) {
            val key = guildId to userId
            val groups = users.getOrPut(key) { mutableListOf() }
            pruneGroups(groups, cutoff)

            val shingles = Similarity.shingles(canon, thresholds.shingleSize)
            val group = groups.firstOrNull {
                it.canon == canon ||
                    Similarity.jaccard(it.shingles, shingles) >= thresholds.similarityThreshold
            } ?: Group(canon, shingles).also {
                groups += it
                if (groups.size > thresholds.maxGroupsPerUser) groups.removeAt(0)
            }

            group.events.addLast(now)
            group.messages.addLast(now to message)
            while (group.messages.size > thresholds.maxTrackedPerGroup) group.messages.removeFirst()

            val count = group.events.size
            return when {
                thresholds.muteThreshold > 0 && count >= thresholds.muteThreshold -> {
                    val tracked = group.messages.map { it.second }
                    users.remove(key) // reset offender's whole state atomically
                    SpamDecision(SpamAction.ESCALATE, count, tracked)
                }

                thresholds.warningThreshold > 0 && count == thresholds.warningThreshold ->
                    SpamDecision(SpamAction.WARN, count)

                else -> SpamDecision(SpamAction.NONE, count)
            }
        }
    }

    /** Drop a user's ephemeral state (e.g. after an externally performed enforcement action). */
    fun reset(guildId: ULong, userId: ULong) {
        synchronized(lock) { users.remove(guildId to userId) }
    }

    /** Number of active (non-expired) groups for a user — exposed for tests/diagnostics. */
    fun activeGroups(guildId: ULong, userId: ULong, now: Long = clock()): Int {
        synchronized(lock) {
            val groups = users[guildId to userId] ?: return 0
            pruneGroups(groups, now - AntiSpamDefaults.DETECTION_WINDOW.inWholeMilliseconds)
            return groups.size
        }
    }

    private fun pruneGroups(groups: MutableList<Group>, cutoff: Long) {
        val iterator = groups.iterator()
        while (iterator.hasNext()) {
            val group = iterator.next()
            while (group.events.isNotEmpty() && group.events.first() < cutoff) group.events.removeFirst()
            while (group.messages.isNotEmpty() && group.messages.first().first < cutoff) {
                group.messages.removeFirst()
            }
            if (group.events.isEmpty()) iterator.remove()
        }
    }
}
