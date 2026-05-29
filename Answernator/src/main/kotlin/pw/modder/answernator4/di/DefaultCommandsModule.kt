package pw.modder.answernator4.di

import org.kodein.di.DI
import org.kodein.di.inBindSet
import pw.modder.answernator4.BuildConfig
import pw.modder.answernator4.command.*
import pw.modder.answernator4.interaction.Command

class DefaultCommandsModule : KodeinModuleProvider {
    override val module = DI.Module("DefaultCommands") {
        inBindSet<Command> {
            addSingleton { BanCommandIssuer(di) }
            addSingleton { BanInfo(di) }
            addSingleton { BotInfo(di) }
            addSingleton { Clean(di) }
            addSingleton { DebugInfo(di) }
            addSingleton { Dice(di) }
            addSingleton { GetCommandIssuer(di) }
            addSingleton { RandomGame(di) }
            addSingleton { Ubuntu(di) }
        }
    }

    override val version = BuildConfig.APP_VERSION
}
