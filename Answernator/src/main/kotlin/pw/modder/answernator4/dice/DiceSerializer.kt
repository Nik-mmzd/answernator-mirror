package pw.modder.answernator4.dice

/**
 * Canonical serializer for [DiceExpression] / [DiceSet]. Top-level sets are joined by a single space;
 * a bracket group is emitted only when a set has more than one [DiceGroup].
 */
object DiceSerializer {
    fun serialize(expression: DiceExpression): String =
        expression.sets.joinToString(separator = " ") { serialize(it) }

    fun serialize(set: DiceSet): String = buildString {
        if (set.bracketed) {
            append('(')
            set.groups.forEachIndexed { index, group ->
                if (index > 0) append(' ')
                appendGroup(group)
            }
            append(')')
        } else {
            appendGroup(set.groups.single())
        }

        if (set.explode) {
            append('e')
            if (set.explodeLimit > 0) append(set.explodeLimit)
        }
        if (set.keep > 0) {
            append('k').append(set.keep)
        }
        if (set.tries != DiceConfig.defaultTries) {
            append('x').append(set.tries)
        }
        when {
            set.modifier > 0 -> append('+').append(set.modifier)
            set.modifier < 0 -> append('-').append(-set.modifier)
        }
    }

    private fun StringBuilder.appendGroup(group: DiceGroup) {
        append(group.count).append('d').append(group.faces)
    }
}