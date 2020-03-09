package pw.modder.answernatorCommandsExtension.commands.localized

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.util.ClientStore
import com.jessecorbett.diskord.util.mention
import com.jessecorbett.diskord.util.words
import kotlinx.serialization.UnstableDefault
import pw.modder.answernatorCommandsExtension.utils.RandomGames
import pw.modder.answernator.utils.LocalizedCommand
import java.util.*

internal val gamesdb = RandomGames()
@UnstableDefault
class RandomGame: LocalizedCommand {
    override val name = "randomgame"

    override suspend fun action(bot: Bot, message: Message, texts: ResourceBundle): CombinedMessageEmbed {
        val num = message.words.getOrNull(1)?.toIntOrNull() ?: 1
        if (num > 64 || num < 1) return textMessage(texts.getStringOrKey("error"))

        val gameNames = mutableListOf<String>()
        repeat(num) {
            gameNames.add(gamesdb.getRandomGame())
        }
        return textMessage(gameNames.joinToString(", ", prefix = "${message.author.mention}, ") { "`$it`" })
    }
}