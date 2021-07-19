package pw.modder.answernator.utils.extensions.bot

import dev.kord.core.Kord
import dev.kord.core.event.guild.GuildCreateEvent
import dev.kord.core.on
import mu.KotlinLogging
import pw.modder.answernator.db.Db

private val logger = KotlinLogging.logger {  }
suspend fun Kord.configService() {
    on<GuildCreateEvent> {
        Db.getConfig(guild.id) ?: Db.createDefaultConfig(guild.id).also {
            logger.info { "Created default config for guild ${guild.name} (id ${guild.id})" }
        }
    }
}