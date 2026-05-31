package pw.modder.answernator4.antispam

import pw.modder.answernator4.antispam.MrBeastDetector.Classification
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class MrBeastDetectorTest {

    private val guild = 1uL
    private val user = 7uL

    private class FakeClock(var now: Long = 0) : () -> Long {
        override fun invoke() = now
    }

    private fun msg(id: ULong) = TrackedMessage(channelId = 5uL, messageId = id)

    @Test
    fun `blank and single-token descriptions are trivial`() {
        assertTrue(MrBeastDetector.isTrivialDescription(""))
        assertTrue(MrBeastDetector.isTrivialDescription("   "))
        assertTrue(MrBeastDetector.isTrivialDescription("bro!"))
        assertTrue(MrBeastDetector.isTrivialDescription("<@123>"))
    }

    @Test
    fun `multi-token descriptions are not trivial`() {
        assertFalse(MrBeastDetector.isTrivialDescription("hey look at this"))
        assertFalse(MrBeastDetector.isTrivialDescription("two words"))
    }

    @Test
    fun `four attachments with trivial text trigger from a single message`() {
        val detector = MrBeastDetector(clock = FakeClock())
        val result = detector.classify(guild, user, msg(1uL), "", attachmentCount = 4)
        val triggered = assertIs<Classification.Triggered>(result)
        assertEquals(listOf(msg(1uL)), triggered.messages)
    }

    @Test
    fun `non-trivial text never triggers regardless of attachments`() {
        val detector = MrBeastDetector(clock = FakeClock())
        assertEquals(
            Classification.None,
            detector.classify(guild, user, msg(1uL), "look at these cool pics", attachmentCount = 8),
        )
    }

    @Test
    fun `two suspicious messages trigger and report both`() {
        val detector = MrBeastDetector(clock = FakeClock())
        assertEquals(Classification.Suspicious, detector.classify(guild, user, msg(1uL), "", attachmentCount = 2))
        val result = detector.classify(guild, user, msg(2uL), "bro", attachmentCount = 3)
        val triggered = assertIs<Classification.Triggered>(result)
        assertEquals(listOf(msg(1uL), msg(2uL)), triggered.messages)
    }

    @Test
    fun `a single suspicious message alone does not trigger`() {
        val detector = MrBeastDetector(clock = FakeClock())
        assertEquals(Classification.Suspicious, detector.classify(guild, user, msg(1uL), "", attachmentCount = 2))
    }

    @Test
    fun `stale suspicious marker expires and does not pair`() {
        val clock = FakeClock()
        val detector = MrBeastDetector(window = 5.minutes, clock = clock)
        assertEquals(Classification.Suspicious, detector.classify(guild, user, msg(1uL), "", attachmentCount = 2))
        clock.now += 6.minutes.inWholeMilliseconds
        // The old marker expired; this just becomes a fresh suspicious marker.
        assertEquals(Classification.Suspicious, detector.classify(guild, user, msg(2uL), "", attachmentCount = 2))
    }

    @Test
    fun `one attachment with trivial text is ignored`() {
        val detector = MrBeastDetector(clock = FakeClock())
        assertEquals(Classification.None, detector.classify(guild, user, msg(1uL), "", attachmentCount = 1))
    }
}
