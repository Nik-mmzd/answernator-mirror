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
                textMessage(texts.getString("online"))
            }
            "dnd" -> {
                bot.setDoNotDisturb()
                textMessage(texts.getString("dnd"))
            }
            "idle" -> {
                bot.setIdle()
                textMessage(texts.getString("idle"))
            }
            "invisible" -> {
                bot.setInvisible()
                textMessage(texts.getString("invisible"))
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
                textMessage(texts.formatString("playing", game))
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
                    return textMessage(texts.formatString("playing", game))
                }
                if (message.words.getOrNull(2)?.toLowerCase() == "stop") {
                    randomGamesTimer?.cancel()
                    return textMessage(texts.getStringOrKey("playing.timed.stopped"))
                }

                if (randomGamesTimer != null) {
                    return textMessage(texts.getStringOrKey("playing.timed.running"))
                }

                val timeout = message.words[2].toLongOrNull()
                    ?: return textMessage(texts.getString("playing.timed.error"))

                if (timeout < 30) return textMessage(texts.getString("playing.timed.error"))

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
                textMessage(texts.formatString("playing.timed.started", timeout))
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
                textMessage(texts.getStringOrKey("clear"))
            }
            else -> textMessage(texts.getStringOrKey("error"))
        }
    }
}