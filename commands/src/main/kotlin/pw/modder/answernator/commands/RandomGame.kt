package pw.modder.answernator.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.util.ClientStore
import com.jessecorbett.diskord.util.mention
import com.jessecorbett.diskord.util.words
import kotlinx.serialization.UnstableDefault
import pw.modder.answernator.commands.utils.RandomGames
import pw.modder.answernator.utils.LocalizedCommand
import java.util.*

private val gamesdb = RandomGames()
@UnstableDefault
class RandomGame: LocalizedCommand {
    override val name = "randomgame"

    override suspend fun action(clientStore: ClientStore, message: Message, locale: Locale): CombinedMessageEmbed {
        val num = message.words.getOrNull(1)?.toIntOrNull() ?: 1
        if (num > 64) return textMessage(getString(locale, "error"))

        val gameNames = mutableListOf<String>()
        repeat(num) {
            gameNames.add(gamesdb.getRandomGame())
        }
        return textMessage(gameNames.joinToString(", ", prefix = "${message.author.mention}, ") { "`$it`" })
    }
}