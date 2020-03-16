package pw.modder.answernator.utils.extensions

import com.jessecorbett.diskord.api.model.Attachment
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.http.userAgent
import kotlinx.serialization.UnstableDefault
import pw.modder.answernator.utils.Globals

@UnstableDefault
private val userAgent = "DiscordBot: (https://gitlab.com/jesselcorbett/diskord, ${Globals.getDependencyVersion("com.jesselcorbett", "diskord")})"
@UnstableDefault
suspend fun Attachment.downloadAsString(): String {
    return Globals.httpClient.get<String> {
        header("Authorization", "Bot ${Globals.config.token}")
        userAgent(userAgent)
        url(this@downloadAsString.url)
    }
}