package pw.modder.answernator4.di

import dev.kord.core.Kord
import dev.kord.gateway.Intents
import org.kodein.di.DI
import org.kodein.di.bindInstance
import org.kodein.di.bindSet
import org.kodein.di.direct
import org.kodein.di.instance
import pw.modder.answernator4.interaction.Command

/**
 * Assembles the application [DI] container from every [KodeinModuleProvider] discovered by
 * [KodeinModuleList].
 *
 * The [kord] instance is bound so modules may inject it. Multibindings declared here let provider
 * modules contribute via `inBindSet` (e.g. [DefaultCommandsModule]):
 * - `Set<`[Command]`>` — interaction commands to register.
 * - `Set<`[KordConfiguration]`>` — runtime Kord setup steps applied by [deployTo].
 * - `Set<`[Intents]`>` — gateway intents each module needs; folded into a single [Intents] binding.
 *
 * [KodeinModuleList.load] must be called first.
 */
fun buildDi(kord: Kord): DI = DI {
    bindInstance { kord }

    bindSet<Command>()
    bindSet<KordConfiguration>()
    bindSet<Intents>()

    KodeinModuleList.providers.forEach { importOnce(it.module) }
}

/**
 * Deploys every [KordConfiguration] contributed by the loaded modules onto [kord], wiring their event
 * handlers and services. Must be called before [Kord.login].
 */
suspend fun DI.deployTo(kord: Kord) {
    direct.instance<Set<KordConfiguration>>().forEach { with(it) { kord.configure(this@deployTo) } }
}
