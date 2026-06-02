package pw.modder.answernator4.dice

open class DiceException(message: String) : RuntimeException(message)

class DiceParseException(message: String, val position: Int) : DiceException(message)

class DiceLimitExceededException(
    message: String,
    val name: String,
    val value: Int,
    val limit: Int,
) : DiceException(message)