package pw.modder.answernator.utils

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import pw.modder.answernator.utils.locale.CommandLocaleBundle
import java.util.Locale

//private val logger: KLogger = KotlinLogging.logger {}
interface LocalizedCommand: Command {
    fun getTexts(locale: Locale): CommandLocaleBundle {
        return CommandLocaleBundle(name, locale, javaClass.classLoader)
    }

    override fun getHelp(locale: Locale): String? {
        return getTexts(locale).getHelp()
    }

    override fun getDescription(locale: Locale): String? {
        return getTexts(locale).getDescription()
    }

    override suspend fun action(bot: Bot, message: Message, locale: Locale): CombinedMessageEmbed {
        return action(bot, message, getTexts(locale))
    }

    suspend fun action(bot: Bot, message: Message, texts: CommandLocaleBundle): CombinedMessageEmbed
}
