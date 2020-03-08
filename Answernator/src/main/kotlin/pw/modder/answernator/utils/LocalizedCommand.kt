package pw.modder.answernator.utils

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.util.ClientStore
import kotlinx.serialization.UnstableDefault
import pw.modder.answernator.utils.extensions.getStringOrKey as getStringOrKey1
import java.util.*

//private val logger: KLogger = KotlinLogging.logger {}
@UnstableDefault
interface LocalizedCommand: Command {
    fun getTexts(locale: Locale): ResourceBundle {
        return ResourceBundle.getBundle("locale.$name", locale, javaClass.classLoader, UTF8Control())
    }

    fun ResourceBundle.getStringOrKey(key: String): String {
        return getStringOrKey1("$name.$key")
    }

    fun ResourceBundle.formatString(key: String, vararg args: Any): String {
        return String.format(getStringOrKey(key), args = *args)
    }

    override fun getHelp(locale: Locale): String? {
        return getTexts(locale).getStringOrKey("help")
    }

    override suspend fun action(clientStore: ClientStore, message: Message, locale: Locale): CombinedMessageEmbed {
        return action(clientStore, message, getTexts(locale))
    }

    suspend fun action(clientStore: ClientStore, message: Message, texts: ResourceBundle): CombinedMessageEmbed
}
