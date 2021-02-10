package pw.modder.answernator.utils.locale

import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.UTF8Control
import java.util.*
import java.util.ResourceBundle as JavaResourceBundle

interface ResourceBundle {
    val name: String
    val locale: Locale

    fun getString(key: String): String
    fun formatString(key: String, vararg args: Any): String
    fun getNullableString(key: String): String?
    fun formatNullableString(key: String, vararg args: Any): String?
    fun getRandomString(key: String): String?
}

class LocaleBundle private constructor(override val name: String, override val locale: Locale, private val bundle: JavaResourceBundle): ResourceBundle {
    constructor(name: String, locale: Locale, classLoader: ClassLoader):
            this(name, locale, JavaResourceBundle.getBundle("locale.$name", locale, classLoader, UTF8Control))

    constructor(name: String, locale: Locale):
            this(name, locale, JavaResourceBundle.getBundle("locale.$name", locale, UTF8Control))

    override fun getString(key: String): String {
        if (bundle.containsKey(key)) return bundle.getString(key)
        return key
    }

    override fun formatString(key: String, vararg args: Any): String {
        return getString(key).format(args = *args)
    }

    override fun getNullableString(key: String): String? {
        if (bundle.containsKey(key)) return bundle.getString(key)
        return null
    }

    override fun getRandomString(key: String): String {
        val keys = bundle.keys.asSequence().filter { it.startsWith("$key.") }.toList()
        if (keys.isEmpty()) return "$key.random"
        return bundle.getString(keys.random(Globals.random))
    }

    override fun formatNullableString(key: String, vararg args: Any): String? {
        return getNullableString(key)?.format(*args)
    }

    fun String.toMessage(): CombinedMessageEmbed {
        return CombinedMessageEmbed(text = this)
    }
}

class CommandLocaleBundle private constructor(override val name: String, override val locale: Locale,
                                              private val bundle: JavaResourceBundle
) : ResourceBundle {
    constructor(name: String, locale: Locale, classLoader: ClassLoader):
            this(name, locale, JavaResourceBundle.getBundle("locale.$name", locale, classLoader, UTF8Control))

    constructor(name: String, locale: Locale):
            this(name, locale, JavaResourceBundle.getBundle("locale.$name", locale, UTF8Control))

    override fun getString(key: String): String {
        if (bundle.containsKey("$name.$key")) return bundle.getString("$name.$key")
        return "$name.$key"
    }

    override fun formatString(key: String, vararg args: Any): String {
        return getString(key).format(args = *args)
    }

    override fun getNullableString(key: String): String? {
        if (bundle.containsKey("$name.$key")) return bundle.getString(key)
        return null
    }

    override fun formatNullableString(key: String, vararg args: Any): String? {
        return getNullableString(key)?.format(*args)
    }

    override fun getRandomString(key: String): String {
        val keys = bundle.keys.asSequence().filter { it.startsWith("$name.$key.") }.toList()
        if (keys.isEmpty()) return "$name.$key.random"
        return bundle.getString(keys.random(Globals.random))
    }

    fun getHelp(): String? {
        return getNullableString("help.usage")
    }

    fun getDescription(): String? {
        return getNullableString("help.description")
    }

    fun getErrorString(key: String = "error"): String {
        if (bundle.containsKey("$name.help.usage"))
            return "${bundle.getString("$name.key")}\n${bundle.getString("$name.help.usage")}"

        return getString(key)
    }
}