package pw.modder.answernator.`fun`.commands

import dev.kord.core.entity.Message
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.extensions.kord.reply
import java.util.*

class DChar: Command {
    override val name = "буквахуй"
    override fun getHelp(locale: Locale) = "х̆уй!"
    override fun getDescription(locale: Locale) = "посвящена одноимённому мему"
    override val localesWhitelist: List<Locale> = listOf(Locale("ru"))
    override val cmdType = Command.CommandGroup.FUN

    override suspend fun action(message: Message, args: List<String>, locale: Locale) {
        message.reply(args.joinToString(" ")
            .replace("х", "х̆").replace("x", "х̆")
            .replace("X", "X̆").replace("Х", "X̆")
            .ifEmpty { "х̆уй!" })
    }
}