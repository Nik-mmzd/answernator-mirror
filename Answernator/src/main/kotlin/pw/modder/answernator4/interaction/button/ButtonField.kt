package pw.modder.answernator4.interaction.button

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji

sealed class ButtonField {
    abstract val id: String
    /**
     * Either a literal label or a localization key. At render time, [renderButtons] resolves it
     * against the supplied `ResourceBundle` (a missing key falls through to the literal), so a
     * plain label and a locale key are written the same way.
     */
    abstract val label: String?
    abstract val emoji: DiscordPartialEmoji?
    abstract val disabled: Boolean
    /** When `false`, the button is omitted from the rendered output entirely. See [visibleIf]. */
    abstract val visible: Boolean

    internal abstract fun withId(id: String): ButtonField
    internal abstract fun withVisible(visible: Boolean): ButtonField
}

internal data class InteractionButtonField(
    override val id: String = "",
    val style: ButtonStyle,
    override val label: String? = null,
    override val emoji: DiscordPartialEmoji? = null,
    override val disabled: Boolean = false,
    override val visible: Boolean = true,
) : ButtonField() {
    init {
        require(style != ButtonStyle.Link) { "Use linkButton() for link buttons" }
        require(style != ButtonStyle.Premium) { "Premium buttons are not supported" }
    }

    override fun withId(id: String) = copy(id = id)
    override fun withVisible(visible: Boolean) = copy(visible = visible)
}

internal data class LinkButtonField(
    override val id: String = "",
    val url: String,
    override val label: String? = null,
    override val emoji: DiscordPartialEmoji? = null,
    override val disabled: Boolean = false,
    override val visible: Boolean = true,
) : ButtonField() {
    override fun withId(id: String) = copy(id = id)
    override fun withVisible(visible: Boolean) = copy(visible = visible)
}

fun button(
    style: ButtonStyle = ButtonStyle.Primary,
    label: String? = null,
    emoji: DiscordPartialEmoji? = null,
    disabled: Boolean = false,
): ButtonField {
    require(label != null || emoji != null) { "Button requires either label or emoji" }
    return InteractionButtonField(
        style = style,
        label = label,
        emoji = emoji,
        disabled = disabled,
    )
}

fun linkButton(
    url: String,
    label: String? = null,
    emoji: DiscordPartialEmoji? = null,
    disabled: Boolean = false,
): ButtonField {
    require(label != null || emoji != null) { "Button requires either label or emoji" }
    return LinkButtonField(
        url = url,
        label = label,
        emoji = emoji,
        disabled = disabled,
    )
}

/**
 * Returns a copy of this button that is only rendered when [condition] is `true`. Composes with the
 * `by` declaration in a [ButtonGroup]; the button still occupies its declared slot for click
 * routing, it just isn't drawn when hidden. Handy for buttons gated on optional config (e.g. a link
 * button whose URL may be blank).
 */
fun ButtonField.visibleIf(condition: Boolean): ButtonField = withVisible(condition)
