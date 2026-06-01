package pw.modder.answernator4.di

import io.github.oshai.kotlinlogging.KotlinLogging
import pw.modder.answernator4.Env
import java.io.File
import java.net.URLClassLoader
import java.util.ServiceLoader

private val logger = KotlinLogging.logger {}

/**
 * Discovers [KodeinModuleProvider]s via [ServiceLoader], both from the core artifact and from
 * hot-swappable plugin JARs in `./commands/`, mirroring [pw.modder.answernator4.interaction.InteractionCommandList].
 *
 * Each provider contributes a [org.kodein.di.DI.Module] to the application container, replacing the
 * legacy `Kord`-extension-method composition with declarative DI bindings.
 */
object KodeinModuleList {
    var providers: List<KodeinModuleProvider> = listOf()
        private set

    fun load() {
        val classLoader = URLClassLoader(
            File(".").resolve("commands")
                .also { if (!it.exists()) it.mkdirs() }
                .listFiles { file -> file.isFile && file.extension.equals("jar", ignoreCase = true) }
                ?.map { it.toURI().toURL() }?.toTypedArray() ?: arrayOf()
        )
        providers = ServiceLoader.load(KodeinModuleProvider::class.java, classLoader).onEach {
            logger.info { "Discovered module $it" }
        }.filter {
            Env.ENABLED_MODULES.isEmpty() || Env.ENABLED_MODULES.contains(it.name.lowercase())
        }.onEach {
            logger.info { "Loaded module $it" }
        }
        logger.info {
            "Loaded ${providers.size} DI modules: " +
                providers.joinToString { "${it.name} (${it.version})" }
        }
    }
}
