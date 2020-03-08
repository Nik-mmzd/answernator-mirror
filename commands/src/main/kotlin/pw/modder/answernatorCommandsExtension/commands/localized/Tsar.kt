package pw.modder.answernatorCommandsExtension.commands.localized

import com.jessecorbett.diskord.api.model.Message
import com.jessecorbett.diskord.api.model.Permissions
import com.jessecorbett.diskord.api.rest.EmbedImage
import com.jessecorbett.diskord.api.rest.client.GuildClient
import com.jessecorbett.diskord.dsl.CombinedMessageEmbed
import com.jessecorbett.diskord.dsl.field
import com.jessecorbett.diskord.dsl.footer
import com.jessecorbett.diskord.util.ClientStore
import com.jessecorbett.diskord.util.words
import kotlinx.serialization.UnstableDefault
import pw.modder.answernator.utils.GlobalConfig
import pw.modder.answernator.utils.GuildConfigs
import pw.modder.answernator.utils.LocalizedCommand
import pw.modder.answernator.utils.extensions.setCurrentTimestamp
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import com.jessecorbett.diskord.dsl.message as dslmessage
import java.util.*
import kotlin.random.Random

private val random = Random(System.currentTimeMillis())
@UnstableDefault
class Tsar: LocalizedCommand {
    override val name = "царь"

    override suspend fun check(message: Message, guildClient: GuildClient?): Boolean {
        val locale = message.guildId?.run { GuildConfigs.get(this).locale } ?: GlobalConfig.get().locale
        return locale == Locale("ru") || super.check(message, guildClient)
    }

    override fun check(message: Message, permissions: Permissions): Boolean {
        val locale = message.guildId?.run { GuildConfigs.get(this).locale } ?: GlobalConfig.get().locale
        return locale == Locale("ru") || super.check(message, permissions)
    }

    override suspend fun action(clientStore: ClientStore, message: Message, texts: ResourceBundle): CombinedMessageEmbed {
        if (message.words.getOrNull(1)?.toLowerCase() != "велит")
            return textMessage(texts.getStringOrKey("invalid"))

        val decreeCount = texts.getStringOrKey("decree.count").toInt()
        val signCount = texts.getStringOrKey("sign.count").toInt()


        return dslmessage {
            title = texts.getStringOrKey("title")

            field(texts.getStringOrKey("decree.title"), texts.getStringOrKey("decree.${random.nextInt(0, decreeCount)}"), false)

            color = 16711680
            thumbnail = EmbedImage(texts.getStringOrKey("thumbnail"))

            footer(texts.getStringOrKey("sign.${random.nextInt(0, signCount)}")) {
                iconUrl = texts.getStringOrKey("footer.icon")
            }

            setCurrentTimestamp()
        }
    }
}