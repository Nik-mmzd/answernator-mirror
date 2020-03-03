package pw.modder.answernator.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.util.ClientStore
import com.jessecorbett.diskord.util.words
import kotlinx.serialization.UnstableDefault
import pw.modder.answernator.utils.CommandList
import pw.modder.answernator.utils.LocalizedCommand
import java.util.*
import com.jessecorbett.diskord.dsl.message as dslmessage

@UnstableDefault
class Help: LocalizedCommand {
    override val name: String = "help"

    override suspend fun action(clientStore: ClientStore, message: Message, locale: Locale): CombinedMessageEmbed {
        if (message.words.size == 1) return dslmessage {
            title = getString(locale, "title_cmdlist")
            description = CommandList.commands.joinToString(separator = "\n") { "`${it.name}`" }
        }

        return CommandList.commands.singleOrNull { it.name == message.words[1] }?.run {
            dslmessage {
                title = this@Help.formatString(locale, "title", message.words[1])
                description = this@run.getHelp(locale) ?: this@Help.getString(locale, "not_available")
            }
        } ?: dslmessage {
            title = this@Help.formatString(locale, "title", message.words[1])
            description = this@Help.formatString(locale, "not_found", message.words[1])
        }
    }
}