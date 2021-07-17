package pw.modder.answernator.tools.utils

import dev.kord.common.entity.Snowflake

val Snowflake.timestamp: Long get() = (value shr 22) + 1420070400000L
val Snowflake.worker: Long get() = (value and 0x3E0000) shr 17
val Snowflake.process: Long get() = (value and 0x1F000) shr 12
val Snowflake.increment: Long get() = value and 0xFFF