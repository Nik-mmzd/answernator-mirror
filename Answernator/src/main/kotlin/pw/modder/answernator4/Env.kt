package pw.modder.answernator4

import dev.kord.common.entity.Snowflake

object Env {
    val BOT_TOKEN get() = System.getenv("ANSWR4_BOT_TOKEN")
        ?: throw RuntimeException("This bot requires Discord Bot Token to run. Please provide bot token using ANSWR4_BOT_TOKEN environment variable.")
    val SENTRY_DSN: String? = System.getenv("ANSWR4_SENTRY_DSN") ?: System.getenv("ANSWERNATOR_SENTRY_DSN")

    val BOT_OWNER_ID = System.getenv("ANSWR4_BOT_OWNER") ?: BuildConfig.APP_CREATOR_ID
    val BOT_OWNER_SNOWFLAKE = Snowflake(BOT_OWNER_ID)
    val BOT_INVITE_LINK = System.getenv("ANSWR4_INVITE_LINK") ?: ""

    val DISABLED_COMMANDS get() = System.getenv("ANSWR4_DISABLED_COMMANDS")?.split(';')?.filterNot { it.isBlank() } ?: emptyList()

    val COMMAND_HASH_CACHE: String = System.getenv("ANSWR4_COMMAND_HASH_CACHE") ?: "command-registry.cache"

    val HOSTNAME: String? get() = System.getenv("HOSTNAME") ?: System.getenv("COMPUTERNAME")
    val OS_NAME: String? get() = System.getProperty("os.name")
    val OS_ARCH: String? get() = System.getProperty("os.arch")
    val OS_STRING: String get() = "${OS_NAME ?: "Unknown"} ${OS_ARCH ?: "Unknown"}"

    val JRE_VENDOR: String? get() = System.getProperty("java.vendor")
    val JRE_VERSION: String? get() = System.getProperty("java.version")
}
