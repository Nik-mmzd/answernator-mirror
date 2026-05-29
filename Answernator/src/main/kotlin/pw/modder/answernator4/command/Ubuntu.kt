package pw.modder.answernator4.command

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.DiscordPartialEmoji
import dev.kord.core.behavior.interaction.response.edit
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import pw.modder.answernator4.interaction.ChatInputCommand
import pw.modder.answernator4.interaction.button.ButtonField
import pw.modder.answernator4.interaction.button.ButtonGroup
import pw.modder.answernator4.interaction.button.button
import pw.modder.answernator4.interaction.button.renderButtons
import pw.modder.answernator4.interaction.button.respondWithCommandButtons

class Ubuntu : ChatInputCommand() {
    override val name = "ubuntu"
    override val bundleName = "v4.ubuntu"
    override val buttons = Buttons()

    private val data = javaClass.classLoader.getResourceAsStream("pw/modder/answernator4/ubuntu.json")!!.reader().use {
        Json.decodeFromString(ListSerializer(UbuntuWord.serializer()), it.readText())
    }

    private fun randomReply() = data.random().let { "${it.first.random()} ${it.second.random()}" }

    override suspend fun ChatInputCommandInteractionCreateEvent.execute() {
        interaction.respondWithCommandButtons(
            this@Ubuntu,
            ephemeral = false,
            content = randomReply(),
        )
    }

    override suspend fun ButtonInteractionCreateEvent.onButtonClick(button: ButtonField, state: String?) {
        if (button != buttons.regen) return
        interaction.deferPublicMessageUpdate().edit {
            renderButtons(
                this@Ubuntu.buttons,
                baseId = "cmd:${this@Ubuntu.effectiveName}",
                contentOverride = randomReply(),
            )
        }
    }

    class Buttons : ButtonGroup() {
        val regen by button(
            style = ButtonStyle.Secondary,
            emoji = DiscordPartialEmoji(name = "🐧"),
        )
    }

    @Serializable
    private data class UbuntuWord(val first: List<String>, val second: List<String>)
}
