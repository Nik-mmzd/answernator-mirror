package pw.modder.answernator4.di

import org.kodein.di.DI
import org.kodein.di.inBindSet
import pw.modder.answernator4.BuildConfig
import pw.modder.answernator4.kord.zombieWatchdog

/**
 * Core module that deploys the zombie-connection watchdog onto Kord. Requires no gateway intents
 * (it only reacts to connection lifecycle events).
 */
class ZombieWatchdogModule : KodeinModuleProvider {
    override val module = DI.Module("ZombieWatchdog") {
        inBindSet<KordConfiguration> {
            addSingleton {
                KordConfiguration { zombieWatchdog() }
            }
        }
    }

    override val version = BuildConfig.APP_VERSION
}