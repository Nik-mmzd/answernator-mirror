# Buttons

Buttons let you attach clickable controls to an interaction's response message. The framework gives you two ways to wire them up depending on how long the buttons need to stay alive:

- **Suspending pattern** — your command's coroutine waits for clicks and reacts to them in-place. Good when the button view is tied to a single user's session — mini-games, confirmation prompts, "click to reroll" generators.
- **Stateless pattern** — the command renders the buttons and returns immediately. Clicks are dispatched by a global listener to the command's `onButtonClick` method. Good when the buttons should outlive the command coroutine — bot status panels, anything that should still respond after a restart.

The framework lives in `pw.modder.answernator4.interaction.button`. Both patterns share the same declaration style (`ButtonGroup` subclasses with `by button(...)` properties) and only differ in *how* you respond to clicks.

A note on Discord's component model: this framework uses Discord's V2 component layout exclusively. That means every button lives inside its own **Section**, accompanied by a text component. The older `ActionRow` containers are deprecated in current Discord and are not supported here. The trade-off: you get an associated text snippet per button (great for context), but you can't stack buttons horizontally in a row.

---

## A complete example — the suspending pattern

Picture a small "choose your action" mini-game:

```kotlin
class GameButtons : ButtonGroup(content = "Choose your action wisely!") {
    val attack by button(
        text = "Strike with your sword",
        style = ButtonStyle.Danger,
        label = "Attack",
    )
    val defend by button(
        text = "Raise your shield",
        style = ButtonStyle.Primary,
        label = "Defend",
    )
    val flee by button(
        text = "Run away",
        style = ButtonStyle.Secondary,
        label = "Flee",
    )
    val docs by linkButton(
        text = "Need help?",
        url = "https://example.com/game-rules",
        label = "Rules",
    )
}

override suspend fun MessageCommandInteractionCreateEvent.execute() {
    val game = GameButtons()
    interaction.respondWithButtons(game, waitFor = 5.minutes) {
        when (button) {
            game.attack -> interaction.updateEphemeralMessage { content = "⚔️ You strike!" }
            game.defend -> interaction.updateEphemeralMessage { content = "🛡️ You defend!" }
            game.flee -> {
                interaction.updateEphemeralMessage { content = "🏃 You flee!" }
                stop()    // bail out early
            }
            else -> {}    // link button — no handler needed
        }
    }
}
```

The shape:

- `GameButtons` is a `ButtonGroup` subclass. Each `val` is a button declaration.
- The optional `content = ...` argument to the `ButtonGroup` constructor sets a header line above the buttons.
- Inside `execute()`, `interaction.respondWithButtons(game, waitFor = 5.minutes) { ... }` sends the message and suspends until the timeout or `stop()`.
- Inside the click handler, `button` tells you which button was clicked, and `interaction` is the *click* interaction (you can edit the original message, defer, etc.).

---

## A complete example — the stateless pattern

The same shape, but with click handling moved onto the command class:

```kotlin
class StatusButtons : ButtonGroup(content = "Bot status") {
    val refresh by button(
        text = "Click to refresh",
        style = ButtonStyle.Primary,
        label = "↻ Refresh",
    )
    val ping by button(
        text = "Latency check",
        style = ButtonStyle.Secondary,
        label = "Ping",
    )
}

class StatusCommand : ChatInputCommand() {
    override val name = "status"
    override val bundleName = "v4.status"

    override val buttons = StatusButtons()

    override suspend fun ChatInputCommandInteractionCreateEvent.execute() {
        interaction.respondWithCommandButtons(this@StatusCommand)
        // execute() returns immediately — the coroutine doesn't linger.
    }

    override suspend fun ButtonInteractionCreateEvent.onButtonClick(button: ButtonField) {
        when (button) {
            buttons.refresh -> interaction.updateEphemeralMessage {
                content = "Status: OK at ${Clock.System.now()}"
            }
            buttons.ping -> interaction.updateEphemeralMessage {
                content = "Pong: ${kord.gateway.averagePing}"
            }
        }
    }
}
```

What's different:

- `buttons` is declared as a property on the command itself, holding a concrete `ButtonGroup` instance.
- `execute()` calls `respondWithCommandButtons(this)` and returns. No suspending, no listener tied to the call.
- `onButtonClick` is overridden on the command — it's the entry point that the global listener (in `interactionCommandService`) routes clicks to.

Crucially, the customId format here is `"cmd:${commandName}:${buttonId}"` — completely deterministic. That means buttons survive bot restarts: a user who clicks the refresh button an hour after the message was sent will still get a response, as long as the command is still registered.

---

## Choosing between the two patterns

| | Suspending | Stateless |
|---|---|---|
| Where the click handler lives | Inside the call to `respondWithButtons` | `onButtonClick` on the command class |
| customId format | `"${GroupClass}:${UUID}:${buttonId}"` (unique per call) | `"cmd:${commandName}:${buttonId}"` (stable) |
| Survives bot restart | No | Yes |
| Can close captured state | Yes — local variables in `execute()` | No — the handler is shared across all invocations |
| Best for | Mini-games, per-user sessions, anything with private state | Refresh buttons, status panels, anything shared |
| Cleanup | Automatic on timeout/`stop()` | None — listener lives in the service |

When in doubt: start with the suspending pattern. Move to the stateless one when you realize you want the buttons to outlive the original interaction.

The two patterns don't interfere — both listeners receive every button event but filter by their `customId` shape. You can use both in the same command if you want.

---

## The pieces

### `ButtonGroup`

The abstract base:

```kotlin
abstract class ButtonGroup(val content: String? = null) {
    val buttons: List<ButtonField>
}
```

`content` is the top-level message text shown above the buttons. Optional.

