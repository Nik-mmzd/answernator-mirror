package pw.modder.answernator4.di

import dev.kord.gateway.Intent
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import org.flywaydb.core.api.Location
import org.flywaydb.core.api.locations.LocationParser
import org.jetbrains.exposed.v1.core.Table
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.inBindSet
import org.kodein.di.instance
import pw.modder.answernator4.BuildConfig
import pw.modder.answernator4.command.AntiSpamConfig
import pw.modder.answernator4.db.DatabaseModule
import pw.modder.answernator4.db.cache.AntiSpamConfigRepository
import pw.modder.answernator4.db.cache.MrBeastRepository
import pw.modder.answernator4.db.cache.SpamMuteRepository
import pw.modder.answernator4.db.tables.AntiSpamConfigs
import pw.modder.answernator4.db.tables.MrBeastLovers
import pw.modder.answernator4.db.tables.SpamMutes
import pw.modder.answernator4.kord.antiSpamService

/**
 * Anti-spam feature module. Wires data access (the database [DatabaseModule], the config cache
 * [AntiSpamConfigRepository], the [SpamMuteRepository]/[MrBeastRepository] stores and Flyway
 * migrations) and the runtime enforcement service ([antiSpamService]) plus the gateway intents it
 * needs. MrBeast and command-spam handling are layered onto the same service.
 *
 * Contributes its own migration location individually (the parent `db/migrations` folder is not
 * scanned). All locations share a single Flyway history, so this module owns the `1.x` version
 * namespace; keep its migration versions distinct from other modules' (e.g. Logs owns `2.x`).
 */
class AntiSpamModule : KodeinModuleProvider {
    @OptIn(PrivilegedIntent::class)
    override val module = DI.Module("AntiSpam") {
        importOnce(DatabaseModule)

        bindSingleton { AntiSpamConfigRepository(instance()) }
        bindSingleton { SpamMuteRepository(instance()) }
        bindSingleton { MrBeastRepository(instance()) }

        inBindSet<pw.modder.answernator4.interaction.Command> {
            addSingleton { AntiSpamConfig(di) }
        }

        inBindSet<Table>(tag = "tables-in-use") {
            addSingleton { AntiSpamConfigs }
            addSingleton { MrBeastLovers }
            addSingleton { SpamMutes }
        }

        inBindSet<Location>(tag = "flyway-migrations") {
            addSingleton {
                LocationParser.parseLocation("classpath:pw/modder/answernator4/db/migrations/antispam")
            }
        }

        inBindSet<KordConfiguration> {
            addSingleton {
                KordConfiguration { antiSpamService(di) }
            }
        }

        // MessageContent is a privileged intent (enable it in the Dev Portal); needed to read text.
        inBindSet<Intents> {
            addSingleton {
                Intents { +Intent.MessageContent; +Intent.GuildMessages }
            }
        }
    }

    override val version = BuildConfig.APP_VERSION
}