package pw.modder.answernator.utils.extensions.kord

import dev.kord.common.entity.Snowflake
import org.joda.time.Instant

val Snowflake.timestamp: Long get() = (value shr 22) + 1420070400000L
val Snowflake.worker: Long get() = (value and 0x3E0000) shr 17
val Snowflake.process: Long get() = (value and 0x1F000) shr 12
val Snowflake.increment: Long get() = value and 0xFFF
val Snowflake.instant: Instant get() = Instant.ofEpochSecond(timeStamp.epochSeconds)