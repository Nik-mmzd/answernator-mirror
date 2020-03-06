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

    override suspend fun action(clientStore: ClientStore, message: Message, locale: Locale): CombinedMessageEmbed {
        val guildClient = message.guildId?.run { clientStore.guilds[this] }
        if (message.words.size == 1) {
            val permissions = when (guildClient) {
                null -> Permissions.NONE
                else -> guildClient.computePermissions(message.authorId)
            }
            return dslmessage {
                title = getString(locale, "title_cmdlist")
                description = CommandList.commands.filter { it.check(message, permissions) }.joinToString(separator = "\n") { "`${it.name}`" }
            }
        }

        CommandList.commands.singleOrNull { it.name == message.words[1] }?.run {
            if (message.authorId != GlobalConfig.get().author && !check(message, guildClient)) return dslmessage {
                title = this@Help.formatString(locale, "title", message.words[1])
                description = this@Help.getString(locale, "no_permissions")
            }

            return dslmessage {
                title = this@Help.formatString(locale, "title", message.words[1])
                description = this@run.getHelp(locale) ?: this@Help.getString(locale, "not_available")
            }
        }
        return dslmessage {
            title = this@Help.formatString(locale, "title", message.words[1].removeGraves())
            description = this@Help.formatString(locale, "not_found", message.words[1].removeGraves())
        }
    }
}