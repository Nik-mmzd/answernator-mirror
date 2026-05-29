# Buttons

Buttons let you attach clickable controls to an interaction's response message. The framework gives you two ways to wire them up depending on how long the buttons need to stay alive:

- **Suspending pattern** — your command's coroutine waits for clicks and reacts to them in-place. Good when the button view is tied to a single user's session — mini-games, confirmation prompts, "click to reroll" generators.
- **Stateless pattern** — the command renders the buttons and returns immediately. Clicks are dispatched by a global listener to the command's `onButtonClick` method. Good when the buttons should outlive the command coroutine — bot status panels, anything that should still respond after a restart.

The framework lives in `pw.modder.answernator4.interaction.button`. Both patterns share the same declaration style (`ButtonGroup` subclasses with `by button(...)` properties) and only differ in *how* you respond to clicks.

A note on Discord's component model: buttons render into **ActionRows** — a horizontal stack of up to five buttons. The framework supports two render targets:

- **V2** — `MessageBuilder.renderButtons(...)` sets the V2 component flag and lays the buttons out in ActionRows (wrapping to a new row every five). This is what the suspending and stateless helpers use. V2 messages **cannot** carry an `embed`.
- **Legacy** — `ActionRowBuilder.renderButtons(...)` fills a classic action row you've opened yourself. Use it when the message also needs an `embed` (Discord rejects embeds under V2). See [rendering next to an embed](#rendering-next-to-an-embed).

Top-level message text (V2 only) is rendered through `ButtonGroup.content` or `renderButtons(contentOverride = ...)`.

---

## A complete example — the suspending pattern

Picture a small "choose your action" mini-game:

```kotlin
class GameButtons : ButtonGroup(content = "Choose your action wisely!") {
    val attack by button(
        style = ButtonStyle.Danger,
        label = "Attack",
    )
    val defend by button(
        style = ButtonStyle.Primary,
        label = "Defend",
    )
    val flee by button(
        style = ButtonStyle.Secondary,
        label = "Flee",
    )
    val docs by linkButton(
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
        style = ButtonStyle.Primary,
        label = "↻ Refresh",
    )
    val ping by button(
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

    override suspend fun ButtonInteractionCreateEvent.onButtonClick(button: ButtonField, state: String?) {
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

### Carrying state through the customId

Stateless buttons can carry a per-invocation payload by appending it to the customId. Both `respondWithCommandButtons(...)` and the lower-level `renderButtons(...)` accept an optional `state: String?` parameter; when set, customIds become `"cmd:${commandName}:${buttonId}:${state}"`. The router strips the suffix and passes it to `onButtonClick`:

```kotlin
override suspend fun ButtonInteractionCreateEvent.onButtonClick(button: ButtonField, state: String?) {
    // state is the same string you passed to respondWithCommandButtons,
    // or null when the customId carried no state suffix
}
```

There's a single `onButtonClick(button, state: String?)` entry point: `state` is `null` when no suffix was rendered, non-null otherwise. Commands that don't care about state simply ignore the parameter.

Why this matters: it lets a button "remember" something about its render without any external storage. For example, a `/dice` reroll button can encode the dice expression in `state`, parse it back on click, and re-roll — no database, no in-memory map, no `waitFor` coroutine to keep alive.

The full message body is independent of `state` and is set per render via the `content` parameter (or `contentOverride` on `renderButtons`), so dynamic visible content is supported on stateless commands too:

```kotlin
override suspend fun ChatInputCommandInteractionCreateEvent.execute() {
    val expression = DiceExpression.parse(/* user input */)
    interaction.respondWithCommandButtons(
        this@Dice,
        ephemeral = false,
        state = expression.toString(),
        content = renderRolls(expression),
    )
}

