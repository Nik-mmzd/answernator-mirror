package pw.modder.answernator.utils.extensions.kord

import dev.kord.common.entity.Snowflake

val Snowflake.timestamp: Long get() = (value shr 22) + 1420070400000L
val Snowflake.worker: Long get() = (value and 0x3E0000) shr 17
val Snowflake.process: Long get() = (value and 0x1F000) shr 12
val Snowflake.increment: Long get() = value and 0xFFF
val Snowflake.timestampMention: String get() = "<t:${timestamp/1000}:f>"
val Snowflake.relTimestampMention: String get() = "<t:${timestamp/1000}:R>"