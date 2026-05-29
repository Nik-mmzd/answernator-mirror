package pw.modder.answernator.utils.extensions

@Deprecated("Switch to v4")
fun String.removeGraves(): String {
    return replace("`", "")
}

@Deprecated("Switch to v4")
private enum class MentionType(val prefix: String) {
    USER("@"), NICKNAME("@!"), CHANNEL("#"), ROLE("@&")
}

@Deprecated("Switch to v4")
private fun String.toMention(mentionType: MentionType, suffix: String? = null): String {
    if (isEmpty()) return this
    if (suffix == null)
        return "<${mentionType.prefix}$this>"
    return "<${mentionType.prefix}$this:$suffix>"
}

fun String.toUserMention(): String = toMention(MentionType.USER)
fun String.toChannelMention(): String = toMention(MentionType.CHANNEL)
fun String.toRoleMention(): String = toMention(MentionType.ROLE)

private fun String.isMention(mentionType: MentionType): Boolean {
    return Regex("<${mentionType.prefix}\\d{18}>").matches(this)
}

fun String.isUserMention(): Boolean = isMention(MentionType.USER) || isMention(MentionType.NICKNAME)
fun String.isChannelMention(): Boolean = isMention(MentionType.CHANNEL)
fun String.isRoleMention(): Boolean = isMention(MentionType.ROLE)

private val SNOWFLAKE_REGEX = Regex("\\d{18}")
fun String.extractMentionedId(): String? {
    return SNOWFLAKE_REGEX.find(this)?.value
}
