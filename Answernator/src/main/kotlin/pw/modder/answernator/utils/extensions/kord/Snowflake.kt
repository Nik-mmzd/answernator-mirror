package pw.modder.answernator.utils.extensions.kord

import dev.kord.common.entity.Snowflake
import pw.modder.answernator.utils.TimestampFormat
import pw.modder.answernator.utils.mention

@Deprecated("Switch to v4")
val Snowflake.timestampMention: String get() = timestamp.mention()

@Deprecated("Switch to v4")
fun Snowflake.timestampMention(format: TimestampFormat) = timestamp.mention(format)
