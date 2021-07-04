package pw.modder.answernator.utils.extensions.bot

import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.DiskordDsl
import com.jessecorbett.diskord.util.authorId
import com.jessecorbett.diskord.util.isFromUser
import com.jessecorbett.diskord.util.mention
import com.jessecorbett.diskord.util.sendMessage
import mu.KotlinLogging
import pw.modder.answernator.cache.AntiSpamCache
import pw.modder.answernator.db.Db
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.locale.LocaleBundle
import java.util.*

// temporary
private const val WARN_INCREMENT = 3
private const val MAX_INCREMENT = 5

private val logger = KotlinLogging.logger {}

@DiskordDsl
fun Bot.antiSpam() {
    messageCreated { message ->
        if (!message.isFromUser) return@messageCreated
        val guild = message.guildId
            ?: return@messageCreated

        if (message.content.startsWith(Globals.config.prefix) && message.content.length <= 48) return@messageCreated

        val config = Db.guilds.get(guild)
        logger.debug { "Guild $guild, antispam enabled: ${config.antiSpam}" }
        if (!config.antiSpam) return@messageCreated

        val increment = AntiSpamCache.increment(message)
        logger.debug { "Guild $guild, message ${message.id}, increment = $increment" }

        when(increment) {
            WARN_INCREMENT -> {
                logger.info { "Warning user: guild $guild, message ${message.id}, user ${message.authorId}, content = <${message.content}>" }
                val texts = LocaleBundle("botGlobal", Locale(config.lang))
                clientStore.channels[message.channelId].sendMessage(texts.formatString("bot.antispam.warning", message.author.mention))
            }
            MAX_INCREMENT -> {
                logger.info { "Creating ban: guild $guild, message ${message.id}, user ${message.authorId}, content = <${message.content}>" }
                val texts = LocaleBundle("botGlobal", Locale(config.lang))
                clientStore.guilds[guild].createBan(message.authorId, 1, texts.getString("bot.antispam.reason"))
            }
        }
    }
}