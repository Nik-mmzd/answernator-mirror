package pw.modder.answernator.utils.extensions.kord

import dev.kord.common.entity.Snowflake
import pw.modder.answernator.utils.TimestampFormat
import pw.modder.answernator.utils.mention

val Snowflake.worker: ULong get() = (value and 0x3E0000u) shr 17
val Snowflake.process: ULong get() = (value and 0x1F000u) shr 12
val Snowflake.increment: ULong get() = value and 0xFFFu
val Snowflake.timestampMention: String get() = timestamp.mention()
fun Snowflake.timestampMention(format: TimestampFormat) = timestamp.mention(format)