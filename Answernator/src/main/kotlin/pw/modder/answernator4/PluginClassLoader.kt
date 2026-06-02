package pw.modder.answernator4

/**
 * Classloader spanning the hot-swappable plugin JARs in `./commands`, shared by every subsystem
 * that resolves resources which may ship inside those plugin JARs.
 *
 * Defaults to this framework's own loader. At startup [pw.modder.answernator4.di.KodeinModuleList]
 * swaps in the loader over the JARs in `./commands`; since that loader delegates to the app loader as
 * its parent, it resolves both core resources and plugin-module resources, which the app loader alone
 * cannot see.
 *
 * Consumers:
 * - command resource bundles under `locale` in [pw.modder.answernator4.interaction.getAllLocalizations]
 *   and the `bundle`/`gbundle` accessors on [pw.modder.answernator4.interaction.Command];
 * - Flyway `classpath:` migration scanning in [pw.modder.answernator4.db.DatabaseModule], so a plugin
 *   contributing a `flyway-migrations` location pointing into its own JAR is actually found.
 */
internal object PluginClassLoader {
    @Volatile
    var value: ClassLoader = PluginClassLoader::class.java.classLoader
}