package pw.modder.answernator.utils.extensions.bot

import dev.kord.core.Kord
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.guild.GuildCreateEvent
import dev.kord.core.on
import mu.KotlinLogging
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import pw.modder.answernator.db.Db
import pw.modder.answernator.db.guild.Configs

private val logger = KotlinLogging.logger {  }
suspend fun Kord.configService() {
    on<ReadyEvent> {
        guildIds.forEach {
            if (transaction { Configs.select { Configs.guildId eq it.asString }.count() } == 0L)
                try {
                    Db.createDefaultConfig(it)
                    logger.info { "Created config for guild ${it.asString}" }
                } catch (e: Exception) {
                    logger.warn(e) { "Cannot create config for guild ${it.asString}" }
                }
        }
    }

    on<GuildCreateEvent> {
        if (transaction { Configs.select { Configs.guildId eq guild.id.asString }.count() } == 0L)
            try {
                Db.createDefaultConfig(guild.id)
                logger.info { "Created config for guild ${guild.name} (${guild.id.asString})" }
            } catch (e: Exception) {
                logger.warn(e) { "Cannot create config for guild ${guild.name} (${guild.id.asString})" }
            }
    }
}