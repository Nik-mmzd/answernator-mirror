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
        if (!args.first().equals("велит", true)) {
            message.reply(texts.getString("invalid"))
            return
        }

        message.replyEmbed {
            title = texts.getString("title")
            field(texts.getString("title.decree"), false) {
                texts.getRandomString("decree")
            }
            color = Color(16711680)
            thumbnail { url = texts.getString("thumbnail") }

            footer {
                text = texts.getRandomString("sign")
                icon = texts.getString("footer.icon")
            }

            timestamp = Clock.System.now()
        }
    }
}