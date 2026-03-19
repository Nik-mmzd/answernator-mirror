package pw.modder.answernator4.interaction

import dev.kord.common.Locale
import dev.kord.core.Kord
import dev.kord.core.event.interaction.InteractionCreateEvent
import pw.modder.answernator.utils.locale.UTF8Control
import java.util.ResourceBundle

private val SUPPORTED_LOCALES = mapOf(
    Locale("en-US") to java.util.Locale.ROOT,
    Locale("ru") to java.util.Locale("ru")
)

internal fun getAllLocalizations(bundleName: String, key: String): Pair<String, Map<Locale, String>> {
    val translations = mutableMapOf<Locale, String>()
    var name = key

    SUPPORTED_LOCALES.forEach { (kordLocale, javaLocale) ->
        try {
            val bundle = ResourceBundle.getBundle("locale.$bundleName", javaLocale, UTF8Control)
            if (bundle.containsKey(key)) {
                val value = bundle.getString(key)
                translations[kordLocale] = value
                if (javaLocale == java.util.Locale.ROOT) {
                    name = value
                }
            }
        } catch (_: Exception) {}
    }
    return name to translations
}

abstract class Command {
    abstract val name: String
    open val description: LocalizableString = LocalizableString.EMPTY
    abstract val bundleName: String

    val InteractionCreateEvent.bundle: ResourceBundle
        get() {
            val bName = bundleName
            val discordLocale = this.interaction.locale ?: Locale("en-US")
            val javaLocale = SUPPORTED_LOCALES[discordLocale] ?: java.util.Locale.ROOT
            return ResourceBundle.getBundle("locale.$bName", javaLocale, UTF8Control)
        }

    private val _options = mutableListOf<OptionImpl<*>>()
    val options: List<Option<*>> get() = _options

    internal fun registerOption(option: OptionImpl<*>) {
        _options.add(option)
    }

    abstract suspend fun register(kord: Kord)
    abstract suspend fun InteractionCreateEvent.execute()
}