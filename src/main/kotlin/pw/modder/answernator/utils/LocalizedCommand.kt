package pw.modder.answernator.utils

import kotlinx.serialization.UnstableDefault
import java.util.*

@UnstableDefault
interface LocalizedCommand: Command {
    val texts: Map<Locale, ResourceBundle>
        get() = lang.associateBy({ it }, { ResourceBundle.getBundle("locale.$name", it) })

    fun getString(locale: Locale, str: String): String {
        return texts[locale]?.getString("$name.$str") ?: "$name.$str"
    }

    fun formatString(locale: Locale, str: String, vararg args: Any?): String {
        return String.format(getString(locale, str), args = *arrayOf(args))
    }
}
