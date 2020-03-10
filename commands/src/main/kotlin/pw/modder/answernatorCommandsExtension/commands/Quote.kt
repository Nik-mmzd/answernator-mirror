package pw.modder.answernatorCommandsExtension.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.model.Permissions
import com.jessecorbett.diskord.api.rest.client.GuildClient
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.dsl.footer
import com.jessecorbett.diskord.util.words
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.UnstableDefault
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.extensions.setTimestamp
import pw.modder.answernatorCommandsExtension.utils.Quote as QuoteData
import com.jessecorbett.diskord.dsl.message as dslmessage
import java.util.*

private val client = HttpClient()
@UnstableDefault
class Quote: Command {
    override val name = "quote"
    override fun getHelp(locale: Locale): String? {
        return "Возвращает цитату с https://modder.pw. Использование: `цитату [номер цитаты]`"
    }

    override suspend fun check(message: Message, guildClient: GuildClient?): Boolean {
        val locale = message.guildId?.run { Globals.getGuildConfig(this).locale } ?: Globals.config.locale
        return locale == Locale("ru") && super.check(message, guildClient)
    }

    override fun check(message: Message, permissions: Permissions): Boolean {
        val locale = message.guildId?.run { Globals.getGuildConfig(this).locale } ?: Globals.config.locale
        return locale == Locale("ru") && super.check(message, permissions)
    }

    override suspend fun action(bot: Bot, message: Message, locale: Locale): CombinedMessageEmbed {
        val id = message.words.getOrNull(1)

        val data = if (id == null) client.get<QuoteData>("https://modder.pw/api/random.php")
            else client.get("https://modder.pw/api/get.php") { parameter("id", id) }

        return dslmessage {
            title = "Цитата #${data.id}"
            url = "https://modder.pw/?id=${data.id}"
            description = data.text.takeIf { it.length < 2000 } ?: data.text.take(1999) + "…"
            setTimestamp(data.created_at)
            footer("${data.likesCount} лайков")
        }
    }
}