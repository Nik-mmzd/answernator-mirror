package pw.modder.answernator.utils.locale

import pw.modder.answernator.utils.Globals
import java.util.*
import java.util.ResourceBundle as JavaResourceBundle

interface ResourceBundle {
    val name: String
    val locale: Locale

    operator fun get(key: String): String
    fun getOrNull(key: String): String?
    fun random(key: String): String?

    @Deprecated("Use getter instead", replaceWith = ReplaceWith("get(key)"))
    fun getString(key: String): String
    @Deprecated("Use get().format()", replaceWith = ReplaceWith("get(key).format(args)"))
    fun formatString(key: String, vararg args: Any): String
    @Deprecated("Use getOrNull()", replaceWith = ReplaceWith("getOrNull(key)"))
    fun getNullableString(key: String): String?
    @Deprecated("Use getOrNull().format()", replaceWith = ReplaceWith("getOrNull(key)?.format(args)"))
    fun formatNullableString(key: String, vararg args: Any): String?
    @Deprecated("Use random()", replaceWith = ReplaceWith("random(key)"))
    fun getRandomString(key: String): String?
}

class LocaleBundle private constructor(override val name: String, override val locale: Locale, private val bundle: JavaResourceBundle): ResourceBundle {
    constructor(name: String, locale: Locale, classLoader: ClassLoader):
            this(name, locale, JavaResourceBundle.getBundle("locale.$name", locale, classLoader, UTF8Control))

    constructor(name: String, locale: Locale):
            this(name, locale, JavaResourceBundle.getBundle("locale.$name", locale, UTF8Control))

    constructor(name: String, locale: String):
            this(name, Locale(locale), JavaResourceBundle.getBundle("locale.$name", Locale(locale), UTF8Control))

    override operator fun get(key: String): String {
        if (bundle.containsKey(key)) return bundle.getString(key)
        return key
    }

    override fun getOrNull(key: String): String? {
        if (bundle.containsKey(key)) return bundle.getString(key)
        return null
    }

    override fun getString(key: String) = get(key)
    override fun formatString(key: String, vararg args: Any): String {
        return get(key).format(args = args)
    }

    override fun getNullableString(key: String) = getOrNull(key)
    override fun formatNullableString(key: String, vararg args: Any): String? {
        return getOrNull(key)?.format(*args)
    }

    override fun random(key: String): String {
        val keys = bundle.keys.asSequence().filter { it.startsWith("$key.") }.toList()
        if (keys.isEmpty()) return "$key.random"
        return bundle.getString(keys.random(Globals.random))
    }

    override fun getRandomString(key: String) = random(key)
}

class CommandLocaleBundle private constructor(override val name: String, override val locale: Locale,
                                              private val bundle: JavaResourceBundle
) : ResourceBundle {
    constructor(name: String, locale: Locale, classLoader: ClassLoader):
            this(name, locale, JavaResourceBundle.getBundle("locale.$name", locale, classLoader, UTF8Control))

    constructor(name: String, locale: Locale):
            this(name, locale, JavaResourceBundle.getBundle("locale.$name", locale, UTF8Control))

    override operator fun get(key: String): String {
        if (bundle.containsKey("$name.$key")) return bundle.getString("$name.$key")
        return "$name.$key"
    }

    override fun getOrNull(key: String): String? {
        if (bundle.containsKey("$name.$key")) return bundle.getString("$name.$key")
        return null
    }

    override fun getString(key: String) = get(key)
    override fun formatString(key: String, vararg args: Any): String {
        return get(key).format(args = args)
    }

    override fun getNullableString(key: String) = getOrNull(key)
    override fun formatNullableString(key: String, vararg args: Any): String? {
        return getOrNull(key)?.format(*args)
    }

    override fun random(key: String): String {
        val keys = bundle.keys.asSequence().filter { it.startsWith("$name.$key.") }.toList()
        if (keys.isEmpty()) return "$name.$key.random"
        return bundle.getString(keys.random(Globals.random))
    }

    override fun getRandomString(key: String) = random(key)

    val help: String? get() = getOrNull("help.usage")
    val description: String? get() = getOrNull("help.description")

    fun error(key: String = "error"): String {
        if (bundle.containsKey("$name.help.usage"))
            return "${get(key)}\n${bundle.getString("$name.help.usage")}"

        return get(key)
    }

    @Deprecated("Use error() instead", ReplaceWith("error(key)"))
    fun getErrorString(key: String = "error") = error(key)
}