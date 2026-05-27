package pw.modder.answernator4.interaction

import dev.kord.common.Locale
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.InteractionCreateEvent
import pw.modder.answernator.utils.locale.UTF8Control
import pw.modder.answernator4.interaction.button.ButtonField
import pw.modder.answernator4.interaction.button.ButtonGroup
import java.util.ResourceBundle

internal val SUPPORTED_LOCALES = mapOf(
    Locale("en-US") to java.util.Locale.ROOT,
    Locale("ru") to java.util.Locale("ru")
)

internal fun getAllLocalizations(bundleName: String, key: String): Pair<String, Map<Locale, String>> {
    val translations = mutableMapOf<Locale, String>()
    var name = key

    SUPPORTED_LOCALES.forEach { (kordLocale, javaLocale) ->
        try {
            val bundle = ResourceBundle.getBundle("locale.$bundleName", javaLocale, UTF8Control)
            if (bundle.containsKey(key)) {
                val value = bundle.getString(key)
                translations[kordLocale] = value
                if (javaLocale == java.util.Locale.ROOT) {
                    name = value
                }
            }
        } catch (_: Exception) {}
    }
    return name to translations
}

internal fun ResourceBundle.l(key: String): String = if (containsKey(key)) getString(key) else key

abstract class Command {
    abstract val name: String
    abstract val bundleName: String
    open val defaultMemberPermissions: Permissions? = null
    open val dmPermission: Boolean? = null

    /**
     * If non-empty, the command is registered per-guild for each ID instead of globally.
     * `dmPermission` is ignored for guild-scoped commands (Discord doesn't allow it there).
     */
    open val guildIds: List<Snowflake> = emptyList()

    /**
     * The command name as Discord stores it: the ROOT-locale value from [bundleName] for the [name] key,
     * or [name] itself if the bundle has no such key. This is what comes back in `invokedCommandName`
     * / `command.rootName`, so it's the right thing to key command-lookup maps by.
     */
    val effectiveName: String by lazy { getAllLocalizations(bundleName, name).first }

    /**
     * Optional. When set, the command's buttons can be rendered with `interaction.respondWithCommandButtons(this)`,
     * and clicks are routed to [onButtonClick] by the global listener in [interactionCommandService].
     */
    open val buttons: ButtonGroup? = null

    /**
     * Called by [interactionCommandService] when a button declared in [buttons] is clicked.
     * Default implementation is a no-op.
     */
    open suspend fun ButtonInteractionCreateEvent.onButtonClick(button: ButtonField) {}

    val InteractionCreateEvent.bundle: ResourceBundle
        get() {
            val discordLocale = interaction.locale ?: Locale("en-US")
            val javaLocale = SUPPORTED_LOCALES[discordLocale] ?: java.util.Locale.ROOT
            return ResourceBundle.getBundle("locale.$bundleName", javaLocale, UTF8Control)
        }

    abstract suspend fun register(kord: Kord)
}
