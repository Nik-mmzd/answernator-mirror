package pw.modder.answernator

import com.jessecorbett.diskord.dsl.bot
import kotlinx.serialization.UnstableDefault
import pw.modder.answernator.utils.CommandList
import pw.modder.answernator.utils.GlobalConfig
import pw.modder.answernator.utils.loadCommandService

@UnstableDefault
suspend fun main() {
    CommandList.load()

    bot(GlobalConfig.get().token) {
        loadCommandService()
    }
}
