import kotlinx.coroutines.runBlocking
import pw.modder.answernator.tools.diceHelper.DiceParser
import pw.modder.answernator.tools.diceHelper.DiceSerializer
import pw.modder.answernator.tools.diceHelper.DiceTokenizer

fun main() {
    val testStrings = listOf("2d3k9+5x4e 2d3k9+5x4e 2d3k9+5x4e", "e3(2d5 3d8 4d12)x5")

    testStrings.forEach { runBlocking {
        try {
            val tokens = DiceTokenizer(it).tokenize()
            println("String $it, tokens $tokens")
            val set = DiceParser(DiceTokenizer(it)).parse()
            println("String $it, set $set")
            val restring = set.map { DiceSerializer(it.tokens).stringify() }
            println("String $it, serialized $restring")
        } catch (e: Exception) {
            println("String $it, caught exception:")
            e.printStackTrace()
            e.cause?.printStackTrace()
        }
    } }
}