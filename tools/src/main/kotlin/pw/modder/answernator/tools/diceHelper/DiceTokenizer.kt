package pw.modder.answernator.tools.diceHelper

private val DiceTokenRegex = Regex("""([^\d\s]?)(\d+)""")

class DiceLimitExceededException(message: String?, cause: Exception? = null): Exception(message, cause)

data class DiceTokenizer(val tokens: Map<String, Int>): Map<String, Int> by tokens {
    fun getOrDefault(key: String, default: Int): Int {
        return get(key) ?: default
    }

    fun getOrDefaultLimited(key: String, default: Int, limit: Int): Int {
        val value = get(key) ?: return default
        if (value > limit) throw DiceLimitExceededException("Key $key, value $value exceeds limit $limit!")
        return value
    }

    companion object {
        fun parse(string: String): DiceTokenizer {
            return DiceTokenizer(DiceTokenRegex.findAll(string).associate { it.groupValues[1] to it.groupValues[2].toInt() })
        }
    }
}