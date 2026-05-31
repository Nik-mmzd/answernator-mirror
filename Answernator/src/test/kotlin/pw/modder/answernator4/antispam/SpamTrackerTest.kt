package pw.modder.answernator4.antispam

import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class SpamTrackerTest {

    private val guild = 1uL
    private val user = 42uL
    private val thresholds = SpamThresholds(warningThreshold = 3, muteThreshold = 5, window = 5.minutes)

    private class FakeClock(var now: Long = 0) : () -> Long {
        override fun invoke() = now
    }

    private fun msg(id: ULong) = TrackedMessage(channelId = 1000uL, messageId = id)

    private fun SpamTracker.send(canon: String, id: ULong) =
        record(guild, user, canon, msg(id), thresholds)

    @Test
    fun `warning fires once at the warning threshold`() {
        val tracker = SpamTracker(FakeClock())
        assertEquals(SpamAction.NONE, tracker.send("spam text", 1uL).action)
        assertEquals(SpamAction.NONE, tracker.send("spam text", 2uL).action)
        assertEquals(SpamAction.WARN, tracker.send("spam text", 3uL).action)
        // Next message does not warn again; it climbs toward the mute threshold.
        assertEquals(SpamAction.NONE, tracker.send("spam text", 4uL).action)
    }

    @Test
    fun `escalation fires at the mute threshold and carries tracked messages`() {
        val tracker = SpamTracker(FakeClock())
        repeat(4) { tracker.send("spam text", it.toULong()) }
        val decision = tracker.send("spam text", 99uL)
        assertEquals(SpamAction.ESCALATE, decision.action)
        assertEquals(5, decision.groupCount)
        assertEquals(5, decision.trackedMessages.size)
        assertTrue(decision.trackedMessages.any { it.messageId == 99uL })
    }

    @Test
    fun `state resets after escalation so counting restarts from zero`() {
        val tracker = SpamTracker(FakeClock())
        repeat(5) { tracker.send("spam text", it.toULong()) } // escalates on the 5th
        assertEquals(0, tracker.activeGroups(guild, user))
        assertEquals(SpamAction.NONE, tracker.send("spam text", 100uL).action)
    }

    @Test
    fun `similar messages share a group`() {
        val tracker = SpamTracker(FakeClock())
        tracker.send("join my server for free nitro gift", 1uL)
        tracker.send("join my server for free nitro gift now", 2uL)
        val third = tracker.send("join my server for free nitro gift today", 3uL)
        assertEquals(SpamAction.WARN, third.action)
        assertEquals(1, tracker.activeGroups(guild, user))
    }

    @Test
    fun `distinct messages get independent counters`() {
        val tracker = SpamTracker(FakeClock())
        // Two unrelated texts interleaved; neither reaches its own threshold.
        assertEquals(SpamAction.NONE, tracker.send("good morning everyone", 1uL).action)
        assertEquals(SpamAction.NONE, tracker.send("buy cheap nitro right now", 2uL).action)
        assertEquals(SpamAction.NONE, tracker.send("good morning everyone", 3uL).action)
        assertEquals(SpamAction.NONE, tracker.send("buy cheap nitro right now", 4uL).action)
        assertEquals(2, tracker.activeGroups(guild, user))
        // The 3rd hit of the "good morning" group reaches its own warning threshold,
        // independently of the "buy cheap nitro" group (still at 2).
        assertEquals(SpamAction.WARN, tracker.send("good morning everyone", 5uL).action)
    }

    @Test
    fun `messages outside the window do not count`() {
        val clock = FakeClock()
        val tracker = SpamTracker(clock)
        tracker.send("spam text", 1uL)
        tracker.send("spam text", 2uL)
        clock.now += 6.minutes.inWholeMilliseconds // past the 5-minute window
        // The two old hits expired; this is the only live one.
        assertEquals(SpamAction.NONE, tracker.send("spam text", 3uL).action)
        assertEquals(1, tracker.activeGroups(guild, user, clock.now))
    }

    @Test
    fun `mute threshold of zero never escalates via that path`() {
        // muteThreshold > 0 is required for escalation; a misconfigured 0 only warns.
        val tracker = SpamTracker(FakeClock())
        val t = SpamThresholds(warningThreshold = 2, muteThreshold = 0, window = 5.minutes)
        tracker.record(guild, user, "x", msg(1uL), t)
        assertEquals(SpamAction.WARN, tracker.record(guild, user, "x", msg(2uL), t).action)
        repeat(10) { tracker.record(guild, user, "x", msg(it.toULong()), t) }
        // Never escalates.
        assertEquals(SpamAction.NONE, tracker.record(guild, user, "x", msg(50uL), t).action)
    }

    @Test
    fun `concurrent records do not lose updates`() {
        val tracker = SpamTracker(FakeClock())
        val highThreshold = SpamThresholds(warningThreshold = 100_000, muteThreshold = 100_000, window = 60.minutes)
        val counter = AtomicInteger()
        val threads = (0 until 8).map { t ->
            Thread {
                repeat(500) {
                    val d = tracker.record(guild, user, "same canon", msg((t * 1000 + it).toULong()), highThreshold)
                    counter.set(d.groupCount)
                }
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        // 8 * 500 messages, one group, none escalated/reset → all counted.
        assertEquals(4000, tracker.record(guild, user, "same canon", msg(999_999uL), highThreshold).groupCount - 1)
    }
}
