package pw.modder.answernator.tools.diceHelper

class DiceParser(val tokenizer: DiceTokenizer) {
    @OptIn(ExperimentalStdlibApi::class)
    fun parse(): List<DiceSet> {
        val tokens = tokenizer.tokenize()
        val result = mutableListOf<DiceSet>()

        // split to raw sets. Check brackets.
        buildList {
            var start = 0
            var insideBrackets = false

            tokens.forEachIndexed { index, diceToken ->
                if (diceToken.type == DiceTokens.SPACE && !insideBrackets) {
                    add(tokens.subList(start, index))
                    start = index+1
                    return@forEachIndexed
                }

                if (diceToken.type == DiceTokens.LEFT_BRACKET) {
                    if (insideBrackets)
                        throw InvalidArgumentException("Multiple brackets inside one another not allowed")
                    insideBrackets = true
                }

                if (diceToken.type == DiceTokens.RIGHT_BRACKET) {
                    if (!insideBrackets)
                        throw InvalidArgumentException("Impossible to close brackets without opening one")
                    insideBrackets = false
                }

                if (index == tokens.lastIndex) {
                    add(tokens.subList(start, index+1))
                }
            }
        }.forEach { rawDiceSet ->
            val duplicates = rawDiceSet.filterNot { it.type.allowCombined }.groupingBy { it.type }.eachCount().filter { it.value > 1 }

            if (duplicates.isNotEmpty()) {
                throw InvalidArgumentException("Tokens ${duplicates.keys.joinToString()} must be specified only once")
            }

            val betweenBrackets = try {
                rawDiceSet.subList(
                    rawDiceSet.indexOfFirst { it.type == DiceTokens.LEFT_BRACKET } + 1,
                    rawDiceSet.indexOfFirst { it.type == DiceTokens.RIGHT_BRACKET }
                )
            } catch (_: IndexOutOfBoundsException) {
                listOf<DiceToken>()
            } catch (_: IllegalArgumentException) {
                listOf<DiceToken>()
            }

            val outsideBrackets = rawDiceSet.filterNot { it in betweenBrackets || it.type == DiceTokens.LEFT_BRACKET || it.type == DiceTokens.RIGHT_BRACKET }

            if (betweenBrackets.any { !it.type.allowCombined })
                throw InvalidArgumentException("Brackets contains invalid tokens")

            if (betweenBrackets.isNotEmpty() && outsideBrackets.filterNot { it.type == DiceTokens.SPACE }.any { it.type.allowCombined })
                throw InvalidArgumentException("Combinable tokens outside brackets")

            val dices = mutableListOf<Dice>()
            var faces: Int = DiceConfig.defautDiceFaces
            var dicesCount: Int = DiceConfig.defaultDices
            var lastToken = DiceTokens.SPACE

            // parse dices in brackets
            betweenBrackets.forEachIndexed { index, token ->
                when(token.type) {
                    DiceTokens.DICES -> when(lastToken) {
                        DiceTokens.DICES -> {
                            repeat(dicesCount) { dices.add(Dice(faces)) }
                            dicesCount = token.value.toInt()
                            faces = DiceConfig.defautDiceFaces
                        }
                        else -> {
                            dicesCount = token.value.toInt()
                            lastToken = token.type
                        }
                    }
                    DiceTokens.FACES -> when(lastToken) {
                        DiceTokens.FACES -> {
                            repeat(dicesCount) { dices.add(Dice(faces)) }
                            faces = token.value.toIntOrNull() ?: DiceConfig.defautDiceFaces
                            dicesCount = DiceConfig.defaultDices
                        }
                        else -> {
                            faces = token.value.toIntOrNull() ?: DiceConfig.defautDiceFaces
                            lastToken = token.type
                        }
                    }
                    else -> {
                        repeat(dicesCount) { dices.add(Dice(faces)) }
                        faces = DiceConfig.defautDiceFaces
                        dicesCount = DiceConfig.defaultDices
                        lastToken = token.type
                    }
                }

                if (index == betweenBrackets.lastIndex) repeat(dicesCount) { dices.add(Dice(faces)) }
            }

            // parse other tokens
            var keep = DiceConfig.defaultKeep
            var explode = DiceConfig.defaultExplode
            var tries = DiceConfig.defaultTries
            var mod = DiceConfig.defaultModifier

            outsideBrackets.forEach { token ->
                when(token.type) {
                    DiceTokens.DICES -> dicesCount = token.value.toInt()
                    DiceTokens.FACES -> faces = token.value.toIntOrNull() ?: DiceConfig.defautDiceFaces
                    DiceTokens.KEEP -> keep = token.value.toInt()
                    DiceTokens.TRIES -> tries = token.value.toInt()
                    DiceTokens.POSITIVE_MODIFIER -> mod = token.value.toInt()
                    DiceTokens.NEGATIVE_MODIFIER -> mod = -token.value.toInt()
                    DiceTokens.EXPLODE -> explode = token.value.toIntOrNull() ?: 0
                    else -> {}
                }
            }

            if (dices.isEmpty()) repeat(dicesCount) { dices.add(Dice(faces)) }

            result.add(
                DiceSet(
                    dices = dices,
                    explode = explode > -1,
                    explodeLimit = explode,
                    keep = keep,
                    tries = tries,
                    modifier = mod,
                    tokens = rawDiceSet
            ))
        }

        return result
    }
}