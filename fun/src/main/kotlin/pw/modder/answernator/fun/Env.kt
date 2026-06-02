package pw.modder.answernator.`fun`

import dev.kord.common.entity.Snowflake

object Env {
    private val defaultAllowedGuilds = listOf(
        Snowflake(177087343511994368), // ll
        Snowflake(227546337061765120) // test
    )
    val QuotesEnabledGuilds = System.getenv("ANSWR4_QUOTES_GUILDS")
        ?.split(';', ' ', ',')?.filter(String::isNotBlank)
        ?.map { Snowflake(it) }
        ?: defaultAllowedGuilds
    val TsarEnabledGuilds = System.getenv("ANSWR4_TSAR_GUILDS")
        ?.split(';', ' ', ',')?.filter(String::isNotBlank)
        ?.map { Snowflake(it) }
        ?: defaultAllowedGuilds
}
