package pw.modder.answernator.`fun`.commands

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.component.actionRow
import dev.kord.rest.builder.message.embed
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.kodein.di.DI
import pw.modder.answernator.`fun`.Env
import pw.modder.answernator4.interaction.ChatInputCommand
import pw.modder.answernator4.interaction.Option
import pw.modder.answernator4.interaction.description
import pw.modder.answernator4.interaction.long
import pw.modder.answernator4.interaction.minValue
import pw.modder.answernator4.interaction.name
import pw.modder.answernator4.interaction.optional
import kotlin.time.Instant

/**
 * `/quote [номер]` — fetches a quote from the modder.pw quote book (https://modder.pw/api/v2).
 * Without an id a random quote is returned.
 *
 * ru-specific and intentionally scoped to a single guild via [guildIds]; the placeholder ID `0`
 * must be replaced with the real guild before deployment.
 */
class Quote(di: DI) : ChatInputCommand(di) {
    override val name = "quote"
    override val bundleName = "fun.quote"
    override val guildIds = Env.QuotesEnabledGuilds

    val id: Option<Long?> by long().name("id").description("id.description").minValue(1).optional()

    override suspend fun ChatInputCommandInteractionCreateEvent.execute() {
        val reply = interaction.deferPublicResponse()
        val id by option(id)

        val requestUrl = if (id == null) "$API_BASE/quotes/random" else "$API_BASE/quotes/$id"
        val response = httpClient.get(requestUrl)

        if (!response.status.isSuccess()) {
            reply.respond { content = "Неверный номер цитаты" }
            return
        }

        val quote = json.decodeFromString(QuoteData.serializer(), response.bodyAsText())

        reply.respond {
            embed {
                title = "Цитата #${quote.id}"
                url = "https://modder.pw/quotes/${quote.id}"
                description = quote.text.takeIf { it.length < 2000 } ?: (quote.text.take(1999) + "…")
                timestamp = Instant.fromEpochMilliseconds(quote.created)
                author {
                    name = quote.author.name.ifBlank { "Автор неизвестен" }
                    url = "https://modder.pw/authors/${quote.author.id}"
                }
                footer { text = "Цитатник McModder'а | modder.pw" }
            }
            actionRow {
                interactionButton(ButtonStyle.Secondary, "quote:likes") {
                    emoji = DiscordPartialEmoji(name = "❤️")
                    label = quote.likes.toString()
                    disabled = true
                }
            }
        }
    }

    @Serializable
    private data class QuoteData(
        val id: Int,
        val author: Author,
        val created: Long,
        val text: String,
        val likes: Long,
    ) {
        @Serializable
        data class Author(val id: Long, val name: String)
    }

    private companion object {
        const val API_BASE = "https://modder.pw/api/v2"

        val httpClient = HttpClient(CIO)
        val json = Json { ignoreUnknownKeys = true }
    }
}
