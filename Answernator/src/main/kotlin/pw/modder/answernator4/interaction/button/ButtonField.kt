package pw.modder.answernator4.interaction.button

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji

sealed class ButtonField {
    abstract val id: String
    /**
     * Context text shown next to the button as a V2 Section's text component. When `null`, the
     * button is rendered inside a V2 ActionRow instead (horizontal stack, no associated text).
     */
    abstract val text: String?
    abstract val label: String?
    abstract val emoji: DiscordPartialEmoji?
    abstract val disabled: Boolean

    internal abstract fun withId(id: String): ButtonField
}

internal data class InteractionButtonField(
    override val id: String = "",
    override val text: String? = null,
    val style: ButtonStyle,
    override val label: String? = null,
    override val emoji: DiscordPartialEmoji? = null,
    override val disabled: Boolean = false,
) : ButtonField() {
    init {
        require(style != ButtonStyle.Link) { "Use linkButton() for link buttons" }
        require(style != ButtonStyle.Premium) { "Premium buttons are not supported" }
    }

    override fun withId(id: String) = copy(id = id)
}

internal data class LinkButtonField(
    override val id: String = "",
    override val text: String? = null,
    val url: String,
    override val label: String? = null,
    override val emoji: DiscordPartialEmoji? = null,
    override val disabled: Boolean = false,
) : ButtonField() {
    override fun withId(id: String) = copy(id = id)
}

fun button(
    style: ButtonStyle = ButtonStyle.Primary,
    label: String? = null,
    emoji: DiscordPartialEmoji? = null,
    text: String? = null,
    disabled: Boolean = false,
): ButtonField {
    require(label != null || emoji != null) { "Button requires either label or emoji" }
    return InteractionButtonField(
        text = text,
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
    text: String? = null,
    disabled: Boolean = false,
): ButtonField {
    require(label != null || emoji != null) { "Button requires either label or emoji" }
    return LinkButtonField(
        text = text,
        url = url,
        label = label,
        emoji = emoji,
        disabled = disabled,
    )
}