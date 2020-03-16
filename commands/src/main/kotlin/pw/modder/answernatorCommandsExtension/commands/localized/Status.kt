package pw.modder.answernatorCommandsExtension.commands.localized

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.model.UserStatus
import com.jessecorbett.diskord.api.websocket.model.ActivityType
import com.jessecorbett.diskord.api.websocket.model.UserStatusActivity
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.util.words
import kotlinx.coroutines.*
import kotlinx.serialization.UnstableDefault
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.LocalizedCommand
import java.util.*

private var randomGamesTimer: Job? = null
@UnstableDefault
class Status: LocalizedCommand {
    override val name = "status"

    override val userGroup = Command.UserGroup.OWNER

    override suspend fun action(bot: Bot, message: Message, texts: ResourceBundle): CombinedMessageEmbed {
        return when(message.words.getOrNull(1)?.toLowerCase()) {
            "online" -> {
                bot.setActive()
                texts.message("online")
            }
            "dnd" -> {
                bot.setDoNotDisturb()
                texts.message("dnd")
            }
            "idle" -> {
                bot.setIdle()
                texts.message("idle")
            }
            "invisible" -> {
                bot.setInvisible()
                texts.message("invisible")
            }
            "game" -> {
                val game = message.words.drop(2).joinToString(" ")
                bot.setStatus(
                    status = UserStatus.ONLINE,
                    activity = UserStatusActivity(
                        name = game,
                        type = ActivityType.GAME
                    )
                )
                texts.message("playing", game)
            }
            "randomgame" -> {
                if (message.words.size == 2) {
                    val game = gamesdb.getRandomGame()
                    bot.setStatus(
                        status = UserStatus.ONLINE,
                        activity = UserStatusActivity(
                            name = game,
                            type = ActivityType.GAME
                        )
                    )
                    return texts.message("playing", game)
                }
                if (message.words.getOrNull(2)?.toLowerCase() == "stop") {
                    randomGamesTimer?.run {
                        cancel()
                        randomGamesTimer = null
                    }
                    return texts.message("playing.timed.stopped")
                }

                if (randomGamesTimer != null) {
                    return texts.message("playing.timed.running")
                }

                val timeout = message.words[2].toLongOrNull()
                    ?: return texts.message("playing.timed.error")

                if (timeout < 30) return texts.message("playing.timed.error")

                randomGamesTimer = GlobalScope.launch {
                    while (isActive) {
                        bot.setStatus(
                            status = UserStatus.ONLINE,
                            activity = UserStatusActivity(
                                name = gamesdb.getRandomGame(),
                                type = ActivityType.GAME
                            )
                        )
                        delay(timeout*60*1000)
                    }
                }
                texts.message("playing.timed.started", timeout)
            }
            "custom" -> TODO("See https://github.com/discordapp/discord-api-docs/issues/1160")
            /*"custom" -> bot.setStatus(
                status = UserStatus.ONLINE,
                activity = UserStatusActivity(
                    name = "stub",
                    type = ActivityType.CUSTOM_STATUS,
                    partyStatus = message.words.drop(2).joinToString(" ")
                )
            )*/
            "clear" -> {
                bot.setStatus(
                    status = UserStatus.ONLINE,
                    activity = UserStatusActivity(
                        name = "",
                        type = ActivityType.GAME
                    )
                )
                texts.message("clear")
            }
            else -> texts.errorMessage()
        }
    }
}