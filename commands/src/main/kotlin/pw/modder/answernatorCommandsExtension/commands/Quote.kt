package pw.modder.answernatorCommandsExtension.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.model.Permissions
import com.jessecorbett.diskord.api.rest.client.GuildClient
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import kotlinx.serialization.UnstableDefault
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.Globals
import java.util.*

@UnstableDefault
class Quote: Command {
    override val name = "quote"
    override fun getHelp(locale: Locale): String? {
        return "Возвращает цитату с https://modder.pw. Использование: `цитату [номер цитаты]`"
    }

    override suspend fun check(message: Message, guildClient: GuildClient?): Boolean {
        val locale = message.guildId?.run { Globals.getGuildConfig(this).locale } ?: Globals.config.locale
        return locale == Locale("ru") || super.check(message, guildClient)
    }

    override fun check(message: Message, permissions: Permissions): Boolean {
        val locale = message.guildId?.run { Globals.getGuildConfig(this).locale } ?: Globals.config.locale
        return locale == Locale("ru") || super.check(message, permissions)
    }

    override suspend fun action(bot: Bot, message: Message, locale: Locale): CombinedMessageEmbed {
        TODO("Not implemented")
    }
}