package pw.modder.answernator4.dice

import kotlin.test.Test
import kotlin.test.assertEquals

class DiceSerializerTest {

    @Test
    fun `simple set without modifiers`() {
        val set = DiceSet(groups = listOf(DiceGroup(2, 6)))
        assertEquals("2d6", DiceSerializer.serialize(set))
    }

    @Test
    fun `set with positive modifier`() {
        val set = DiceSet(groups = listOf(DiceGroup(2, 6)), modifier = 5)
        assertEquals("2d6+5", DiceSerializer.serialize(set))
    }

    @Test
    fun `set with negative modifier`() {
        val set = DiceSet(groups = listOf(DiceGroup(2, 6)), modifier = -3)
        assertEquals("2d6-3", DiceSerializer.serialize(set))
    }

    @Test
    fun `set with keep`() {
        val set = DiceSet(groups = listOf(DiceGroup(4, 6)), keep = 3)
        assertEquals("4d6k3", DiceSerializer.serialize(set))
    }

    @Test
    fun `set with explode limit`() {
        val set = DiceSet(groups = listOf(DiceGroup(2, 6)), explode = true, explodeLimit = 3)
        assertEquals("2d6e3", DiceSerializer.serialize(set))
    }

    @Test
    fun `set with unlimited explode omits the zero`() {
        val set = DiceSet(groups = listOf(DiceGroup(2, 6)), explode = true)
        assertEquals("2d6e", DiceSerializer.serialize(set))
    }

    @Test
    fun `set with tries`() {
        val set = DiceSet(groups = listOf(DiceGroup(1, 20)), tries = 3)
        assertEquals("1d20x3", DiceSerializer.serialize(set))
    }

    @Test
    fun `bracketed group with multiple modifiers`() {
        val set = DiceSet(
            groups = listOf(DiceGroup(1, 4), DiceGroup(4, 6), DiceGroup(4, 6), DiceGroup(2, 4)),
            keep = 3,
            explode = true,
            modifier = -1,
        )
        assertEquals("(1d4 4d6 4d6 2d4)ek3-1", DiceSerializer.serialize(set))
    }

    @Test
    fun `modifier order is e k x then sign`() {
        val set = DiceSet(
            groups = listOf(DiceGroup(4, 6)),
            keep = 3,
            explode = true,
            explodeLimit = 2,
            tries = 2,
            modifier = 5,
        )
        assertEquals("4d6e2k3x2+5", DiceSerializer.serialize(set))
    }

    @Test
    fun `multiple sets joined by space`() {
        val expr = DiceExpression(
            listOf(
                DiceSet(listOf(DiceGroup(1, 6))),
                DiceSet(listOf(DiceGroup(2, 20))),
            ),
        )
        assertEquals("1d6 2d20", DiceSerializer.serialize(expr))
    }

}