package pw.modder.answernator.utils.extensions.bot

import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.DiskordDsl
import com.jessecorbett.diskord.util.authorId
import com.jessecorbett.diskord.util.isFromUser
import pw.modder.answernator.cache.AntiSpamCache
import pw.modder.answernator.db.Db
import pw.modder.answernator.utils.locale.LocaleBundle
import java.util.*

// temporary
private const val MAX_INCREMENT = 5

@DiskordDsl
fun Bot.antiSpam() {
    messageCreated { message ->
        if (!message.isFromUser) return@messageCreated
        val guild = message.guildId
            ?: return@messageCreated

        val config = Db.guilds.get(guild)
        if (!config.antiSpam) return@messageCreated

        if (AntiSpamCache.increment(message) >= MAX_INCREMENT) {
            val texts = LocaleBundle("botGlobal", Locale(config.lang))
            clientStore.guilds[guild].createBan(message.authorId, 1, texts.getString("bot.antispam.reason"))
        }
    }
}