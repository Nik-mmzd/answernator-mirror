package pw.modder.answernator.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.model.Permissions
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.util.ClientStore
import com.jessecorbett.diskord.util.authorId
import com.jessecorbett.diskord.util.words
import kotlinx.serialization.UnstableDefault
import pw.modder.answernator.utils.*
import java.util.*
import com.jessecorbett.diskord.dsl.message as dslmessage

@UnstableDefault
class Help: LocalizedCommand {
    override val name: String = "help"

    override suspend fun action(clientStore: ClientStore, message: Message, texts: ResourceBundle): CombinedMessageEmbed {
        val guildClient = message.guildId?.run { clientStore.guilds[this] }
        if (message.words.size == 1) {
            val permissions = when (guildClient) {
                null -> Permissions.NONE
                else -> guildClient.computePermissions(message.authorId)
            }
            return dslmessage {
                title = texts.getStringOrKey("title_cmdlist")
                description = CommandList.commands.filter { it.check(message, permissions) }.joinToString(separator = "\n") { "`${it.name}`" }
            }
        }

        val cmd = CommandList.commands.singleOrNull { it.name == message.words[1].toLowerCase() }
            ?: return dslmessage {
                title = texts.formatString("title", message.words[1].removeGraves())
                description = texts.formatString("not_found", message.words[1].removeGraves())
            }

        if (message.authorId != GlobalConfig.get().author && !cmd.check(message, guildClient)) return dslmessage {
            title = texts.formatString("title", message.words[1])
            description = texts.getStringOrKey("no_permissions")
        }

        return dslmessage {
            title = texts.formatString("title", message.words[1])
            description = cmd.getHelp(texts.locale) ?: texts.getString("not_available")
        }
    }
}