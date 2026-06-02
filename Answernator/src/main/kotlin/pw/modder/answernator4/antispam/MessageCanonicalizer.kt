package pw.modder.answernator4.antispam

import java.text.Normalizer

/** A Discord attachment reduced to the only stable identity Discord gives us: name + size. */
data class AttachmentSignature(val filename: String, val size: Long)

/**
 * Reduces a message to an aggressively normalized canonical form so that "similar" (not just
 * byte-identical) messages collapse together. Pure and side-effect free.
 *
 * Discord exposes no content hash for attachments, so an attachment contributes its
 * `filename + size` to the canon (the legacy implementation hashed only the size).
 */
object MessageCanonicalizer {
    private val customEmoji = Regex("""<a?:\w+:\d+>""")
    private val mention = Regex("""<(?:@[!&]?|#)\d+>""")
    private val url = Regex("""https?://(?:www\.)?([^\s/?#]+)\S*""", RegexOption.IGNORE_CASE)
    private val combiningMarks = Regex("""\p{Mn}+""")
    private val symbols = Regex("[\\p{So}\\uFE0F\\u200D]+") // pictographs, VS16, ZWJ
    private val digits = Regex("""\p{Nd}+""")
    private val repeats = Regex("""(.)\1{2,}""")
    private val whitespace = Regex("""\s+""")

    /** Canonical form of a user message: normalized text plus a stable attachment signature. */
    fun canonicalize(content: String, attachments: List<AttachmentSignature> = emptyList()): String {
        var s = Normalizer.normalize(content, Normalizer.Form.NFKC).lowercase()
        // Replace structured tokens before stripping digits (mentions/emoji embed ids).
        s = customEmoji.replace(s, " :e: ")
        s = mention.replace(s, " @u ")
        s = url.replace(s) { " ${it.groupValues[1]} " }
        // Strip diacritics: decompose and drop combining marks.
        s = Normalizer.normalize(s, Normalizer.Form.NFD)
        s = combiningMarks.replace(s, "")
        s = symbols.replace(s, "")
        s = digits.replace(s, "")
        s = repeats.replace(s) { it.groupValues[1].repeat(2) }
        s = whitespace.replace(s, " ").trim()

        if (attachments.isEmpty()) return s

        val signature = attachments
            .map { it.filename.lowercase().trim() to it.size }
            .sortedWith(compareBy({ it.first }, { it.second }))
            .joinToString(",") { "${it.first}:${it.second}" }
        return if (s.isEmpty()) "att|$signature" else "$s att|$signature"
    }

    /**
     * Canon for command (interaction) spam — the invoked command name, prefixed so it cannot
     * collide with a normalized text canon.
     */
    fun commandCanon(commandName: String): String = "cmd:${commandName.trim().lowercase()}"
}
