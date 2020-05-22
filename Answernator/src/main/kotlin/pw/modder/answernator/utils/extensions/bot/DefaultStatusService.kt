package pw.modder.answernator.utils.extensions.bot

import com.jessecorbett.diskord.api.model.UserStatus
import com.jessecorbett.diskord.api.websocket.model.ActivityType
import com.jessecorbett.diskord.api.websocket.model.UserStatusActivity
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.DiskordDsl
import pw.modder.answernator.utils.Globals

@DiskordDsl
fun Bot.defaultStatusService() {
    started {
        setStatus(
            status = UserStatus.ONLINE,
            activity = UserStatusActivity(
                name = Globals.config.defaultStatus,
                type = ActivityType.GAME
            )
        )
    }
}