package pw.modder.answernator4.dice

import kotlin.random.Random

/** A top-level dice expression: one or more [DiceSet]s separated by ` `, `,` or `;`. */
data class DiceExpression(val sets: List<DiceSet>) {
    init {
        if (sets.isEmpty()) throw DiceException("DiceExpression must contain at least one set")
    }

    fun roll(random: Random = Random.Default): List<List<DiceSet.TryResult>> =
        sets.map { it.roll(random) }

    override fun toString(): String = DiceSerializer.serialize(this)

    companion object {
        fun parse(input: String): DiceExpression = DiceParser.parse(input)
    }
}