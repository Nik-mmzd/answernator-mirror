package pw.modder.answernator4.command

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.core.behavior.interaction.response.edit
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import pw.modder.answernator4.interaction.ChatInputCommand
import pw.modder.answernator4.interaction.Option
import pw.modder.answernator4.interaction.button.ButtonField
import pw.modder.answernator4.interaction.button.ButtonGroup
import pw.modder.answernator4.interaction.button.button
import pw.modder.answernator4.interaction.button.renderButtons
import pw.modder.answernator4.interaction.button.respondWithCommandButtons
import pw.modder.answernator4.interaction.coerceValue
import pw.modder.answernator4.interaction.default
import pw.modder.answernator4.interaction.description
import pw.modder.answernator4.interaction.long
import pw.modder.answernator4.interaction.name
import kotlin.random.Random

private const val MIN_GAMES = 1L
private const val MAX_GAMES = 64L

class RandomGame : ChatInputCommand() {
    override val name = "randomgame"
    override val bundleName = "v4.randomgame"
    override val buttons = Buttons()

    private val games = RandomGames()

    val count: Option<Long> by long()
        .name("randomgame.count")
        .description("randomgame.count.description")
        .coerceValue(MIN_GAMES, MAX_GAMES)
        .default(1L)

    override suspend fun ChatInputCommandInteractionCreateEvent.execute() {
        val count by option(count)

        interaction.respondWithCommandButtons(
            this@RandomGame,
            ephemeral = false,
            state = count.toString(),
            content = renderGames(count),
        )
    }

    override suspend fun ButtonInteractionCreateEvent.onButtonClick(button: ButtonField, state: String?) {
        if (button != buttons.regen) return
        val count = (state?.toLongOrNull() ?: 1L).coerceIn(MIN_GAMES, MAX_GAMES)
        interaction.deferPublicMessageUpdate().edit {
            renderButtons(
                this@RandomGame.buttons,
                baseId = "cmd:${this@RandomGame.effectiveName}",
                state = state,
                contentOverride = renderGames(count),
            )
        }
    }

    private fun renderGames(count: Long): String =
        (1..count).joinToString("\n") { games.getRandomGame() }

    class Buttons : ButtonGroup() {
        val regen by button(
            style = ButtonStyle.Secondary,
            emoji = DiscordPartialEmoji(name = "🎮"),
        )
    }
}

/**
 * Random video game name generator. Reads a copy of the original `tools` module's `gameslist.txt`:
 * three `----`-separated word lists, joined into 2–3 word combinations with `^`-delimited exclusion
 * groups so incompatible words never co-occur.
 */
private class RandomGames {
    private val words: List<List<String>>
    private val random = Random(System.currentTimeMillis())

    init {
        val lines = javaClass.classLoader.getResourceAsStream("pw/modder/answernator4/gameslist.txt")!!.reader().use { it.readLines() }
        val firstSep = lines.indexOf("----")
        val secondSep = lines.lastIndexOf("----")

        words = listOf(
            lines.subList(0, firstSep - 1),
            lines.subList(firstSep + 1, secondSep - 1),
            lines.subList(secondSep + 1, lines.lastIndex)
        )
    }

    fun getRandomGame(): String {
        val goodWords = mutableListOf<String>()
        val badWords = mutableListOf<String>()
        val wordsCount = random.nextInt(2, 4)
        var i = if (wordsCount == 3) 0 else random.nextInt(0, 2) // first
        do {
            val word = words[i].random(random).split('^', limit = 2)
            if (word[0] in badWords) continue

            word.getOrNull(1)?.run {
                badWords.addAll(split('|'))
            }
            goodWords.add(word[0])
            i++
        } while (goodWords.size < wordsCount)

        return goodWords.joinToString(" ")
    }
}
