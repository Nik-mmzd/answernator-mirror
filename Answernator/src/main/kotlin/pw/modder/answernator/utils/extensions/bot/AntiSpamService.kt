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
import pw.modder.answernator.db.guild.Features
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.locale.LocaleBundle
import java.util.*

private val logger = KotlinLogging.logger {}

@DiskordDsl
fun Bot.antiSpam() {
    messageCreated { message ->
        if (!message.isFromUser) return@messageCreated
        val guild = message.guildId
            ?: return@messageCreated

        if (message.content.startsWith(Globals.config.prefix) && message.content.length <= 48) return@messageCreated

        val config = Db.getAntiSpamConfig(guild)

        logger.debug { "Guild $guild, antispam enabled: ${config.isEnabled(Features.ANTI_SPAM)}" }
        if (!config.isEnabled(Features.ANTI_SPAM)) return@messageCreated

        val increment = AntiSpamCache.increment(message)
        logger.debug { "Guild $guild, message ${message.id}, increment = $increment" }

        when(increment) {
            config.antiSpamWarn -> {
                logger.info { "Warning user: guild $guild, message ${message.id}, user ${message.authorId}, content = <${message.content}>" }
                if (config.isEnabled(Features.ANTI_SPAM_SILENT))
                    return@messageCreated

                clientStore.channels[message.channelId].sendMessage(config.antiSpamWarnText.replace("%user%", message.author.mention))
            }
            config.antiSpamBan -> {
                logger.info { "Creating ban: guild $guild, message ${message.id}, user ${message.authorId}, content = <${message.content}>" }
                clientStore.guilds[guild].createBan(message.authorId, 1, config.antiSpamBanText)
            }
        }
    }
}