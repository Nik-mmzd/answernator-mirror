package pw.modder.answernator.tools.utils

class Snowflake(val snowflake: Long) {
    constructor(snowflake: String): this(snowflake.toLong())

    val timestamp: Long = (snowflake shr 22) + 1420070400000L
    val worker: Long = (snowflake and 0x3E0000) shr 17
    val process: Long = (snowflake and 0x1F000) shr 12
    val increment: Long = snowflake and 0xFFF
}