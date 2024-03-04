package pw.modder.answernator.utils.extensions.bot

import dev.kord.core.Kord
import dev.kord.core.behavior.ban
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import io.github.oshai.kotlinlogging.KotlinLogging
import pw.modder.answernator.cache.AntiSpamCache
import pw.modder.answernator.db.Db
import pw.modder.answernator.db.guild.Features
import pw.modder.answernator.utils.extensions.kord.authorId
import pw.modder.answernator.utils.extensions.kord.guildId
import pw.modder.answernator.utils.extensions.kord.reply
import kotlin.time.Duration.Companion.days

private val logger = KotlinLogging.logger {}
suspend fun Kord.antiSpam() {
    on<MessageCreateEvent> {
        if (message.author?.isBot != false) return@on
        val guild = message.guildId
            ?: return@on

        if (message.content.isEmpty() && message.attachments.isEmpty()) {
            logger.warn { "Guild $guild, message ${message.id}, user ${message.authorId}, message is EMPTY, check bot permissions" }
            return@on
        } // hotfix
        val config = Db.getAntiSpamConfig(guild) ?: return@on
        if (message.content.startsWith(config.cmdPrefix) && message.content.length <= 48) return@on

        logger.debug { "Guild $guild, antispam enabled: ${config.isEnabled(Features.ANTI_SPAM)}" }
        if (!config.isEnabled(Features.ANTI_SPAM)) return@on

        val increment = AntiSpamCache.increment(message)
        logger.debug { "Guild $guild, message ${message.id}, increment = $increment" }

        when(increment) {
            config.antiSpamWarn -> {
                logger.info { "Warning user: guild $guild, message ${message.id}, user ${message.authorId}, content = <${message.content}>" }
                if (config.isEnabled(Features.ANTI_SPAM_SILENT))
                    return@on

                AntiSpamCache.putMessage(
                    guild,
                    message.data.author.id,
                    message.reply(config.antiSpamWarnText.format(message.author?.mention ?: "\$user"))
                )
            }
            config.antiSpamBan -> {
                logger.info { "Creating ban: guild $guild, message ${message.id}, user ${message.authorId}, content = <${message.content}>" }

                message.getGuild().ban(message.author!!.id) {
                    deleteMessageDuration = 1.days
                    reason = config.antiSpamBanText
                }
                AntiSpamCache.getMessage(guild, message.data.author.id)?.delete()
            }
        }
    }
}