package pw.modder.answernator.utils.extensions

import dev.kord.core.entity.Attachment
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.client.statement.*
import io.ktor.http.cookies
import io.ktor.http.userAgent
import pw.modder.answernator.utils.Globals

private val userAgent = "DiscordBot: (https://github.com/kordlib/kord, ${Globals.getDependencyVersion("dev.kord", "kord-core")})"
suspend fun Attachment.downloadAsString(proxied: Boolean = false): String {
    return Globals.httpClient.get {
        userAgent(userAgent)
        url(if (proxied) this@downloadAsString.proxyUrl else this@downloadAsString.url)
        cookies()
    }.bodyAsText(Charsets.UTF_8)
}
