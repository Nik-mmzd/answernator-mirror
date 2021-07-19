package pw.modder.answernator.utils.extensions.bot

import dev.kord.common.entity.PresenceStatus
import dev.kord.core.Kord
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.on
import pw.modder.answernator.utils.Globals

suspend fun Kord.defaultStatusService() {
    on<ReadyEvent> {
        kord.editPresence {
            status = PresenceStatus.Online
            playing(Globals.config.defaultStatus)
        }
    }
}