package pw.modder.answernator.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.Bot
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.util.words
import kotlinx.serialization.UnstableDefault
import pw.modder.answernator.utils.Command
import java.util.*

@UnstableDefault
class Leave: Command {
    override val name = "leave"
    override val userGroup = Command.UserGroup.OWNER
    override val cmdType = Command.CommandGroup.OWNER
    override fun getHelp(locale: Locale): String? {
        return "Usage: `leave` to get guild ids or `leave guild-id` to leave guild"
    }

    override fun getDescription(locale: Locale): String? {
        return "leave any server"
    }

    override suspend fun action(bot: Bot, message: Message, locale: Locale): CombinedMessageEmbed {
        return try {
            bot.clientStore.guilds[message.words[1]].leave()
            textMessage("Done.")
        } catch (e: Exception) {
            textMessage(bot.clientStore.discord.getGuilds()
                .joinToString("\n", prefix = "${getHelp(locale)}\nAvailable guilds:\n") {
                    "${it.name}: `${it.id}`"
                }
            )
        }
    }
}