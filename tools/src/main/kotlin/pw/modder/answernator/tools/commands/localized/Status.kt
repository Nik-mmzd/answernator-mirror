package pw.modder.answernator.tools.commands.localized

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.model.UserStatus
import com.jessecorbett.diskord.api.websocket.model.ActivityType
import com.jessecorbett.diskord.api.websocket.model.UserStatusActivity
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.util.words
import kotlinx.coroutines.*
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.LocalizedCommand
import pw.modder.answernator.utils.locale.CommandLocaleBundle
import java.util.*

private var randomGamesTimer: Job? = null
class Status: LocalizedCommand {
    override val name = "status"

    override val userGroup = Command.UserGroup.OWNER
    override val cmdType = Command.CommandGroup.OWNER

    override suspend fun action(bot: Bot, message: Message, texts: CommandLocaleBundle): CombinedMessageEmbed {
        return when(message.words.getOrNull(1)?.toLowerCase()) {
            "online" -> {
                bot.setActive()
                texts.getString("online")
            }
            "dnd" -> {
                bot.setDoNotDisturb()
                texts.getString("dnd")
            }
            "idle" -> {
                bot.setIdle()
                texts.getString("idle")
            }
            "invisible" -> {
                bot.setInvisible()
                texts.getString("invisible")
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
                texts.formatString("playing", game)
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
                    return texts.formatString("playing", game).toMessage()
                }
                if (message.words.getOrNull(2)?.toLowerCase() == "stop") {
                    randomGamesTimer?.run {
                        cancel()
                        randomGamesTimer = null
                    }
                    return texts.getString("playing.timed.stopped").toMessage()
                }

                if (randomGamesTimer != null) {
                    return texts.getString("playing.timed.running").toMessage()
                }

                val timeout = message.words[2].toLongOrNull()
                    ?: return texts.getString("playing.timed.error").toMessage()

                if (timeout < 30) return texts.getString("playing.timed.error").toMessage()

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
                texts.formatString("playing.timed.started", timeout)
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
                texts.getString("clear")
            }
            else -> texts.getErrorString()
        }.toMessage()
    }
}