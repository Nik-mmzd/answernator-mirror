package pw.modder.answernator.`fun`.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.model.Permissions
import com.jessecorbett.diskord.api.rest.client.GuildClient
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.util.words
import pw.modder.answernator.db.Db
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.Globals
import java.util.*

class DChar: Command {
    override val name = "буквахуй"
    override fun getHelp(locale: Locale) = "х̆уй!"
    override fun getDescription(locale: Locale) = "посвящена одноимённому мему"
    override val cmdType = Command.CommandGroup.FUN

    override fun check(message: Message, permissions: Permissions): Boolean {
        val locale = message.guildId?.run { Locale(Db.guilds.get(this).lang) } ?: Globals.config.locale
        return locale == Locale("ru") && super.check(message, permissions)
    }

    override suspend fun check(message: Message, guildClient: GuildClient?): Boolean {
        val locale = message.guildId?.run { Locale(Db.guilds.get(this).lang) } ?: Globals.config.locale
        return locale == Locale("ru") && super.check(message, guildClient)
    }

    override suspend fun action(bot: Bot, message: Message, locale: Locale): CombinedMessageEmbed {
        return textMessage(message.words.drop(1).joinToString(" ")
            .replace("х", "х̆").replace("x", "х̆")
            .replace("X", "X̆").replace("Х", "X̆")
            .ifEmpty { "х̆уй!" })
    }
}