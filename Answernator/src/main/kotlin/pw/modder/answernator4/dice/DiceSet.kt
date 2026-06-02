package pw.modder.answernator4.dice

import kotlin.math.abs
import kotlin.random.Random

data class DiceSet(
    val groups: List<DiceGroup>,
    val keep: Int = DiceConfig.defaultKeep,
    val explode: Boolean = false,
    val explodeLimit: Int = DiceConfig.defaultExplodeLimit,
    val tries: Int = DiceConfig.defaultTries,
    val modifier: Int = DiceConfig.defaultModifier,
) {
    init {
        if (groups.isEmpty()) throw DiceException("DiceSet must contain at least one group")

        val total = groups.sumOf { it.count }
        if (DiceConfig.dicesLimit > 0 && total > DiceConfig.dicesLimit) {
            throw DiceLimitExceededException(
                "Dices count $total exceeds limit ${DiceConfig.dicesLimit}",
                "dices", total, DiceConfig.dicesLimit,
            )
        }
        if (keep < 0) throw DiceException("Keep must be >= 0 (got $keep)")
        if (DiceConfig.keepLimit > 0 && keep > DiceConfig.keepLimit) {
            throw DiceLimitExceededException(
                "Keep count $keep exceeds limit ${DiceConfig.keepLimit}",
                "keep", keep, DiceConfig.keepLimit,
            )
        }
        if (explodeLimit < 0) throw DiceException("Explode limit must be >= 0 (got $explodeLimit)")
        if (DiceConfig.explodeLimit > 0 && explodeLimit > DiceConfig.explodeLimit) {
            throw DiceLimitExceededException(
                "Explode limit $explodeLimit exceeds limit ${DiceConfig.explodeLimit}",
                "explode", explodeLimit, DiceConfig.explodeLimit,
            )
        }
        if (tries < 1) throw DiceException("Tries must be >= 1 (got $tries)")
        if (DiceConfig.triesLimit > 0 && tries > DiceConfig.triesLimit) {
            throw DiceLimitExceededException(
                "Tries count $tries exceeds limit ${DiceConfig.triesLimit}",
                "tries", tries, DiceConfig.triesLimit,
            )
        }
        if (DiceConfig.modLimit > 0 && abs(modifier) > DiceConfig.modLimit) {
            throw DiceLimitExceededException(
                "Modifier $modifier exceeds limit ${DiceConfig.modLimit}",
                "modifier", modifier, DiceConfig.modLimit,
            )
        }
    }

    /** True when this set contains multiple groups, i.e. should be rendered as `(... ...)`. */
    val bracketed: Boolean get() = groups.size > 1

    /** Total number of dice across all groups. */
    val totalDice: Int get() = groups.sumOf { it.count }

    data class TryResult(val values: List<Int>, val modifier: Int) {
        val total: Int get() = values.sum() + modifier
    }

    fun roll(random: Random = Random.Default): List<TryResult> {
        val dice = groups.flatMap { group -> List(group.count) { Dice(group.faces) } }
        return List(tries) {
            val rolls = dice.map { it.roll(random, explode, explodeLimit) }
            val kept = if (keep in 1 until rolls.size) rolls.sortedDescending().take(keep) else rolls
            TryResult(kept, modifier)
        }
    }

    override fun toString(): String = DiceSerializer.serialize(this)
}