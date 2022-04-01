package pw.modder.answernator.tools.diceHelper

private val DiceTokenRegex = Regex("""([^\d\s]?)(\d+)""")

class InvalidArgumentException(message: String? = null, cause: Throwable? = null): Exception(message, cause)

enum class DiceTokens(
    val allowEmpty: Boolean = false,
    val allowCombined: Boolean = false,
    val hasValue: Boolean = true
) {
    DICES(allowCombined = true),
    FACES(allowCombined = true, allowEmpty = true),
    KEEP,
    EXPLODE(allowEmpty = true),
    POSITIVE_MODIFIER,
    NEGATIVE_MODIFIER,
    TRIES,
    LEFT_BRACKET(hasValue = false),
    RIGHT_BRACKET(hasValue = false),
    SPACE(hasValue = false, allowCombined = true)
}

private val dice_tokens = mapOf(
    'd' to DiceTokens.FACES,//
    'k' to DiceTokens.KEEP,//
    'e' to DiceTokens.EXPLODE,//
    '(' to DiceTokens.LEFT_BRACKET,//
    ')' to DiceTokens.RIGHT_BRACKET,//
    'x' to DiceTokens.TRIES,//
    't' to DiceTokens.TRIES,//
    '*' to DiceTokens.TRIES,//
    '+' to DiceTokens.POSITIVE_MODIFIER,
    '-' to DiceTokens.NEGATIVE_MODIFIER,
    ' ' to DiceTokens.SPACE,
    ';' to DiceTokens.SPACE,
    ',' to DiceTokens.SPACE
)

data class DiceToken(val type: DiceTokens, val value: String)

class DiceTokenizer(val input: String) {
    fun tokenize(): List<DiceToken> {
        return buildList {
            var tokenPos = 0
            while (tokenPos < input.length) {
                val token = dice_tokens[input[tokenPos]]
                    ?: DiceTokens.DICES.takeIf { input[tokenPos].isDigit() }
                    ?: throw InvalidArgumentException("Found unknown token ${input[tokenPos]}")

                if (token == DiceTokens.SPACE && (isEmpty() || last().type == DiceTokens.SPACE)) {
                    tokenPos++
                    continue
                }

                if (!token.hasValue) {
                    tokenPos++
                    add(DiceToken(token, ""))
                    continue
                }

                if (token == DiceTokens.DICES)
                    tokenPos--

                var valueLastPos = 1
                while (tokenPos + valueLastPos <= input.lastIndex && input[tokenPos + valueLastPos].isDigit()) {
                    valueLastPos++
                }

                if (valueLastPos == 1 && !token.allowEmpty)
                    throw InvalidArgumentException("Token ${token.name} must have any value")

                add(DiceToken(token, input.substring(tokenPos + 1, tokenPos + valueLastPos)))
                tokenPos += valueLastPos
            }
        }
    }
}