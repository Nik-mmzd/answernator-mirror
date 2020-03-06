package pw.modder.answernator.utils

import kotlinx.serialization.UnstableDefault
import mu.KLogger
import mu.KotlinLogging
import java.util.*

private val logger: KLogger = KotlinLogging.logger {}
@UnstableDefault
interface LocalizedCommand: Command {
    val texts: Map<Locale, ResourceBundle>
        get() = lang.associateBy({ it }, {
            ResourceBundle.clearCache(javaClass.classLoader)
            ResourceBundle.getBundle("locale.$name", it, javaClass.classLoader, UTF8Control())
        })

    fun getString(locale: Locale, str: String): String {
        logger.debug { "getting string \"$str\" for locale ${locale.toLanguageTag()}" }
        return try {
            texts[locale]?.getString("$name.$str") ?: "$name.$str"
        } catch (_: MissingResourceException) {
            "$name.$str"
        }
    }

    fun formatString(locale: Locale, str: String, vararg arguments: Any?): String {
        return String.format(getString(locale, str), args = *arguments)
    }

    override fun getHelp(locale: Locale): String? {
        return getString(locale, "help")
    }
}
