package pw.modder.answernator4.antispam

/**
 * Fuzzy text similarity via Jaccard index over word shingles. Pure and side-effect free.
 *
 * Word shingles of [shingleSize] capture local word order; for texts shorter than the shingle
 * size the whole token list becomes a single shingle (so very short messages still compare).
 */
object Similarity {
    /** Word shingles of size [shingleSize] for a canonicalized string. */
    fun shingles(canon: String, shingleSize: Int = AntiSpamDefaults.SHINGLE_SIZE): Set<String> {
        val tokens = canon.split(' ').filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return emptySet()
        if (tokens.size < shingleSize || shingleSize <= 1) return tokens.toSet()
        return (0..tokens.size - shingleSize)
            .mapTo(HashSet()) { tokens.subList(it, it + shingleSize).joinToString(" ") }
    }

    /** Jaccard index of two shingle sets. Two empty sets are considered identical. */
    fun jaccard(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() && b.isEmpty()) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val intersection = a.count { it in b }
        val union = a.size + b.size - intersection
        return intersection.toDouble() / union
    }

    /** True if [a] and [b] are identical or their Jaccard similarity reaches [threshold]. */
    fun isSimilar(
        a: String,
        b: String,
        threshold: Double = AntiSpamDefaults.SIMILARITY_THRESHOLD,
        shingleSize: Int = AntiSpamDefaults.SHINGLE_SIZE,
    ): Boolean {
        if (a == b) return true
        return jaccard(shingles(a, shingleSize), shingles(b, shingleSize)) >= threshold
    }
}
