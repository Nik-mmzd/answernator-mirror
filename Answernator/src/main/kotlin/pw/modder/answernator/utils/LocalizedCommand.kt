package pw.modder.answernator.utils

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import pw.modder.answernator.utils.extensions.getStringSafe
import java.util.*
import pw.modder.answernator.utils.extensions.getStringOrKey as getStringOrKey1

//private val logger: KLogger = KotlinLogging.logger {}
interface LocalizedCommand: Command {
    fun getTexts(locale: Locale): ResourceBundle {
        return ResourceBundle.getBundle("locale.$name", locale, javaClass.classLoader, UTF8Control())
    }

    fun ResourceBundle.getStringOrKey(key: String): String = getStringOrKey1("$name.$key")
    fun ResourceBundle.formatString(key: String, vararg args: Any): String = getStringOrKey(key).format(*args)
    fun ResourceBundle.message(key: String): CombinedMessageEmbed = textMessage(getStringOrKey(key))
    fun ResourceBundle.message(key: String, vararg args: Any): CombinedMessageEmbed = textMessage(formatString(key, *args))
    fun ResourceBundle.errorMessage(key: String = "error"): CombinedMessageEmbed {
        val help = getHelp(locale) ?: return textMessage(getStringOrKey(key))
        return textMessage("${getStringOrKey(key)}\n$help")
    }

    override fun getHelp(locale: Locale): String? {
        return getTexts(locale).getStringSafe("$name.help.usage")
    }

    override fun getDescription(locale: Locale): String? {
        return getTexts(locale).getStringSafe("$name.help.description")
    }

    override suspend fun action(bot: Bot, message: Message, locale: Locale): CombinedMessageEmbed {
        return action(bot, message, getTexts(locale))
    }

    suspend fun action(bot: Bot, message: Message, texts: ResourceBundle): CombinedMessageEmbed
}