You subclass it and declare buttons as `by` properties. The framework uses the property name as the button's id (the `buttonId` half of the customId).

### `ButtonField`

Sealed type with two concrete forms:

- **`InteractionButtonField`** — has a `style` (Primary/Secondary/Success/Danger) and triggers click events when pressed.
- **`LinkButtonField`** — has a `url` and just opens it; doesn't fire interaction events.

Both expose:

- `id: String` — derived from the property name.
- `text: String` — the text component shown next to the button inside the Section.
- `label: String?` and `emoji: DiscordPartialEmoji?` — at least one is required.
- `disabled: Boolean` — for greying the button out.

Construction is via the factories:

```kotlin
fun button(
    text: String,
    style: ButtonStyle = ButtonStyle.Primary,
    label: String? = null,
    emoji: DiscordPartialEmoji? = null,
    disabled: Boolean = false,
): ButtonField

fun linkButton(
    text: String,
    url: String,
    label: String? = null,
    emoji: DiscordPartialEmoji? = null,
    disabled: Boolean = false,
): ButtonField
```

Notes:

- `ButtonStyle.Link` is rejected by `button(...)` — use `linkButton(...)` for those.
- `ButtonStyle.Premium` is unsupported by this framework. Premium buttons are tied to Discord's subscription/SKU system; we have no use for them.
- The framework requires either `label` or `emoji` to be non-null. A button with neither is invisible to the user.

### `respondWithButtons` (suspending)

```kotlin
suspend fun ActionInteractionBehavior.respondWithButtons(
    group: ButtonGroup,
    ephemeral: Boolean = true,
    waitFor: Duration = 5.minutes,
    onClick: suspend ButtonClickContext.() -> Unit = {},
)
```

Sends the message, registers a one-shot listener, and suspends until timeout or `stop()`. The listener is cleaned up in a `finally`, so a thrown exception in the handler won't leak it.

### `respondWithCommandButtons` (stateless)

```kotlin
suspend fun ActionInteractionBehavior.respondWithCommandButtons(
    command: Command,
    ephemeral: Boolean = true,
)
```

Sends the message and returns. The command's `buttons` property must be non-null; the function will throw `IllegalStateException` if it isn't.

Click events are routed by the global listener registered in `interactionCommandService()`. The listener:

1. Inspects `interaction.componentId`, splits on `:` with limit 3.
2. If the first part isn't `"cmd"`, it bails — that customId belongs to the suspending pattern (or to something else entirely).
3. Looks up the command by name across all three command-type maps.
4. Finds the matching `ButtonField` by id in the command's `buttons.buttons` list.
5. Invokes `command.onButtonClick(button)` in a try/catch.

### `ButtonClickContext`

The receiver inside the suspending `onClick` lambda:

```kotlin
class ButtonClickContext(
    val event: ButtonInteractionCreateEvent,
    val button: ButtonField,
    private val stopSignal: CompletableDeferred<Unit>,
) {
    val interaction: ButtonInteraction
    fun stop()
}
```

- `event` — the raw Kord event, in case you need it.
- `button` — which `ButtonField` was clicked. Compare against your group's declarations (`game.attack`, etc.).
- `interaction` — the `ButtonInteraction`. Call `updateEphemeralMessage { }`, `updatePublicMessage { }`, `deferEphemeralMessageUpdate()`, etc. on it.
- `stop()` — completes the internal signal and breaks the wait loop. Useful for "first click wins" prompts or any mini-game with an exit condition.

For the stateless pattern, the handler runs as `ButtonInteractionCreateEvent.onButtonClick(button)` on the command itself, without the `ButtonClickContext` wrapper. You have `this` (the event) directly.

---

## Responding to a click

A button click is a *new* Discord interaction. You have ~3 seconds to acknowledge it before Discord shows "interaction failed" to the user. The two most common options:

```kotlin
// Edit the original message and acknowledge the click in one step.
interaction.updateEphemeralMessage { content = "..." }

// Acknowledge first, do work, then edit later.
val response = interaction.deferEphemeralMessageUpdate()
// ... long-running work ...
response.edit { content = "..." }
```

`updateEphemeralMessage` / `updatePublicMessage` should match the ephemerality of the original message. If you ran `respondWithButtons(group, ephemeral = true, ...)`, use the ephemeral update on clicks. If you used `ephemeral = false`, use the public one.

---

## Discord-side restrictions you'll bump into

- **`text` is mandatory per button.** Sections in V2 require a text component alongside the accessory. If your UX wants buttons with no associated text, this framework can't express it — you'd need to drop down to the deprecated ActionRow, which isn't supported here.
- **Components V2 flag is automatic.** The framework sets `MessageFlag.IsComponentsV2` for you when rendering. No manual flag-juggling.
- **Buttons can't go in modals.** Discord doesn't allow this — modals have a built-in Submit/Cancel. If you want a flow of `command → modal → buttons`, the modal's submit handler can call `respondWithButtons` or `respondWithCommandButtons` on its own interaction.
- **Premium buttons are intentionally rejected.** They tie into Discord's SKU/monetization system, which we don't use. Passing `ButtonStyle.Premium` to the `button(...)` factory throws at construction time.

---

## Where to look in the source

```
interaction/button/
├── ButtonField.kt       — sealed class + InteractionButtonField + LinkButtonField + factories
├── ButtonGroup.kt       — ButtonGroup abstract class + provideDelegate/getValue
└── ButtonExtensions.kt  — respondWithButtons (suspending), respondWithCommandButtons (stateless), ButtonClickContext
```

The global click router lives in `interaction/InteractionCommandService.kt` — the `on<ButtonInteractionCreateEvent>` block at the bottom does the parsing and dispatch.