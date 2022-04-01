package pw.modder.answernator.tools.diceHelper

import pw.modder.answernator.utils.Globals
import kotlin.math.abs

class DiceLimitExceededException(message: String? = null, val name: String, val value: Int, val limit: Int): Exception(message)

class Dice(val faces: Int) {
    init {
        if (faces > DiceConfig.diceFases)
            throw DiceLimitExceededException("Faces count $faces exceeds limit ${DiceConfig.diceFases}", "faces", faces, DiceConfig.diceFases)
        if (faces < 2)
            throw InvalidArgumentException("Dice faces count ($faces) can't be less that 2")
    }

    fun roll(explode: Boolean, limit: Int): Int {
        return buildList {
            var explodes = 0
            do {
                add(Globals.random.nextInt(1, faces+1))
                if (last() != faces) break
                explodes++
            } while (explode && (limit == 0 || explodes < limit))
        }.sum()
    }
}

class DiceSet(
    val tokens: List<DiceToken>,
    val dices: List<Dice>,
    val explode: Boolean = false,
    val explodeLimit: Int = 0,
    val keep: Int = 0,
    val modifier: Int = 0,
    val tries: Int = 1
): List<Dice> by dices {
    init {
        if (dices.isEmpty() || DiceConfig.dicesLimit in 1 until dices.size)
            throw DiceLimitExceededException("Dices count ${dices.size} exceeds limit ${DiceConfig.dicesLimit}", "dices", dices.size, DiceConfig.dicesLimit)
        if (DiceConfig.explodeLimit in 1 until explodeLimit)
            throw DiceLimitExceededException("Explodes count $explodeLimit exceeds limit ${DiceConfig.explodeLimit}", "explode", explodeLimit, DiceConfig.explodeLimit)
        if (DiceConfig.keepLimit in 1 until keep)
            throw DiceLimitExceededException("Keep count $keep exceeds limit ${DiceConfig.keepLimit}", "keep", keep, DiceConfig.keepLimit)
        if (DiceConfig.modLimit > 0 && abs(modifier) > DiceConfig.modLimit)
            throw DiceLimitExceededException("Modifier $modifier exceeds limit ${DiceConfig.modLimit}", "modifier", modifier, DiceConfig.modLimit)
        if (tries < 1 || DiceConfig.triesLimit in 1 until tries)
            throw DiceLimitExceededException("Tries count $tries exceeds limit ${DiceConfig.triesLimit}", "tries", tries, DiceConfig.triesLimit)
    }

    fun roll(): List<List<Int>> = buildList {
        repeat(tries) {
            val roll = dices.map { it.roll(explode, explodeLimit) }
            when(keep) {
                0 -> add(roll)
                else -> add(roll.sortedDescending().take(keep))
            }
        }
    }
}