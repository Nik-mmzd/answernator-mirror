package pw.modder.answernator.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.util.ClientStore
import kotlinx.serialization.UnstableDefault
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.CommandList
import pw.modder.answernator.utils.UserGroup
import java.util.*

@UnstableDefault
class Reload: Command {
    override val name: String = "reload"
    override val userGroup: UserGroup = UserGroup.OWNER

    override fun getHelp(locale: Locale): String? {
        return "Reloads bot. Not a restart! Not localized."
    }

    override suspend fun action(clientStore: ClientStore, message: Message, locale: Locale): CombinedMessageEmbed {
        CommandList.load()
        return textMessage("Reloaded. Loaded ${CommandList.commands.size} commands.")
    }
}
