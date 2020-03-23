package pw.modder.answernator.`fun`.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.model.Permissions
import com.jessecorbett.diskord.api.rest.client.GuildClient
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.dsl.footer
import com.jessecorbett.diskord.util.words
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import pw.modder.answernator.db.Db
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.extensions.setTimestamp
import pw.modder.answernator.`fun`.utils.Quote as QuoteData
import com.jessecorbett.diskord.dsl.message as dslmessage
import java.util.*

@UnstableDefault
private val json = Json(JsonConfiguration(strictMode = false))

@UnstableDefault
class Quote: Command {
    override val name = "quote"
    override fun getHelp(locale: Locale): String? {
        return "Возвращает цитату с https://modder.pw. Использование: `quote [номер цитаты]`"
    }

    override suspend fun check(message: Message, guildClient: GuildClient?): Boolean {
        val locale = message.guildId?.run { Locale(Db.guilds.get(this).lang) } ?: Globals.config.locale
        return locale == Locale("ru") && super.check(message, guildClient)
    }

    override fun check(message: Message, permissions: Permissions): Boolean {
        val locale = message.guildId?.run { Locale(Db.guilds.get(this).lang) } ?: Globals.config.locale
        return locale == Locale("ru") && super.check(message, permissions)
    }

    override suspend fun action(bot: Bot, message: Message, locale: Locale): CombinedMessageEmbed {
        val id = message.words.getOrNull(1)

        val request = if (id == null) Globals.httpClient.get<String>("https://modder.pw/api/random.php")
            else Globals.httpClient.get<String>("https://modder.pw/api/get.php") { parameter("id", id) }

        val data = try {
            json.parse(QuoteData.serializer(), request)
        } catch (e: MissingFieldException) {
            if (!json.parse(QuoteData.Error.serializer(), request).success) return textMessage("Неверный номер цитаты")
            throw e
        }

        return dslmessage {
            title = "Цитата #${data.id}"
            url = "https://modder.pw/?id=${data.id}"
            description = data.text.takeIf { it.length < 2000 } ?: data.text.take(1999) + "…"
            setTimestamp(data.createdAt)
            field("Автор", data.creatorMention, true)
            field("Лайков", data.likesCount.toString(), true)
            footer("Источник: Цитатник McModder'а | modder.pw")
        }
    }
}