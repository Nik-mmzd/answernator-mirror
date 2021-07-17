package pw.modder.answernator.tools.commands.localized

import dev.kord.core.entity.Message
import pw.modder.answernator.tools.utils.RandomGames
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.LocalizedCommand
import pw.modder.answernator.utils.extensions.kord.reply
import pw.modder.answernator.utils.locale.CommandLocaleBundle

internal val gamesdb = RandomGames()
class RandomGame: LocalizedCommand {
    override val name = "randomgame"
    override val cmdType = Command.CommandGroup.FUN

    override suspend fun action(message: Message, args: List<String>, texts: CommandLocaleBundle) {
        val num = args.firstOrNull()?.toIntOrNull() ?: 1
        if (num > 64 || num < 1) {
            message.reply(texts.getErrorString())
            return
        }

        val gameNames = mutableListOf<String>()
        repeat(num) {
            gameNames.add(gamesdb.getRandomGame())
        }
        message.reply(gameNames.joinToString(", ", prefix = "${message.author!!.mention}, ") { "`$it`" })
    }
}