package pw.modder.answernator4.antispam

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MessageCanonicalizerTest {

    private fun canon(s: String, atts: List<AttachmentSignature> = emptyList()) =
        MessageCanonicalizer.canonicalize(s, atts)

    @Test
    fun `case and surrounding whitespace are normalized`() {
        assertEquals(canon("Hello World"), canon("  hELLo   world  "))
    }

    @Test
    fun `inner whitespace runs collapse`() {
        assertEquals("a b c", canon("a    b\t\nc"))
    }

    @Test
    fun `digits are stripped`() {
        assertEquals(canon("buy now"), canon("buy now 12345"))
    }

    @Test
    fun `mentions collapse to a placeholder regardless of target`() {
        assertEquals(canon("hi <@111111111>"), canon("hi <@!999999999>"))
    }

    @Test
    fun `custom emoji collapse to a placeholder`() {
        assertEquals(canon("nice <:kek:111>"), canon("nice <a:lol:222>"))
    }

    @Test
    fun `urls reduce to their host`() {
        assertEquals(
            canon("see https://example.com/a/b?x=1"),
            canon("see https://www.example.com/different/path"),
        )
    }

    @Test
    fun `character repeats collapse`() {
        assertEquals(canon("heyy"), canon("heyyyyyyy"))
    }

    @Test
    fun `unicode emoji are removed`() {
        assertEquals(canon("free money"), canon("free 💰💰 money 🎉"))
    }

    @Test
    fun `diacritics are stripped`() {
        assertEquals(canon("cafe resume"), canon("café résumé"))
    }

    @Test
    fun `attachments contribute to the canon`() {
        val a = canon("", listOf(AttachmentSignature("pic.png", 100)))
        val b = canon("", listOf(AttachmentSignature("pic.png", 200)))
        assertNotEquals(a, b)
    }

    @Test
    fun `attachment order does not matter`() {
        val one = AttachmentSignature("a.png", 1)
        val two = AttachmentSignature("b.png", 2)
        assertEquals(
            canon("hi", listOf(one, two)),
            canon("hi", listOf(two, one)),
        )
    }

    @Test
    fun `text with attachment differs from text alone`() {
        assertNotEquals(canon("hi"), canon("hi", listOf(AttachmentSignature("a.png", 1))))
    }

    @Test
    fun `command canon is distinct from text of the same words`() {
        assertNotEquals(MessageCanonicalizer.commandCanon("ban"), canon("ban"))
        assertEquals(MessageCanonicalizer.commandCanon("Ban"), MessageCanonicalizer.commandCanon(" ban "))
    }

    @Test
    fun `empty message canonicalizes to empty string`() {
        assertTrue(canon("").isEmpty())
    }
}
