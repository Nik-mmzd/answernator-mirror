package pw.modder.answernator.utils.extensions.bot

import dev.kord.core.Kord
import dev.kord.core.behavior.ban
import dev.kord.core.behavior.reply
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import mu.KotlinLogging
import pw.modder.answernator.cache.AntiSpamCache
import pw.modder.answernator.db.Db
import pw.modder.answernator.db.guild.Features
import pw.modder.answernator.utils.Globals
import pw.modder.answernator.utils.extensions.kord.authorId
import pw.modder.answernator.utils.extensions.kord.guildId

private val logger = KotlinLogging.logger {}
fun Kord.antiSpam() {
    on<MessageCreateEvent> {
        if (message.data.author.bot.orElse(false)) return@on
        val guild = message.guildId
            ?: return@on

        if (message.content.startsWith(Globals.config.prefix) && message.content.length <= 48) return@on

        val config = Db.getAntiSpamConfig(guild.asString)

        logger.debug { "Guild $guild, antispam enabled: ${config.isEnabled(Features.ANTI_SPAM)}" }
        if (!config.isEnabled(Features.ANTI_SPAM)) return@on

        val increment = AntiSpamCache.increment(message)
        logger.debug { "Guild $guild, message ${message.id}, increment = $increment" }

        when(increment) {
            config.antiSpamWarn -> {
                logger.info { "Warning user: guild $guild, message ${message.id}, user ${message.authorId}, content = <${message.content}>" }
                if (config.isEnabled(Features.ANTI_SPAM_SILENT))
                    return@on

                message.reply { content = config.antiSpamWarnText.format(message.author?.mention ?: "\$user") }
            }
            config.antiSpamBan -> {
                logger.info { "Creating ban: guild $guild, message ${message.id}, user ${message.authorId}, content = <${message.content}>" }

                message.getGuild().ban(message.author!!.id) {
                    deleteMessagesDays = 1
                    reason = config.antiSpamBanText
                }
            }
        }
    }
}