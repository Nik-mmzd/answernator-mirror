package pw.modder.answernator.utils.extensions

fun String.removeGraves(): String {
    return replace("`", "")
}
private enum class MentionType(val prefix: String) { USER("@"), USERNAME("@!"), CHANNEL("#"), ROLE("@&") }

private fun String.toMention(mentionType: MentionType): String {
    if (isEmpty()) return this
    return "<${mentionType.prefix}$this>"
}

fun String.toUserMention(): String = toMention(MentionType.USER)
fun String.toChannelMention(): String = toMention(MentionType.CHANNEL)
fun String.toRoleMention(): String = toMention(MentionType.ROLE)

private fun String.isMention(mentionType: MentionType): Boolean {
    return Regex("<${mentionType.prefix}\\d{18}>").matches(this)
}

fun String.isUserMention(): Boolean = isMention(MentionType.USER) || isMention(MentionType.USERNAME)
fun String.isChannelMention(): Boolean = isMention(MentionType.CHANNEL)
fun String.isRoleMention(): Boolean = isMention(MentionType.ROLE)
