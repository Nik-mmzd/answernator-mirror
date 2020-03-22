package pw.modder.answernator.utils.extensions

import com.jessecorbett.diskord.api.model.Attachment
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.http.cookies
import io.ktor.http.userAgent
import kotlinx.serialization.UnstableDefault
import pw.modder.answernator.utils.Globals

@UnstableDefault
private val userAgent = "DiscordBot: (https://gitlab.com/jesselcorbett/diskord, ${Globals.getDependencyVersion("com.jessecorbett", "diskord-jvm")})"
@UnstableDefault
suspend fun Attachment.downloadAsString(): String {
    return Globals.httpClient.get<String> {
        userAgent(userAgent)
        url(this@downloadAsString.proxiedUrl)
        cookies()
    }
}