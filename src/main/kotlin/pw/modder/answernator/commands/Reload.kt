package pw.modder.answernator.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.dsl.message
import com.jessecorbett.diskord.util.ClientStore
import kotlinx.serialization.UnstableDefault
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.CommandList
import pw.modder.answernator.utils.UserGroup

@UnstableDefault
class Reload: Command {
    override val command: String = "reload"
    override val userGroup: UserGroup = UserGroup.OWNER
    override suspend fun action(clientStore: ClientStore, message: Message) {
        CommandList.load()
        message {
            text = "Reloaded. Loaded ${CommandList.commands.size} commands."
        }
    }
}
