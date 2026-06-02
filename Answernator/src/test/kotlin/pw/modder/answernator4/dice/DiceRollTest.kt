package pw.modder.answernator4.dice

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DiceRollTest {

    @Test
    fun `rolled value within bounds`() {
        val dice = Dice(faces = 6)
        repeat(200) {
            val v = dice.roll(Random(it.toLong()))
            assertTrue(v in 1..6, "out of bounds: $v")
        }
    }

    @Test
    fun `set roll has correct number of tries`() {
        val set = DiceSet(groups = listOf(DiceGroup(2, 6)), tries = 3)
        val results = set.roll(Random(0))
        assertEquals(3, results.size)
    }

    @Test
    fun `keep retains top N values`() {
        val set = DiceSet(groups = listOf(DiceGroup(4, 6)), keep = 2)
        val results = set.roll(Random(42))
        for (try_ in results) {
            assertEquals(2, try_.values.size)
        }
    }

    @Test
    fun `bracket group expands to all dice`() {
        val set = DiceSet(groups = listOf(DiceGroup(2, 4), DiceGroup(3, 6)))
        val tryResult = set.roll(Random(0)).single()
        assertEquals(5, tryResult.values.size)
    }

    @Test
    fun `explode produces values larger than faces possible`() {
        // With a 2-sided die and many rolls, at least one will explode at some seed.
        val set = DiceSet(groups = listOf(DiceGroup(1, 2)), explode = true, explodeLimit = 10)
        val random = Random(123)
        val anyExploded = (1..200).any { set.roll(random).single().values.single() > 2 }
        assertTrue(anyExploded, "expected at least one exploded result over 200 rolls of 1d2")
    }

    @Test
    fun `explode respects limit`() {
        // 1d2 with limit=1 can explode at most once → max value is 4.
        val set = DiceSet(groups = listOf(DiceGroup(1, 2)), explode = true, explodeLimit = 1)
        val random = Random(7)
        val max = (1..500).maxOf { set.roll(random).single().values.single() }
        assertTrue(max <= 4, "with limit=1 on d2 max should be <= 4, got $max")
    }

    @Test
    fun `keep zero keeps everything`() {
        val set = DiceSet(groups = listOf(DiceGroup(3, 6)), keep = 0)
        val result = set.roll(Random(0)).single()
        assertEquals(3, result.values.size)
    }

    @Test
    fun `dice faces below two rejected`() {
        assertFailsWith<DiceException> { Dice(1) }
        assertFailsWith<DiceException> { DiceGroup(1, 1) }
    }

    @Test
    fun `dices limit enforced`() {
        assertFailsWith<DiceLimitExceededException> {
            DiceSet(groups = listOf(DiceGroup(DiceConfig.dicesLimit + 1, 6)))
        }
    }

    @Test
    fun `modifier limit enforced`() {
        assertFailsWith<DiceLimitExceededException> {
            DiceSet(groups = listOf(DiceGroup(1, 6)), modifier = DiceConfig.modLimit + 1)
        }
        assertFailsWith<DiceLimitExceededException> {
            DiceSet(groups = listOf(DiceGroup(1, 6)), modifier = -(DiceConfig.modLimit + 1))
        }
    }

    @Test
    fun `tries limit enforced`() {
        assertFailsWith<DiceLimitExceededException> {
            DiceSet(groups = listOf(DiceGroup(1, 6)), tries = DiceConfig.triesLimit + 1)
        }
    }

    @Test
    fun `faces limit enforced`() {
        assertFailsWith<DiceLimitExceededException> { Dice(DiceConfig.facesLimit + 1) }
    }
}