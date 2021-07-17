package pw.modder.answernator.tools.commands.localized

import dev.kord.common.entity.PresenceStatus
import dev.kord.core.entity.Message
import kotlinx.coroutines.*
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.LocalizedCommand
import pw.modder.answernator.utils.extensions.kord.reply
import pw.modder.answernator.utils.locale.CommandLocaleBundle

private var randomGamesTimer: Job? = null
class Status: LocalizedCommand {
    override val name = "status"

    override val userGroup = Command.UserGroup.OWNER
    override val cmdType = Command.CommandGroup.OWNER

    override suspend fun action(message: Message, args: List<String>, texts: CommandLocaleBundle) {
        message.reply(when(args.firstOrNull()?.lowercase()) {
            "online" -> {
                message.kord.editPresence {
                    this.status = PresenceStatus.Online
                }
                texts.getString("online")
            }
            "dnd" -> {
                message.kord.editPresence {
                    this.status = PresenceStatus.DoNotDisturb
                }
                texts.getString("dnd")
            }
            "idle" -> {
                message.kord.editPresence {
                    this.status = PresenceStatus.Idle
                }
                texts.getString("idle")
            }
            "invisible" -> {
                message.kord.editPresence {
                    this.status = PresenceStatus.Invisible
                }
                texts.getString("invisible")
            }
            "game" -> {
                val game = args.drop(1).joinToString(" ")
                message.kord.editPresence {
                    this.status = PresenceStatus.Online
                    this.playing(game)
                }
                texts.formatString("playing", game)
            }
            "randomgame" -> when(args.getOrNull(1)) {
                null -> {
                    val game = gamesdb.getRandomGame()
                    message.kord.editPresence {
                        this.status = PresenceStatus.Online
                        this.playing(game)
                    }
                    texts.formatString("playing", game)
                }
                "stop" -> {
                    randomGamesTimer?.cancel()
                    randomGamesTimer = null
                    texts.getString("playing.timed.stopped")
                }
                else -> {
                    if (randomGamesTimer != null) {
                        message.reply(texts.getString("playing.timed.running"))
                        return
                    }

                    val timeout = args[1].toLongOrNull()
                    if (timeout == null) {
                        message.reply(texts.getString("playing.timed.error"))
                        return
                    }

                    if (timeout < 30) {
                        message.reply(texts.getString("playing.timed.error"))
                        return
                    }

                    randomGamesTimer = coroutineScope {
                        launch {
                            while (isActive) {
                                message.kord.editPresence {
                                    this.status = PresenceStatus.Online
                                    this.playing(gamesdb.getRandomGame())
                                }
                                delay(timeout * 60 * 1000)
                            }
                        }
                    }

                    texts.formatString("playing.timed.started", timeout)
                }
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
                message.kord.editPresence {
                    this.status = PresenceStatus.Online
                }
                texts.getString("clear")
            }
            else -> texts.getErrorString()
        })
    }
}