override suspend fun ButtonInteractionCreateEvent.onButtonClick(button: ButtonField, state: String?) {
    val expression = DiceExpression.parse(state ?: return)
    val response = interaction.deferPublicMessageUpdate()
    response.edit {
        renderButtons(
            buttons!!,
            baseId = "cmd:${effectiveName}",
            state = state,
            contentOverride = renderRolls(expression),
        )
    }
}
```

#### The 100-character cap

Discord caps `custom_id` at 100 characters total. The prefix `"cmd:${commandName}:${buttonId}:"` eats into that, leaving the rest for `state`. There is no built-in length validation — if your `state` plus prefix exceeds 100, Discord rejects the message at render time. Commands that carry state should compute the available budget (e.g. `100 - "cmd:dice:reroll:".length`) and either truncate, reject, or fall back to a non-stateful path when input is too long. The `/dice` command rejects oversized expressions with a localized error before rendering.

#### V2 component edit semantics

A click handler that wants to update the message body cannot edit only the text-display component; Discord requires the full `components` array on every edit. That means re-rendering buttons too, with their `style`, `label`, and `emoji` reconstructed. The simplest pattern: rebuild the same `ButtonGroup` (or use the class-level one if it's static) and call `renderButtons(...)` inside the edit block — the example above shows it.

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
- `label: String?` and `emoji: DiscordPartialEmoji?` — at least one is required. `label` may be a
  **localization key**: when a `bundle` is passed to `renderButtons` (see below) each label is
  resolved against it, and a key that isn't present in the bundle falls through to the literal — so
  a plain label and a locale key are written identically.
- `disabled: Boolean` — for greying the button out (still shown).
- `visible: Boolean` — set via the `visibleIf(condition)` wrapper; a hidden button is omitted from
  the render entirely (but keeps its slot for click routing). Use it for buttons gated on optional
  config, e.g. a link button whose URL may be blank. To toggle visibility **per render** (per user
  / per click) see [toggling visibility per render](#toggling-visibility-per-render).

Construction is via the factories (chain `visibleIf` to gate visibility):

```kotlin
val source by linkButton(BuildConfig.APP_SOURCE_URL, label = "command.info.links.source")
    .visibleIf(BuildConfig.APP_SOURCE_URL.isNotBlank())
```

```kotlin
fun button(
    style: ButtonStyle = ButtonStyle.Primary,
    label: String? = null,
    emoji: DiscordPartialEmoji? = null,
    disabled: Boolean = false,
): ButtonField

