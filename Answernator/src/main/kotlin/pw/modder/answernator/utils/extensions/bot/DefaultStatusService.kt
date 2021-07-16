package pw.modder.answernator.utils.extensions.bot

import dev.kord.common.entity.ActivityType
import dev.kord.common.entity.DiscordBotActivity
import dev.kord.common.entity.PresenceStatus
import dev.kord.core.Kord
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.on
import dev.kord.gateway.UpdateStatus
import pw.modder.answernator.utils.Globals

suspend fun Kord.defaultStatusService() {
    on<ReadyEvent> {
        gateway.send(UpdateStatus(
            status = PresenceStatus.Online,
            activities = listOf(DiscordBotActivity(name = Globals.config.defaultStatus, type = ActivityType.Game)),
            since = null,
            afk = false
        ))
    }
}