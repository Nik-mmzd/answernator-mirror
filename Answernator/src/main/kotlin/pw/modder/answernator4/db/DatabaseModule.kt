package pw.modder.answernator4.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.Location
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils
import org.kodein.di.DI
import org.kodein.di.bindSet
import org.kodein.di.bindSingleton
import org.kodein.di.inBindSet
import org.kodein.di.instance
import pw.modder.answernator4.Env
import pw.modder.answernator4.PluginClassLoader
import javax.sql.DataSource

val DatabaseModule = DI.Module("AnswernatorDB") {
    bindSingleton {
        if (Env.Db.DB_URL.isEmpty())
            throw RuntimeException("Database needs to be configured. Please set ANSWR4_DB_URL, ANSWR4_DB_USER and ANSWR4_DB_PASSWORD env variables.")

        HikariConfig().apply {
            jdbcUrl = Env.Db.DB_URL
            username = Env.Db.DB_USER
            password = Env.Db.DB_PASSWORD
        }
    }

    bindSingleton<DataSource> {
        HikariDataSource(instance())
    }

    bindSingleton {
        Database.connect(datasource = instance())
    }

    bindSet<Table>(tag = "tables-in-use")

    bindSet<Location>(tag = "flyway-migrations")
    bindSet<Pair<String, String>>(tag = "flyway-placeholders")
    bindSet<FlywayPlaceholdersProvider>(tag = "flyway-placeholder-providers")
    inBindSet<FlywayPlaceholdersProvider>(tag = "flyway-placeholder-providers") {
        addProvider { FlywayDataTypePlaceholders }
    }

    bindSingleton {
        val locations = instance<Set<Location>>(tag = "flyway-migrations")
        val placeholders = instance<Set<Pair<String, String>>>(tag = "flyway-placeholders").toMap()
        val placeholderProviders = instance<Set<FlywayPlaceholdersProvider>>(tag = "flyway-placeholder-providers")

        val database = instance<Database>()
        val computedPlaceholders = transaction(db = database) {
            placeholderProviders.fold(placeholders) { acc, provider ->
                acc + provider.providePlaceholders()
            }
        }

        val flyway = Flyway.configure(PluginClassLoader.value)
            .dataSource(instance())
            .outOfOrder(true)
            .locations(*locations.sorted().toTypedArray())
            .validateMigrationNaming(true)
            .loggers(FlywayLogCreator::class.qualifiedName)
            .placeholders(computedPlaceholders)
            .executeInTransaction(true)


        Env.Db.DB_BASELINE?.let { baselineVersion ->
            flyway.baselineOnMigrate(true).baselineVersion(baselineVersion)
        }

        flyway.load()
    }

    onReady {
        instance<Flyway>().migrate()
        val database = instance<Database>()

        val tablesInUse = instance<Set<Table>>(tag = "tables-in-use")
        val logger = KotlinLogging.logger("pw.modder.answernator4.ActualizeDB")

        val statements = transaction(db = database) {
            MigrationUtils.statementsRequiredForDatabaseMigration(*tablesInUse.toTypedArray(), withLogs = false)
        }

        if (statements.isNotEmpty()) {
            logger.warn { "SQL required to actualize a schema!" }
            statements.forEach { statement ->
                logger.warn { statement }
            }
        }
    }
}
