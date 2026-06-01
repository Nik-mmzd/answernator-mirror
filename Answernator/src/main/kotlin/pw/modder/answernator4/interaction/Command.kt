package pw.modder.answernator4.interaction

import dev.kord.common.Locale
import dev.kord.common.asJavaLocale
import dev.kord.common.entity.ApplicationCommandType
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.InteractionCreateEvent
import org.kodein.di.DI
import org.kodein.di.DIAware
import pw.modder.answernator4.interaction.button.ButtonField
import pw.modder.answernator4.interaction.button.ButtonGroup
import java.util.ResourceBundle

private val DEFAULT_LOCALE = Locale("en-US")

internal val SUPPORTED_LOCALES: Set<Locale> = linkedSetOf(DEFAULT_LOCALE, Locale("ru"))

/**
 * Classloader used to resolve command resource bundles.
 *
 * Defaults to this framework's own loader. At startup [pw.modder.answernator4.di.KodeinModuleList]
 * swaps in the loader spanning the hot-swappable plugin JARs in `./commands`; since that loader
 * delegates to the app loader as its parent, it resolves both the core bundles under `locale/v4`
 * and plugin-module bundles (e.g. `fun`'s bundles under `locale/fun`), which the app loader alone
 * cannot see.
 */
internal object BundleClassLoader {
    @Volatile
    var value: ClassLoader = BundleClassLoader::class.java.classLoader
}

internal fun getAllLocalizations(bundleName: String, key: String): Pair<String, Map<Locale, String>> {
    val translations = mutableMapOf<Locale, String>()
    var name = key

    SUPPORTED_LOCALES.forEach { kordLocale ->
        try {
            val bundle = ResourceBundle.getBundle("locale.$bundleName", kordLocale.asJavaLocale(), BundleClassLoader.value)
            if (bundle.containsKey(key)) {
                val value = bundle.getString(key)
                translations[kordLocale] = value
                if (kordLocale == DEFAULT_LOCALE) name = value
            }
        } catch (_: Exception) {}
    }
    return name to translations
}

internal fun ResourceBundle.l(key: String): String = if (containsKey(key)) getString(key) else key

abstract class Command(override val di: DI) : DIAware {
    abstract val name: String
    abstract val bundleName: String
    abstract val discordType: ApplicationCommandType
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
     *
     * [state] is the payload encoded into the customId at render time (via [respondWithCommandButtons]'s
     * or [renderButtons]'s `state` parameter), or `null` when the customId carries no state suffix.
     *
     * Default implementation is a no-op.
     */
    open suspend fun ButtonInteractionCreateEvent.onButtonClick(button: ButtonField, state: String?) {}

    /**
     * Provides localization bundle for **member** locale
     */
    val InteractionCreateEvent.bundle: ResourceBundle
        get() {
            val discordLocale = interaction.locale ?: DEFAULT_LOCALE
            return ResourceBundle.getBundle("locale.$bundleName", discordLocale.asJavaLocale(), BundleClassLoader.value)
        }

    /**
     * Provides localization bundle for **guild** locale
     */
    val InteractionCreateEvent.gbundle: ResourceBundle
        get() {
            val discordLocale = interaction.guildLocale ?: interaction.locale ?: DEFAULT_LOCALE
            return ResourceBundle.getBundle("locale.$bundleName", discordLocale.asJavaLocale(), BundleClassLoader.value)
        }


    abstract suspend fun register(kord: Kord)
}
