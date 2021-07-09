package pw.modder.answernator.`fun`.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.model.Permissions
import com.jessecorbett.diskord.api.rest.EmbedImage
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.dsl.footer
import com.jessecorbett.diskord.util.GuildClients
import com.jessecorbett.diskord.util.words
import pw.modder.answernator.db.Db
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.LocalizedCommand
import pw.modder.answernator.utils.extensions.setCurrentTimestamp
import pw.modder.answernator.utils.locale.CommandLocaleBundle
import java.util.*
import kotlin.random.Random
import com.jessecorbett.diskord.dsl.message as dslmessage

class Tsar: LocalizedCommand {
    override val name = "царь"
    override val cmdType = Command.CommandGroup.FUN

    override suspend fun check(message: Message, guildClients: GuildClients): Boolean {
        val locale = message.guildId?.run { Db.getGuildConfig(this).lang } ?: Globals.config.lang
        return locale.equals("ru", true) && super.check(message, guildClients)
    }

    override fun check(message: Message, permissions: Permissions): Boolean {
        val locale = message.guildId?.run { Db.getGuildConfig(this).lang } ?: Globals.config.lang
        return locale.equals("ru", true) && super.check(message, permissions)
    }

    override suspend fun action(bot: Bot, message: Message, texts: CommandLocaleBundle): CombinedMessageEmbed {
        if (message.words.getOrNull(1)?.toLowerCase() != "велит")
            return texts.getString("invalid").toMessage()

        return dslmessage {
            title = texts.getString("title")

            field(texts.getString("title.decree"), texts.getRandomString("decree"), false)

            color = 16711680
            thumbnail = EmbedImage(texts.getString("thumbnail"))

            footer(texts.getRandomString("sign")) {
                iconUrl = texts.getString("footer.icon")
            }

            setCurrentTimestamp()
        }
    }
}