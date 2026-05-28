package pw.modder.answernator4.dice

/**
 * Recursive-descent parser for the dice expression grammar.
 *
 * ```
 * Expression  := Set (Separator+ Set)*
 * Set         := DicePart Modifier*
 * DicePart    := BracketGroup | SimpleDice
 * BracketGroup:= '(' SimpleDice (Separator+ SimpleDice)* ')'
 * SimpleDice  := Number ('d' Number?)?      // bare N → N dice of default faces
 *              | 'd' Number?                 // bare d[N] → 1 die of N (or default) faces
 * Modifier    := 'k' Number
 *              | ('t'|'x'|'*') Number
 *              | 'e' Number?                 // 0 / omitted → unlimited
 *              | ('+'|'-') Number
 * Separator   := ' ' | ',' | ';'
 * ```
 *
 * Empty input is treated as a single default die (`1d6`).
 */
object DiceParser {
    private const val SEPARATORS = " ,;"

    fun parse(input: String): DiceExpression {
        val s = State(input)
        s.skipSeparators()
        if (s.eof) {
            return DiceExpression(
                listOf(DiceSet(groups = listOf(DiceGroup(DiceConfig.defaultDices, DiceConfig.defaultFaces)))),
            )
        }

        val sets = mutableListOf(parseSet(s))
        while (s.consumeSeparators() && !s.eof) {
            sets.add(parseSet(s))
        }
        if (!s.eof) {
            throw DiceParseException("Unexpected character '${s.peek()}' at position ${s.pos}", s.pos)
        }
        return DiceExpression(sets)
    }

    private fun parseSet(s: State): DiceSet {
        val groups = if (s.peek() == '(') parseBracketGroup(s) else listOf(parseSimpleDice(s))

        var keep: Int? = null
        var explode = false
        var explodeLimit: Int? = null
        var tries: Int? = null
        var modifier: Int? = null

        while (!s.eof && s.peek() !in SEPARATORS) {
            when (val c = s.peek()) {
                'k' -> {
                    if (keep != null) duplicate(s, "keep")
                    s.advance()
                    keep = s.requireNumber("keep")
                }
                't', 'x', '*' -> {
                    if (tries != null) duplicate(s, "tries")
                    s.advance()
                    tries = s.requireNumber("tries")
                }
                'e' -> {
                    if (explode) duplicate(s, "explode")
                    s.advance()
                    explode = true
                    explodeLimit = s.optionalNumber()
                }
                '+' -> {
                    if (modifier != null) duplicate(s, "modifier")
                    s.advance()
                    modifier = s.requireNumber("modifier")
                }
                '-' -> {
                    if (modifier != null) duplicate(s, "modifier")
                    s.advance()
                    modifier = -s.requireNumber("modifier")
                }
                else -> throw DiceParseException("Unexpected character '$c' at position ${s.pos}", s.pos)
            }
        }

        return DiceSet(
            groups = groups,
            keep = keep ?: DiceConfig.defaultKeep,
            explode = explode,
            explodeLimit = explodeLimit ?: DiceConfig.defaultExplodeLimit,
            tries = tries ?: DiceConfig.defaultTries,
            modifier = modifier ?: DiceConfig.defaultModifier,
        )
    }

    private fun parseBracketGroup(s: State): List<DiceGroup> {
        s.expect('(')
        s.skipSeparators()
        val groups = mutableListOf<DiceGroup>()
        if (!s.eof && s.peek() != ')') {
            groups.add(parseSimpleDice(s))
            while (s.consumeSeparators() && !s.eof && s.peek() != ')') {
                groups.add(parseSimpleDice(s))
            }
        }
        s.expect(')')
        if (groups.isEmpty()) {
            throw DiceParseException("Empty bracket group at position ${s.pos}", s.pos)
        }
        return groups
    }

    private fun parseSimpleDice(s: State): DiceGroup {
        var count: Int? = null
        var faces: Int? = null
        var matched = false

        if (!s.eof && s.peek().isDigit()) {
            count = s.requireNumber("dice count")
            matched = true
        }
        if (!s.eof && s.peek() == 'd') {
            s.advance()
            matched = true
            if (!s.eof && s.peek().isDigit()) {
                faces = s.requireNumber("faces")
            }
        }
        if (!matched) {
            throw DiceParseException(
                "Expected dice spec at position ${s.pos}, got '${if (s.eof) "<end>" else s.peek().toString()}'",
                s.pos,
            )
        }
        return DiceGroup(
            count = count ?: DiceConfig.defaultDices,
            faces = faces ?: DiceConfig.defaultFaces,
        )
    }

    private fun duplicate(s: State, name: String): Nothing =
        throw DiceParseException("Duplicate $name modifier at position ${s.pos}", s.pos)

    private class State(val input: String) {
        var pos = 0
        val eof get() = pos >= input.length
        fun peek(): Char = input[pos]
        fun advance() { pos++ }

        fun expect(c: Char) {
            if (eof || input[pos] != c) {
                throw DiceParseException("Expected '$c' at position $pos", pos)
            }
            pos++
        }

        fun skipSeparators() {
            while (!eof && input[pos] in SEPARATORS) pos++
        }

        /** Consume one or more separators. Returns true if at least one was consumed. */
        fun consumeSeparators(): Boolean {
            if (eof || input[pos] !in SEPARATORS) return false
            skipSeparators()
            return true
        }

        fun requireNumber(what: String): Int {
            val start = pos
            while (!eof && input[pos].isDigit()) pos++
            if (start == pos) {
                throw DiceParseException("Expected number for $what at position $pos", pos)
            }
            return input.substring(start, pos).toInt()
        }

        fun optionalNumber(): Int? {
            if (eof || !input[pos].isDigit()) return null
            return requireNumber("number")
        }
    }
}