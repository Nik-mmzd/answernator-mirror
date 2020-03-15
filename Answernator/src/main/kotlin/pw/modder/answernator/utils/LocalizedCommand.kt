package pw.modder.answernator.utils

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import kotlinx.serialization.UnstableDefault
import pw.modder.answernator.utils.extensions.getStringOrKey as getStringOrKey1
import java.util.*

//private val logger: KLogger = KotlinLogging.logger {}
@UnstableDefault
interface LocalizedCommand: Command {
    fun getTexts(locale: Locale): ResourceBundle {
        return ResourceBundle.getBundle("locale.$name", locale, javaClass.classLoader, UTF8Control())
    }

    fun ResourceBundle.getStringOrKey(key: String): String = getStringOrKey1("$name.$key")
    fun ResourceBundle.formatString(key: String, vararg args: Any): String = String.format(getStringOrKey(key), args = *args)
    fun ResourceBundle.message(key: String): CombinedMessageEmbed = textMessage(getStringOrKey(key))
    fun ResourceBundle.message(key: String, vararg args: Any): CombinedMessageEmbed = textMessage(formatString(key, *args))
    fun ResourceBundle.errorMessage(): CombinedMessageEmbed = message("error")

    override fun getHelp(locale: Locale): String? {
        return getTexts(locale).getStringOrKey("help")
    }

    override suspend fun action(bot: Bot, message: Message, locale: Locale): CombinedMessageEmbed {
        return action(bot, message, getTexts(locale))
    }

    suspend fun action(bot: Bot, message: Message, texts: ResourceBundle): CombinedMessageEmbed
}
