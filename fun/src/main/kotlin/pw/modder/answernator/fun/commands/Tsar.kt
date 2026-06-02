package pw.modder.answernator.`fun`.commands

import dev.kord.common.Color
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.message.embed
import org.kodein.di.DI
import pw.modder.answernator.`fun`.Env
import pw.modder.answernator4.interaction.ChatInputCommand
import kotlin.time.Clock

/**
 * `/царь` — issues a random royal decree as an embed.
 *
 * ru-specific and intentionally scoped to a single guild via [guildIds]; the placeholder ID `0`
 * must be replaced with the real guild before deployment.
 */
class Tsar(di: DI) : ChatInputCommand(di) {
    override val name = "царь"
    override val bundleName = "fun.tsar"
    override val guildIds = Env.TsarEnabledGuilds

    override suspend fun ChatInputCommandInteractionCreateEvent.execute() {
        interaction.respondPublic {
            embed {
                title = TITLE
                field(DECREE_FIELD, false) { DECREES.random() }
                color = Color(0xFF0000)
                thumbnail { url = THUMBNAIL }
                footer {
                    text = SIGNS.random()
                    icon = FOOTER_ICON
                }
                timestamp = Clock.System.now()
            }
        }
    }

    private companion object {
        const val TITLE = "Повеление Царя:"
        const val DECREE_FIELD = "Царь Велитъ"
        const val THUMBNAIL = "https://files.modder.pw/answernator/tsar.jpg"
        const val FOOTER_ICON = "https://files.modder.pw/answernator/crown.png"

        val DECREES = listOf(
            "Ебать Васъ в сраку",
            "Бросить на съеденье ракамъ",
            "Ебать Васъ ракомъ",
            "Ебать Васъ в сраку,\nБросить на съеденье ракамъ",
            "Ебать Васъ в сраку,\nБросить на съеденье ракамъ\nИ царицу, и приплодъ",
            "Ебать Васъ в сраку,\nБросить на съеденье ракамъ\nИ царицу, и приплодъ\nЗдесь печать и подпись. ВотЪ.",
        )

        val SIGNS = listOf(
            "Царь Графиний Де Бойан.",
            "Царь Демидович Семён.",
            "Императоръ ВодкинЪ.",
            "Ихне Величество В.В.П.",
        )
    }
}
