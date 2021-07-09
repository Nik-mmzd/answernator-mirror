package pw.modder.answernator.utils.extensions.bot

import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.DiskordDsl
import mu.KotlinLogging
import org.jetbrains.exposed.sql.transactions.transaction
import pw.modder.answernator.db.Db
import pw.modder.answernator.db.guild.Config
import pw.modder.answernator.db.guild.Configs

private val logger = KotlinLogging.logger {}
@DiskordDsl
fun Bot.configService() {
    guildCreated {
        if (transaction { Config.find { Configs.guildId eq it.id } }.count() == 0L) {
            Db.createDefaultConfig(it.id)
            logger.debug { "Created default config for guild ${it.name} (ID ${it.id})" }
        }
    }
}