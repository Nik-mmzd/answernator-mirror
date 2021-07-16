package pw.modder.answernator.utils

import dev.kord.core.entity.Message
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

    override suspend fun action(message: Message, args: List<String>, locale: Locale) {
        action(message, args, getTexts(locale))
    }

    suspend fun action(message: Message, args: List<String>, texts: CommandLocaleBundle) {

    }
}
