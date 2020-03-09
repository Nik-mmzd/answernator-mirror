package pw.modder.answernator.utils

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UnstableDefault
import java.util.*

@UnstableDefault
@Serializable
data class GuildConfig(
    val guildId: String,
    val lang: String = Globals.config.lang,
    val greetNewUsers: Boolean = false,
    val greetingText: String = "%2\$s greets %1\$s!",
    val greetingsChannel: String = ""
) {
    @Transient
    val locale: Locale = Locale(lang)
}
