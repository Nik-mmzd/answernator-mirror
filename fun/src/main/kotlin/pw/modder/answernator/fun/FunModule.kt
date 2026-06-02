package pw.modder.answernator.`fun`

import org.kodein.di.DI
import org.kodein.di.inBindSet
import pw.modder.answernator.`fun`.commands.Quote
import pw.modder.answernator.`fun`.commands.Tsar
import pw.modder.answernator4.di.KodeinModuleProvider
import pw.modder.answernator4.interaction.Command

/**
 * v4 entry point for the optional `fun` plugin module. Discovered via [java.util.ServiceLoader]
 * from the hot-swappable plugin JARs in `./commands` and registered through [KodeinModuleProvider].
 */
class FunModule : KodeinModuleProvider {
    override val module = DI.Module("fun") {
        inBindSet<Command> {
            addSingleton { Tsar(di) }
            addSingleton { Quote(di) }
        }
    }

    override val version: String = BuildConfig.APP_VERSION
}
