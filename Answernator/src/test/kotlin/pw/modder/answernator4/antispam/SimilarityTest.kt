package pw.modder.answernator4.antispam

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SimilarityTest {

    @Test
    fun `identical strings are similar`() {
        assertTrue(Similarity.isSimilar("buy cheap viagra now", "buy cheap viagra now"))
    }

    @Test
    fun `a small edit stays similar`() {
        assertTrue(
            Similarity.isSimilar(
                "join my server discord gg abcde for free nitro",
                "join my server discord gg abcde for free nitro today",
            ),
        )
    }

    @Test
    fun `unrelated strings are not similar`() {
        assertFalse(Similarity.isSimilar("good morning everyone", "buy cheap nitro right now"))
    }

    @Test
    fun `jaccard of disjoint sets is zero`() {
        assertEquals(0.0, Similarity.jaccard(setOf("a", "b"), setOf("c", "d")))
    }

    @Test
    fun `jaccard of identical sets is one`() {
        assertEquals(1.0, Similarity.jaccard(setOf("a", "b"), setOf("a", "b")))
    }

    @Test
    fun `jaccard of two empty sets is one`() {
        assertEquals(1.0, Similarity.jaccard(emptySet(), emptySet()))
    }

    @Test
    fun `jaccard with one empty set is zero`() {
        assertEquals(0.0, Similarity.jaccard(setOf("a"), emptySet()))
    }

    @Test
    fun `half overlap is one third by jaccard`() {
        // {a,b} vs {b,c}: intersection 1, union 3
        assertEquals(1.0 / 3.0, Similarity.jaccard(setOf("a", "b"), setOf("b", "c")), 1e-9)
    }

    @Test
    fun `short texts fall back to unigram comparison`() {
        // Fewer tokens than the default shingle size: compared as token sets.
        assertTrue(Similarity.isSimilar("hi", "hi"))
        assertFalse(Similarity.isSimilar("hi", "bye"))
    }

    @Test
    fun `bigram shingles are sensitive to word order`() {
        val a = Similarity.shingles("alpha beta gamma", 2)
        assertEquals(setOf("alpha beta", "beta gamma"), a)
    }
}
