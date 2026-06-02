package pw.modder.answernator4.di

import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.direct
import org.kodein.di.inBindSet
import org.kodein.di.instance
import pw.modder.answernator4.BuildConfig
import pw.modder.answernator4.interaction.Command
import pw.modder.answernator4.interaction.CommandRegistry
import pw.modder.answernator4.interaction.interactionCommandService

/**
 * Core module that deploys the interaction (slash/user/message command + button) dispatcher onto Kord,
 * registering every [Command] contributed to the container by other modules (e.g. [DefaultCommandsModule]).
 */
class InteractionModule : KodeinModuleProvider {
    override val module = DI.Module("Interactions") {
        bindSingleton { CommandRegistry() }

        inBindSet<KordConfiguration> {
            addSingleton {
                KordConfiguration { di ->
                    interactionCommandService(di)
                }
            }
        }
    }

    override val version = BuildConfig.APP_VERSION
}
