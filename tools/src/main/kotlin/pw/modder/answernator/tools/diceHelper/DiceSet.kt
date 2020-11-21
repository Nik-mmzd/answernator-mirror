package pw.modder.answernator.tools.diceHelper

import kotlin.random.Random

private val random = Random(System.currentTimeMillis())
data class DiceSet(val max: Int, val count: Int, val modifier: Int, val tries: Int) {

    @OptIn(ExperimentalStdlibApi::class)
    fun roll(): List<List<Int>> {
        return buildList {
            repeat(tries) {
                add(buildList {
                    repeat(count) {
                        add(random.nextInt(1, max+1))
                    }
                })
            }
        }
    }

    companion object {
        fun parse(string: String): DiceSet {
            val diceConfig = DiceTokenizer.parse(string)

            return DiceSet(
                max = diceConfig.getOrDefaultLimited("d", DiceConfig.defautDicePips, DiceConfig.dotsLimit).also { if (it < 2) throw DiceLimitExceededException("Pips count for dice cannot be less that 2!") },
                count = diceConfig.getOrDefaultLimited("", DiceConfig.defaultDices, DiceConfig.dicesLimit),
                modifier = diceConfig.getOrDefaultLimited("+", DiceConfig.defaultModifier, DiceConfig.modLimit),
                tries = diceConfig.getOrDefaultLimited("x", DiceConfig.defaultTries, DiceConfig.triesLimit)
            )
        }
    }
}