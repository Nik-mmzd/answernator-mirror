package pw.modder.answernator4.command

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.common.entity.MessageFlag
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.respondPublic
import dev.kord.core.behavior.interaction.response.edit
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import pw.modder.answernator4.dice.DiceException
import pw.modder.answernator4.dice.DiceExpression
import pw.modder.answernator4.interaction.ChatInputCommand
import pw.modder.answernator4.interaction.Option
import pw.modder.answernator4.interaction.boolean
import pw.modder.answernator4.interaction.button.ButtonField
import pw.modder.answernator4.interaction.button.ButtonGroup
import pw.modder.answernator4.interaction.button.button
import pw.modder.answernator4.interaction.button.renderButtons
import pw.modder.answernator4.interaction.button.respondWithCommandButtons
import pw.modder.answernator4.interaction.default
import pw.modder.answernator4.interaction.description
import pw.modder.answernator4.interaction.l
import pw.modder.answernator4.interaction.maxLength
import pw.modder.answernator4.interaction.name
import pw.modder.answernator4.interaction.string
import java.util.ResourceBundle

private const val CUSTOM_ID_PREFIX_LEN = "cmd:dice:reroll:".length
private const val MAX_STATE_LEN = 100 - CUSTOM_ID_PREFIX_LEN

class Dice : ChatInputCommand() {
    override val name = "dice"
    override val bundleName = "v4.dice"
    override val buttons = Buttons()

    val dices: Option<String> by string().name("dice.dices").description("dice.dices.description").maxLength(80).default("1d6")
    val public: Option<Boolean> by boolean().name("dice.public").description("dice.public.description").default(false)
    val help: Option<Boolean> by boolean().name("dice.help").description("dice.help.description").default(false)

    override suspend fun ChatInputCommandInteractionCreateEvent.execute() {
        val dices by option(dices)
        val isPublic by option(public)
        val isHelp by option(help)
        val texts = if (isPublic) gbundle else bundle

        if (isHelp) {
            val helpText = texts.l("command.dice.help")
            if (isPublic) {
                interaction.respondPublic { content = helpText }
            } else {
                interaction.respondEphemeral { content = helpText }
            }
            return
        }

        val expression = try {
            DiceExpression.parse(dices)
        } catch (e: DiceException) {
            interaction.respondEphemeral {
                content = texts.l("command.dice.error").format(e.message ?: dices)
            }
            return
        }

        val state = expression.toString()
        if (state.length > MAX_STATE_LEN) {
            interaction.respondEphemeral {
                content = texts.l("command.dice.too_long").format(MAX_STATE_LEN, state.length)
            }
            return
        }

        interaction.respondWithCommandButtons(
            this@Dice,
            ephemeral = !isPublic,
            state = state,
            content = renderRolls(expression, texts),
        )
    }

    override suspend fun ButtonInteractionCreateEvent.onButtonClick(button: ButtonField, state: String?) {
        if (button != buttons.reroll) return
        val expression = try {
            DiceExpression.parse(state ?: return)
        } catch (_: DiceException) {
            return
        }
        val isEphemeral = interaction.message.flags?.contains(MessageFlag.Ephemeral) == true
        val bundle = if (isEphemeral) bundle else gbundle
        val response = if (isEphemeral) {
            interaction.deferEphemeralMessageUpdate()
        } else {
            interaction.deferPublicMessageUpdate()
        }
        response.edit {
            renderButtons(
                this@Dice.buttons,
                baseId = "cmd:${this@Dice.effectiveName}",
                state = state,
                contentOverride = renderRolls(expression, bundle),
            )
        }
    }

    private fun renderRolls(expression: DiceExpression, bundle: ResourceBundle): String = buildString {
        val results = expression.roll()
        val rollingTemplate = bundle.l("command.dice.rolling")
        expression.sets.forEachIndexed { setIndex, set ->
            if (setIndex > 0) appendLine()
            append(rollingTemplate.format(set)).appendLine()
            results[setIndex].forEach { tryResult ->
                tryResult.values.joinTo(this, separator = " ")
                append(" (**").append(tryResult.total).append("**)").appendLine()
            }
        }
    }.trimEnd()

    class Buttons : ButtonGroup() {
        val reroll by button(
            style = ButtonStyle.Secondary,
            emoji = DiscordPartialEmoji(name = "🎲"),
        )
    }
}
