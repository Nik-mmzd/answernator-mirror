package pw.modder.answernator.utils.extensions

import java.util.*

fun ResourceBundle.getStringSafe(key: String): String? {
    return try {
        getString(key)
    } catch (_: MissingResourceException) {
        null
    }
}
fun ResourceBundle.getStringOrKey(key: String): String = getStringSafe(key) ?: key



fun ResourceBundle.formatString(key: String, vararg args: Any): String {
    return String.format(getStringOrKey(key), args = *args)
}