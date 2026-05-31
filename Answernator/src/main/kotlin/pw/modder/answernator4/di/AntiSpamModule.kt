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
import pw.modder.answernator4.db.cache.AntiSpamConfigRepository
import pw.modder.answernator4.db.cache.MrBeastRepository
import pw.modder.answernator4.db.cache.SpamMuteRepository
import pw.modder.answernator4.db.tables.AntiSpamConfigs
import pw.modder.answernator4.db.tables.MrBeastLovers
import pw.modder.answernator4.db.tables.SpamMutes

/**
 * Anti-spam feature module. For now it only wires data access — the database ([DatabaseModule]),
 * the config cache ([AntiSpamConfigRepository]) and its Flyway migrations. Kord event handling is
 * intentionally not wired yet.
 *
 * Contributes its own migration location individually (the parent `db/migrations` folder is not
 * scanned). All locations share a single Flyway history, so this module owns the `1.x` version
 * namespace; keep its migration versions distinct from other modules' (e.g. Logs owns `2.x`).
 */
class AntiSpamModule : KodeinModuleProvider {
    override val module = DI.Module("AntiSpam") {
        importOnce(DatabaseModule)

        bindSingleton { AntiSpamConfigRepository(instance()) }
        bindSingleton { SpamMuteRepository(instance()) }
        bindSingleton { MrBeastRepository(instance()) }

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
    }

    override val version = BuildConfig.APP_VERSION
}