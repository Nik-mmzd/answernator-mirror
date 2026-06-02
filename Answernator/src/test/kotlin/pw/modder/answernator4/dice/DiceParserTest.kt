package pw.modder.answernator4.dice

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiceParserTest {

    @Test
    fun `empty input yields a single default die`() {
        val expr = DiceParser.parse("")
        assertEquals(1, expr.sets.size)
        val set = expr.sets.single()
        assertEquals(listOf(DiceGroup(DiceConfig.defaultDices, DiceConfig.defaultFaces)), set.groups)
        assertEquals(DiceConfig.defaultTries, set.tries)
        assertEquals(0, set.modifier)
        assertFalse(set.explode)
        assertEquals(0, set.keep)
    }

    @Test
    fun `whitespace-only input yields a single default die`() {
        val expr = DiceParser.parse("   ,;  ")
        assertEquals(1, expr.sets.size)
        assertEquals(listOf(DiceGroup(1, 6)), expr.sets.single().groups)
    }

    @Test
    fun `bare N is N dice of default faces`() {
        val set = DiceParser.parse("4").sets.single()
        assertEquals(listOf(DiceGroup(4, DiceConfig.defaultFaces)), set.groups)
    }

    @Test
    fun `dM is one die with M faces`() {
        val set = DiceParser.parse("d20").sets.single()
        assertEquals(listOf(DiceGroup(1, 20)), set.groups)
    }

    @Test
    fun `Nd is N dice of default faces`() {
        val set = DiceParser.parse("3d").sets.single()
        assertEquals(listOf(DiceGroup(3, DiceConfig.defaultFaces)), set.groups)
    }

    @Test
    fun `NdM parses count and faces`() {
        val set = DiceParser.parse("2d20").sets.single()
        assertEquals(listOf(DiceGroup(2, 20)), set.groups)
    }

    @Test
    fun `positive modifier`() {
        val set = DiceParser.parse("2d6+5").sets.single()
        assertEquals(5, set.modifier)
    }

    @Test
    fun `negative modifier`() {
        val set = DiceParser.parse("2d6-3").sets.single()
        assertEquals(-3, set.modifier)
    }

    @Test
    fun `keep modifier`() {
        val set = DiceParser.parse("4d6k3").sets.single()
        assertEquals(3, set.keep)
    }

    @Test
    fun `tries accepts t x and star`() {
        for (input in listOf("2d6t4", "2d6x4", "2d6*4")) {
            val set = DiceParser.parse(input).sets.single()
            assertEquals(4, set.tries, "input=$input")
        }
    }

    @Test
    fun `explode with limit`() {
        val set = DiceParser.parse("2d6e3").sets.single()
        assertTrue(set.explode)
        assertEquals(3, set.explodeLimit)
    }

    @Test
    fun `bare e and e0 both mean unlimited explode`() {
        for (input in listOf("2d6e", "2d6e0")) {
            val set = DiceParser.parse(input).sets.single()
            assertTrue(set.explode, "input=$input")
            assertEquals(0, set.explodeLimit, "input=$input")
        }
    }

    @Test
    fun `bracket group with mixed dice`() {
        val set = DiceParser.parse("(d4 4 4d 2d4)ek3-1").sets.single()
        assertEquals(
            listOf(DiceGroup(1, 4), DiceGroup(4, 6), DiceGroup(4, 6), DiceGroup(2, 4)),
            set.groups,
        )
        assertTrue(set.explode)
        assertEquals(0, set.explodeLimit)
        assertEquals(3, set.keep)
        assertEquals(-1, set.modifier)
        assertTrue(set.bracketed)
    }

    @Test
    fun `multiple sets separated by space, comma, semicolon`() {
        val expr = DiceParser.parse("1d6 2d8,3d10;4d12")
        assertEquals(4, expr.sets.size)
        assertEquals(listOf(DiceGroup(1, 6)), expr.sets[0].groups)
        assertEquals(listOf(DiceGroup(2, 8)), expr.sets[1].groups)
        assertEquals(listOf(DiceGroup(3, 10)), expr.sets[2].groups)
        assertEquals(listOf(DiceGroup(4, 12)), expr.sets[3].groups)
    }

    @Test
    fun `runs of mixed separators collapse`() {
        val expr = DiceParser.parse("1d6 , ; 2d8")
        assertEquals(2, expr.sets.size)
    }

    @Test
    fun `all modifiers together`() {
        val set = DiceParser.parse("4d6e2k3x2+5").sets.single()
        assertEquals(listOf(DiceGroup(4, 6)), set.groups)
        assertTrue(set.explode)
        assertEquals(2, set.explodeLimit)
        assertEquals(3, set.keep)
        assertEquals(2, set.tries)
        assertEquals(5, set.modifier)
    }

    @Test
    fun `duplicate modifier throws`() {
        assertFailsWith<DiceParseException> { DiceParser.parse("2d6+1+2") }
        assertFailsWith<DiceParseException> { DiceParser.parse("2d6k1k2") }
        assertFailsWith<DiceParseException> { DiceParser.parse("2d6ee") }
        assertFailsWith<DiceParseException> { DiceParser.parse("2d6t2x2") }
    }

    @Test
    fun `missing required number throws`() {
        assertFailsWith<DiceParseException> { DiceParser.parse("2d6k") }
        assertFailsWith<DiceParseException> { DiceParser.parse("2d6+") }
        assertFailsWith<DiceParseException> { DiceParser.parse("2d6-") }
        assertFailsWith<DiceParseException> { DiceParser.parse("2d6t") }
    }

    @Test
    fun `unknown character throws`() {
        assertFailsWith<DiceParseException> { DiceParser.parse("2d6?") }
    }

    @Test
    fun `unbalanced brackets throw`() {
        assertFailsWith<DiceParseException> { DiceParser.parse("(2d6") }
        assertFailsWith<DiceParseException> { DiceParser.parse("2d6)") }
    }

    @Test
    fun `empty bracket group throws`() {
        assertFailsWith<DiceParseException> { DiceParser.parse("()") }
    }

    @Test
    fun `nested brackets are rejected`() {
        // After a top-level set, the next token must be a separator. '(' isn't.
        assertFailsWith<DiceParseException> { DiceParser.parse("((2d6))") }
    }
}