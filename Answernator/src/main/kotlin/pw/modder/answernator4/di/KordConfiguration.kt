package pw.modder.answernator4.di

import dev.kord.core.Kord
import org.kodein.di.DI

/**
 * A unit of runtime Kord configuration contributed by a module — registers event listeners, services,
 * command dispatchers, etc. This is the v4 replacement for the legacy `Kord`-extension-method
 * composition (`commandService()`, `muteService()`, …).
 *
 * Modules bind these into a `Set<KordConfiguration>` (typically via `inBindSet<KordConfiguration>` in
 * their [KodeinModuleProvider.module]); the bot applies all of them to the live Kord instance at
 * startup via [deployTo]. The container is passed to [configure] so a configuration can resolve other
 * bindings (e.g. the `Set<`[pw.modder.answernator4.interaction.Command]`>`).
 */
fun interface KordConfiguration {
    suspend fun Kord.configure(di: DI)
}