package pw.modder.answernator.utils.extensions

fun String.removeGraves(): String {
    return replace("`", "")
}
private enum class MentionType(val prefix: String) { USER("@"), CHANNEL("#"), ROLE("@&") }

private fun String.toMention(mentionType: MentionType): String {
    if (isEmpty()) return this
    return "<${mentionType.prefix}$this>"
}

fun String.toUserMention(): String = toMention(MentionType.USER)
fun String.toChannelMention(): String = toMention(MentionType.CHANNEL)
fun String.toRoleMention(): String = toMention(MentionType.ROLE)