package pw.modder.answernator.`fun`.commands

import dev.kord.core.entity.Message
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import pw.modder.answernator.db.guild.Config
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.extensions.kord.reply
import pw.modder.answernator.utils.extensions.kord.replyEmbed
import java.util.*
import pw.modder.answernator.`fun`.utils.Quote as QuoteData

private val json = Json { ignoreUnknownKeys = true }

class Quote: Command {
    override val name = "quote"
    override val cmdType = Command.CommandGroup.FUN
    override val localesWhitelist: List<Locale> = listOf(Locale("ru"))

    override fun getHelp(locale: Locale): String? {
        return "Возвращает цитату с https://modder.pw. Использование: `quote [номер цитаты]`"
    }

    override fun getDescription(locale: Locale): String? {
        return "цитата из цитатника modder.pw"
    }

    override suspend fun action(message: Message, args: List<String>, locale: Locale, config: Config?) {
        val request: String = when(val id = args.firstOrNull()?.toIntOrNull()) {
            null -> Globals.httpClient.get("https://modder.pw/api/random.php")
            else -> Globals.httpClient.get("https://modder.pw/api/get.php") { parameter("id", id) }
        }

        val data = try {
            json.decodeFromString(QuoteData.serializer(), request)
        } catch (e: Exception) {
            if (!json.decodeFromString(QuoteData.Error.serializer(), request).success) {
                message.reply("Неверный номер цитаты")
                return
            }
            throw e
        }

        message.replyEmbed {
            title = "Цитата #${data.id}"
            url = "https://modder.pw/?id=${data.id}"
            description = data.text.takeIf { it.length < 2000 } ?: data.text.take(1999) + "…"
            timestamp = Instant.fromEpochSeconds(data.createdAt)
            field("Автор", true) { data.creatorMention }
            field("Лайков", true) { data.likesCount.toString() }
            footer { text = "Источник: Цитатник McModder'а | modder.pw" }
        }
    }
}