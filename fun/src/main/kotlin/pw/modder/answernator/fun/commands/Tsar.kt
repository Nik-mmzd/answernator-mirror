package pw.modder.answernator.`fun`.commands

import dev.kord.common.Color
import dev.kord.core.entity.Message
import kotlinx.datetime.Clock
import pw.modder.answernator.db.guild.Config
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.LocalizedCommand
import pw.modder.answernator.utils.extensions.kord.reply
import pw.modder.answernator.utils.extensions.kord.replyEmbed
import pw.modder.answernator.utils.locale.CommandLocaleBundle
import java.util.*

class Tsar: LocalizedCommand {
    override val name = "царь"
    override val cmdType = Command.CommandGroup.FUN
    override val localesWhitelist: List<Locale> = listOf(Locale("ru"))

    override suspend fun action(message: Message, args: List<String>, texts: CommandLocaleBundle, config: Config?) {
        if (args.firstOrNull()?.equals("велит", true) != true) {
            message.reply(texts["invalid"])
            return
        }

        message.replyEmbed {
            title = texts["title"]
            field(texts["title.decree"], false) {
                texts.random("decree")
            }
            color = Color(16711680)
            thumbnail { url = texts["thumbnail"] }

            footer {
                text = texts.random("sign")
                icon = texts["footer.icon"]
            }

            timestamp = Clock.System.now()
        }
    }
}