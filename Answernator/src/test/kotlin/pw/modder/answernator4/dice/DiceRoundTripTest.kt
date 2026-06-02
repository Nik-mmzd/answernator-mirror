package pw.modder.answernator4.dice

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Round-trip: input parses, serializes to a canonical form (that differs from input —
 * pure round-trips are already covered by parser+serializer tests), and the canonical
 * form parses back to an equal [DiceExpression].
 */
class DiceRoundTripTest {

    private fun assertRoundTrip(input: String, canonical: String) {
        val first = DiceParser.parse(input)
        val serialized = DiceSerializer.serialize(first)
        assertEquals(canonical, serialized, "canonical form for `$input`")

        val second = DiceParser.parse(serialized)
        assertEquals(first, second, "round-trip equality for `$input`")
    }

    @Test fun `empty input expands to default die`() = assertRoundTrip("", "1d6")
    @Test fun `bare N expands to NdDefault`() = assertRoundTrip("4", "4d6")
    @Test fun `dM expands to 1dM`() = assertRoundTrip("d20", "1d20")
    @Test fun `Nd expands to NdDefault`() = assertRoundTrip("3d", "3d6")
    @Test fun `tries via star collapses to x`() = assertRoundTrip("2d6*3", "2d6x3")
    @Test fun `tries via t collapses to x`() = assertRoundTrip("2d6t3", "2d6x3")
    @Test fun `explode zero collapses to bare e`() = assertRoundTrip("2d6e0", "2d6e")
    @Test fun `bracket spec from spec`() = assertRoundTrip("(d4 4 4d 2d4)ek3-1", "(1d4 4d6 4d6 2d4)ek3-1")
    @Test fun `comma separator becomes space`() = assertRoundTrip("1d6,2d20", "1d6 2d20")
    @Test fun `semicolon separator becomes space`() = assertRoundTrip("1d6;2d20", "1d6 2d20")
    @Test fun `separator runs collapse to single space`() = assertRoundTrip("1d6 , ; 2d20", "1d6 2d20")
}