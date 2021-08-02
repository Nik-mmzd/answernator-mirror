package pw.modder.answernator.utils

import dev.kord.core.entity.Message
import pw.modder.answernator.db.guild.Config
import pw.modder.answernator.utils.locale.CommandLocaleBundle
import java.util.Locale

//private val logger: KLogger = KotlinLogging.logger {}
interface LocalizedCommand: Command {
    fun getTexts(locale: Locale): CommandLocaleBundle {
        return CommandLocaleBundle(name, locale, javaClass.classLoader)
    }

    override fun getHelp(locale: Locale): String? {
        return getTexts(locale).help
    }

    override fun getDescription(locale: Locale): String? {
        return getTexts(locale).description
    }

    override suspend fun action(message: Message, args: List<String>, locale: Locale, config: Config?) {
        action(message, args, getTexts(locale), config)
    }

    suspend fun action(message: Message, args: List<String>, texts: CommandLocaleBundle, config: Config?)
}
