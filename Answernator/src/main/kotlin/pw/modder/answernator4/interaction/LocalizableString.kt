package pw.modder.answernator4.interaction

data class LocalizableString(
    val key: String
) {
    companion object {
        val EMPTY: LocalizableString = LocalizableString("")
    }
}

fun String.asLocaleKey() = LocalizableString(this)