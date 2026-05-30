package pw.modder.answernator4.di

import org.flywaydb.core.api.Location
import org.flywaydb.core.api.locations.LocationParser
import org.jetbrains.exposed.v1.core.Table
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.inBindSet
import org.kodein.di.instance
import pw.modder.answernator4.BuildConfig
import pw.modder.answernator4.db.DatabaseModule
import pw.modder.answernator4.db.cache.LogsConfigRepository
import pw.modder.answernator4.db.tables.LogsConfigs
import pw.modder.answernator4.kord.guildLogService
import pw.modder.answernator4.kord.zombieWatchdog

/**
 * Guild logging feature module. For now it only wires data access — the database ([DatabaseModule]),
 * the config cache ([LogsConfigRepository]) and its Flyway migrations. Kord event handling is
 * intentionally not wired yet.
 *
 * Contributes its own migration location individually (the parent `db/migrations` folder is not
 * scanned). All locations share a single Flyway history, so this module owns the `2.x` version
 * namespace; keep its migration versions distinct from other modules' (e.g. AntiSpam owns `1.x`).
 */
class LogsModule : KodeinModuleProvider {
    override val module = DI.Module("Logs") {
        importOnce(DatabaseModule)

        bindSingleton { LogsConfigRepository(instance()) }

        inBindSet<Table>(tag = "tables-in-use") {
            addSingleton { LogsConfigs }
        }

        inBindSet<Location>(tag = "flyway-migrations") {
            addSingleton {
                LocationParser.parseLocation("classpath:pw/modder/answernator4/db/migrations/guild_logs")
            }
        }

        inBindSet<KordConfiguration> {
            addSingleton {
                KordConfiguration { guildLogService(di) }
            }
        }
    }

    override val version = BuildConfig.APP_VERSION
}