fun linkButton(
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
- All buttons in a group render into ActionRows, wrapping to a new row every five buttons (Discord's per-row limit), preserving declaration order.

### `respondWithButtons` (suspending)

```kotlin
suspend fun ActionInteractionBehavior.respondWithButtons(
    group: ButtonGroup,
    ephemeral: Boolean = true,
    waitFor: Duration = 5.minutes,
    bundle: ResourceBundle? = null,
    onClick: suspend ButtonClickContext.() -> Unit = {},
)
```

Sends the message, registers a one-shot listener, and suspends until timeout or `stop()`. The listener is cleaned up in a `finally`, so a thrown exception in the handler won't leak it.

### `respondWithCommandButtons` (stateless)

```kotlin
suspend fun ActionInteractionBehavior.respondWithCommandButtons(
    command: Command,
    ephemeral: Boolean = true,
    state: String? = null,
    content: String? = null,
    bundle: ResourceBundle? = null,
    fields: List<ButtonField>? = null,
)
```

Sends the message and returns. The command's `buttons` property must be non-null; the function will throw `IllegalStateException` if it isn't.

- `state` — optional payload encoded into each button's customId; delivered back via `onButtonClick(button, state)` (non-null state). See [carrying state through the customId](#carrying-state-through-the-customid).
- `content` — optional top-level text rendered above the buttons. When `null`, the group's own `ButtonGroup.content` is used. Pass it for dynamic message bodies on stateless commands.
- `bundle` — optional locale bundle for resolving labels written as localization keys; `null` renders labels literally.
- `fields` — optional explicit list to render instead of the group's full set; pass per-render copies to toggle visibility. See [toggling visibility per render](#toggling-visibility-per-render).

Click events are routed by the global listener registered in `interactionCommandService()`. The listener:

1. Inspects `interaction.componentId`, splits on `:` with limit 4.
2. If the first part isn't `"cmd"` or there are fewer than 3 parts, it bails — that customId belongs to the suspending pattern (or to something else entirely).
3. Looks up the command by name across all three command-type maps.
4. Finds the matching `ButtonField` by id in the command's `buttons.buttons` list.
5. Invokes `command.onButtonClick(button, state)` inside a try/catch, where `state` is the 4th part of the customId if present, or `null` otherwise.

### `renderButtons` (low-level)

```kotlin
// V2: sets the components-V2 flag, wraps buttons into ActionRows of 5
fun MessageBuilder.renderButtons(group: ButtonGroup, baseId, state?, contentOverride?, bundle?)
fun MessageBuilder.renderButtons(fields: List<ButtonField>, baseId, state?, contentOverride?, bundle?)

// Legacy: fills a classic action row you've already opened (no V2 flag)
fun ActionRowBuilder.renderButtons(group: ButtonGroup, baseId, state?, bundle?)
fun ActionRowBuilder.renderButtons(fields: List<ButtonField>, baseId, state?, bundle?)
```

Each receiver has two overloads: one takes the whole `ButtonGroup`, the other an explicit `List<ButtonField>`. The `MessageBuilder` overload is the shared render path used by both `respondWithButtons` and `respondWithCommandButtons`. Call it directly inside an interaction-response edit when you need to re-render the same view after a click:

```kotlin
val response = interaction.deferEphemeralMessageUpdate()
response.edit {
    renderButtons(
        buttons!!,
        baseId = "cmd:${effectiveName}",
        state = newState,
        contentOverride = "Updated body",
    )
}
```

Both `baseId` (suspending receives this via `ButtonClickContext.baseId`) and `state` must be the same values the original render used, otherwise Discord and the framework's router won't match the clicked customId back to the right button.

### Toggling visibility per render

`visibleIf(condition)` evaluated in a `ButtonGroup` declaration is fixed at construction — fine for config-gated buttons, but a stateless command's `buttons` is a **single shared instance** across every invocation and user, so you must never mutate `visible` on it at runtime (you'd change what everyone else sees).

Instead, render an explicit list of per-render copies. Because `ButtonField` is immutable and `visibleIf` returns a copy, this leaves the shared group untouched, and routing still works (the listener matches the clicked `id` against the group's full list):

```kotlin
override suspend fun ButtonInteractionCreateEvent.onButtonClick(button: ButtonField, state: String?) {
    val expanded = button == buttons.toggle  // or decode from `state`
    interaction.deferEphemeralMessageUpdate().edit {
        renderButtons(
            listOf(
                buttons.toggle,
                buttons.detail.visibleIf(expanded),   // copy; shared group unchanged
            ),
            baseId = "cmd:$effectiveName",
            state = state,
        )
    }
}
```

For the initial render, `respondWithCommandButtons(..., fields = listOf(...))` accepts the same kind of list. Hidden buttons keep their declared slot for click routing — they just aren't drawn.

### `ButtonClickContext`

The receiver inside the suspending `onClick` lambda:

```kotlin
class ButtonClickContext(
    val event: ButtonInteractionCreateEvent,
    val button: ButtonField,
    val baseId: String,
    private val stopSignal: CompletableDeferred<Unit>,
) {
    val interaction: ButtonInteraction
    fun stop()
}
```

- `event` — the raw Kord event, in case you need it.
- `button` — which `ButtonField` was clicked. Compare against your group's declarations (`game.attack`, etc.).
- `baseId` — the customId prefix used at render time (`"${GroupClass}:${UUID}"`). Pass it to `renderButtons(...)` if you want to re-render the same view in response to the click; the rebuilt customIds will match the existing listener.
- `interaction` — the `ButtonInteraction`. Call `deferEphemeralMessageUpdate()` / `deferPublicMessageUpdate()` then `.edit { ... }` to update the message, or use `updateEphemeralMessage { ... }` / `updatePublicMessage { ... }` for a one-shot update.
- `stop()` — completes the internal signal and breaks the wait loop. Useful for "first click wins" prompts or any mini-game with an exit condition.

For the stateless pattern, the handler runs as `ButtonInteractionCreateEvent.onButtonClick(button, state)` on the command itself, without the `ButtonClickContext` wrapper. You have `this` (the event) directly, and `state` is `null` when the customId carried no payload.

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

## Rendering next to an embed

V2 messages can't carry an `embed`, so the `MessageBuilder.renderButtons` / `respondWithCommandButtons` path is off-limits when your response is an embed. Use the `ActionRowBuilder` overload instead: declare a `ButtonGroup` on the command as usual, then render it into a classic action row alongside the embed. Clicks still route through `onButtonClick` exactly the same way.

This is also the canonical place to combine **localized labels** (declare them as locale keys, pass the `bundle`) and **conditional buttons** (`visibleIf`):

```kotlin
class BotInfo : ChatInputCommand() {
    override val name = "bot_info"
    override val buttons = Buttons()

    override suspend fun ChatInputCommandInteractionCreateEvent.execute() {
        interaction.deferPublicResponse().respond {
            embed { /* ... */ }
            actionRow { renderButtons(buttons, baseId = "cmd:$effectiveName", bundle = gbundle) }
        }
    }

    override suspend fun ButtonInteractionCreateEvent.onButtonClick(button: ButtonField, state: String?) {
        if (button != buttons.refresh) return
        // re-render the embed + row inside a deferPublicMessageUpdate().edit { ... }
    }

    class Buttons : ButtonGroup() {
        val refresh by button(style = ButtonStyle.Secondary, emoji = DiscordPartialEmoji(name = "🔄"))
        val source by linkButton(BuildConfig.APP_SOURCE_URL, label = "command.info.links.source")
            .visibleIf(BuildConfig.APP_SOURCE_URL.isNotBlank())
    }
}
```

The `source` label is a locale key resolved against the passed `bundle`; the button is dropped when its URL is blank. `baseId` must be `"cmd:${effectiveName}"` so the router (which keys on `effectiveName`) matches the click back to the command. A single action row holds at most five buttons.

---

## Discord-side restrictions you'll bump into

- **V2 messages can't carry an embed.** `MessageBuilder.renderButtons` sets `MessageFlag.IsComponentsV2`; if you need an `embed`, render the buttons through `ActionRowBuilder.renderButtons` (a classic action row) instead. See [rendering next to an embed](#rendering-next-to-an-embed).
- **Components V2 flag is automatic.** `MessageBuilder.renderButtons` sets `MessageFlag.IsComponentsV2` for you. No manual flag-juggling. (The `ActionRowBuilder` overload leaves the message classic.)
- **Buttons can't go in modals.** Discord doesn't allow this — modals have a built-in Submit/Cancel. If you want a flow of `command → modal → buttons`, the modal's submit handler can call `respondWithButtons` or `respondWithCommandButtons` on its own interaction.
- **Premium buttons are intentionally rejected.** They tie into Discord's SKU/monetization system, which we don't use. Passing `ButtonStyle.Premium` to the `button(...)` factory throws at construction time.

---

## Where to look in the source

```
interaction/button/
├── ButtonField.kt       — sealed class + InteractionButtonField + LinkButtonField + factories
├── ButtonGroup.kt       — ButtonGroup abstract class + member provideDelegate/getValue
└── ButtonExtensions.kt  — respondWithButtons (suspending), respondWithCommandButtons (stateless),
                           renderButtons (MessageBuilder = V2, ActionRowBuilder = legacy), ButtonClickContext
```

The global click router lives in `interaction/InteractionCommandService.kt` — the `on<ButtonInteractionCreateEvent>` block at the bottom does the parsing and dispatch.