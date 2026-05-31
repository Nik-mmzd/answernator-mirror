package pw.modder.answernator4.antispam

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Shared defaults for the pure anti-spam detection logic. Values that map to per-guild
 * configuration (mute duration/validity, thresholds) live in the DB config; these are the
 * detection-engine knobs that are not worth exposing per guild.
 */
object AntiSpamDefaults {
    /** Jaccard similarity above which two canonicalized messages count as the "same" group. */
    const val SIMILARITY_THRESHOLD: Double = 0.8

    /** Word-shingle size used for Jaccard comparison. Falls back to unigrams for short texts. */
    const val SHINGLE_SIZE: Int = 2

    /** Sliding window over which spam is counted. Ephemeral, reset on restart. */
    val DETECTION_WINDOW: Duration = 10.minutes

    /** Cap on distinct message groups tracked per (guild, user) to bound memory. */
    const val MAX_GROUPS_PER_USER: Int = 16

    /** Cap on tracked message ids retained per group (for cleanup) to bound memory. */
    const val MAX_TRACKED_PER_GROUP: Int = 100
}
