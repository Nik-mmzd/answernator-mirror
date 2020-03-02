package pw.modder.answernator.commands

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.util.ClientStore
import com.jessecorbett.diskord.util.sendMessage
import kotlinx.serialization.UnstableDefault
import pw.modder.answernator.utils.Command
import pw.modder.answernator.utils.CommandList
import pw.modder.answernator.utils.UserGroup

@UnstableDefault
class Reload: Command {
    override val name: String = "reload"
    override val userGroup: UserGroup = UserGroup.OWNER
    override suspend fun action(clientStore: ClientStore, message: Message) {
        CommandList.load()
        clientStore.channels[message.channelId].sendMessage("Reloaded. Loaded ${CommandList.commands.size} commands.")
    }
}
