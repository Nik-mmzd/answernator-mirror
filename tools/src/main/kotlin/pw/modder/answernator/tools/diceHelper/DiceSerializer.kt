package pw.modder.answernator.tools.diceHelper


class DiceSerializer(val input: List<DiceToken>) {
    fun stringify(): String {
        val b = StringBuilder()

        input.forEach{
            when(it.type) {
                DiceTokens.DICES -> b.append(it.value)
                DiceTokens.FACES -> b.append("d", it.value)
                DiceTokens.SPACE -> b.append(" ")
                DiceTokens.EXPLODE -> b.append("e", it.value)
                DiceTokens.KEEP -> b.append("k", it.value)
                DiceTokens.TRIES -> b.append("x", it.value)
                DiceTokens.NEGATIVE_MODIFIER -> b.append("-", it.value)
                DiceTokens.POSITIVE_MODIFIER -> b.append("+", it.value)
                DiceTokens.RIGHT_BRACKET -> b.append("(")
                DiceTokens.LEFT_BRACKET -> b.append(")")
            }
        }

        return b.toString()
    }
}