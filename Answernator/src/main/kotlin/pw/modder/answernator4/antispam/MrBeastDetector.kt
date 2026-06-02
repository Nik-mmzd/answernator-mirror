package pw.modder.answernator4.antispam

import kotlin.time.Duration

/**
 * Detects the "MrBeast lover" pattern: image dumps with little or no text. Pure and thread-safe,
 * holding only an ephemeral per-(guild, user) "suspicious" marker for the two-message pattern.
 *
 * The 90-day repeat-offender accounting (mute → ban) lives in the DB, not here; this only decides
 * whether a *violation* occurred for a given message.
 *
 * @param window how long a single suspicious message waits for a follow-up.
 * @param clock millisecond time source, injected for testing.
 */
class MrBeastDetector(
    private val window: Duration = AntiSpamDefaults.DETECTION_WINDOW,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    companion object {
        /** Attachment count that trips the pattern from a single message. */
        const val SINGLE_ATTACHMENT_THRESHOLD = 4

        /** Per-message attachment count that makes a message "suspicious" for the two-message pattern. */
        const val PAIR_ATTACHMENT_THRESHOLD = 2

        /** A description is trivial if it is blank or a single whitespace-separated token. */
        fun isTrivialDescription(content: String): Boolean {
            val trimmed = content.trim()
            if (trimmed.isEmpty()) return true
            return trimmed.split(WHITESPACE).size <= 1
        }

        private val WHITESPACE = Regex("""\s+""")
    }

    sealed interface Classification {
        /** Not a MrBeast message (or not enough attachments). */
        data object None : Classification

        /** First half of the two-message pattern recorded; no action yet. */
        data object Suspicious : Classification

        /** A violation occurred; [messages] are the offending messages to delete. */
        data class Triggered(val messages: List<TrackedMessage>) : Classification
    }

    private val lock = Any()
    private val markers = HashMap<Pair<ULong, ULong>, Pair<Long, TrackedMessage>>()

    /** Classify one message; updates the ephemeral suspicious marker as a side effect. */
    fun classify(
        guildId: ULong,
        userId: ULong,
        message: TrackedMessage,
        content: String,
        attachmentCount: Int,
    ): Classification {
        if (!isTrivialDescription(content)) return Classification.None

        val now = clock()
        val key = guildId to userId

        synchronized(lock) {
            when {
                attachmentCount >= SINGLE_ATTACHMENT_THRESHOLD -> {
                    markers.remove(key)
                    return Classification.Triggered(listOf(message))
                }

                attachmentCount >= PAIR_ATTACHMENT_THRESHOLD -> {
                    val existing = markers[key]?.takeIf { it.first >= now - window.inWholeMilliseconds }
                    return if (existing != null) {
                        markers.remove(key)
                        Classification.Triggered(listOf(existing.second, message))
                    } else {
                        markers[key] = now to message
                        Classification.Suspicious
                    }
                }

                else -> return Classification.None
            }
        }
    }

    /** Drop a user's suspicious marker (e.g. after enforcement). */
    fun reset(guildId: ULong, userId: ULong) {
        synchronized(lock) { markers.remove(guildId to userId) }
    }
}
