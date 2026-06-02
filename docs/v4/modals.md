# Modals

A modal is a popup form that Discord shows on top of the client. The user fills it in, hits submit, and you get the typed values back — all in a single suspending call from your command. Use modals when you need structured input that doesn't fit cleanly into slash command options: long-form text, multi-step choices, free-form reasons, and so on.

The framework lives in `pw.modder.answernator4.interaction.modal`. It's a thin typed wrapper around Kord's modal API, with the same "declare a class, read by reference" feel as the rest of the v4 system.

---

## A complete example

Before we get into the API surface, here's a non-trivial modal — a ban dialog with a free-text reason, a severity radio, and a few toggle options:

```kotlin
import pw.modder.answernator4.interaction.modal.*
import dev.kord.common.entity.TextInputStyle
import kotlin.time.Duration.Companion.minutes

class BanModal : Modal(title = "Ban member") {
    val intro = textDisplay("Fill in the ban details below.")

    val reason by textField(
        label = "Reason",
        style = TextInputStyle.Paragraph,
        allowedLength = 1..512,
    ).optional()

    val severity by radioGroup(
        label = "Severity",
        description = "How severe is the violation?",
        choices = listOf(
            SelectChoice("Warning",   "warn"),
            SelectChoice("Temp ban",  "temp"),
            SelectChoice("Permanent", "perm", default = true),
        ),
    )

    val notify by checkbox("Send DM notification", default = true)
}

// Inside a command:
override suspend fun MessageCommandInteractionCreateEvent.execute() {
    val modal = BanModal()
    val reply = interaction.showModal(modal, waitFor = 5.minutes) ?: return

    val reason:   String?  = reply[modal.reason]
    val severity: String   = reply[modal.severity]
    val notify:   Boolean  = reply[modal.notify]

    reply.interaction.deferEphemeralResponse().respond {
        content = "Will ban with severity=$severity (reason: ${reason ?: "—"}), notify=$notify"
    }
}
```

A few things to call out from this example:

- The modal is a class. You instantiate it once per invocation (`val modal = BanModal()`).
- Fields are declared as `val`s with `by` delegation. The framework uses the property name as the field's `customId`.
- A modal can contain *static text* (via `textDisplay(...)`) interleaved between fields. Useful for instructions and section dividers.
- `showModal(...)` suspends until the user submits, then returns a `ModalReply`. If the timeout elapses or the user closes the modal, it returns `null`.
- After submission, you respond to `reply.interaction` — that's a *different* Discord interaction from the original one. The original one was "consumed" by sending the modal.

---

## Lifecycle: what `showModal` actually does

When you call `interaction.showModal(form, waitFor = ...)`:

1. The framework generates a unique `customId` of the form `"BanModal:<uuid>"`. The UUID guarantees that this specific render is uniquely identified, even if multiple users open the same modal class at the same time.
2. It sends the modal as the response to the original interaction. This consumes the original interaction's response slot — you can't `respond { }` to it afterwards.
3. It registers a temporary `ModalSubmitInteractionCreateEvent` listener that matches by that UUID-suffixed `customId`.
4. The function suspends until either the listener fires (user hit submit) or `waitFor` elapses.
5. The listener is cancelled in a `finally`, so even if your code throws, the listener gets cleaned up.

The implication: a `null` return means "user didn't submit in time" or "user pressed escape". Treat it the same way you treat a cancelled action.

---

## The pieces

### `Modal`

The abstract base. Subclass it per form:

```kotlin
class MyModal : Modal(title = "My form") {
    val name by textField("Name")
}
```

The property name becomes the field's `customId`. If you ever need to override this (you almost certainly don't), pass `id = ...` explicitly to the factory.

### Elements vs. fields

A `Modal` tracks an ordered `elements: List<ModalElement>` — every element in declaration order. There are two kinds:

- **`ModalField<T>`** — an input. Has an `id` (used as customId), produces a typed value `T`. Registered via `by` delegation.
- **`TextDisplay`** — static display-only text. Registered by calling `textDisplay("...")` inside the class.

`fields: List<ModalField<*>>` is a convenience view filtered to just the inputs. The render code in `ModalExtensions.kt` walks `elements` so order is preserved.

### `showModal`

```kotlin
suspend fun ModalParentInteractionBehavior.showModal(
    form: Modal,
    waitFor: Duration = 5.minutes,
): ModalReply?
```

The receiver type is `ModalParentInteractionBehavior` — any Kord interaction that can spawn a modal. That includes interactions from slash, user, and message commands, as well as button and select-menu submits. Effectively: anything except a modal submit itself (you can't open a modal from inside a modal submit).

### `ModalReply`

```kotlin
class ModalReply(val interaction: ModalSubmitInteraction) {
    operator fun <T> get(field: ModalField<T>): T
}
```

Index the reply with a field reference to get the typed value. The wrapped `interaction` is the submit-side interaction — call `deferEphemeralResponse()`, `respondPublic { }`, etc. on it to actually respond to the user.

---

## Field reference

Every field factory accepts a common `description: String? = null` argument that maps to Discord's optional "helper text under the label" feature.

### `textField` — text input

```kotlin
val reason by textField(
    label = "Reason",
    description = null,
    style = TextInputStyle.Short,       // or Paragraph for multiline
    placeholder = null,
    allowedLength = 1..500,             // optional ClosedRange<Int>
    defaultValue = null,
)
```

Returns `ModalField<String>`. Apply `.optional()` for `ModalField<String?>`.

### `stringSelect` — dropdown with custom choices

```kotlin
val color by stringSelect(
    label = "Pick a color",
    choices = listOf(
        SelectChoice("Red",   "red"),
        SelectChoice("Green", "green", description = "Like a tree"),
        SelectChoice("Blue",  "blue",  default = true),
    ),
    placeholder = null,
)
```

`SelectChoice` carries the user-visible label, the dev-defined `value` (what comes back in the reply), and optional `description` and `default`.

Returns `ModalField<String>`. `.multiple(allowedValues)` switches to multi-select, returning `ModalField<List<String>>`.

### Entity selects

```kotlin
val target by userSelect(label = "Target user")
val role   by roleSelect(label = "Role")
val ping   by mentionableSelect(label = "Who to ping")
val ch     by channelSelect(
    label = "Channel",
    channelTypes = listOf(ChannelType.GuildText, ChannelType.GuildVoice),
)
```

All return `ModalField<Snowflake>` (the ID of the picked entity). `.multiple()` for multi-pick → `ModalField<List<Snowflake>>`. The non-`mentionable` variants also accept `defaultUsers` / `defaultRoles` / `defaultChannels` for pre-selection.

### `radioGroup` — pick one

```kotlin
val severity by radioGroup(
    label = "Severity",
    choices = listOf(
        SelectChoice("Low",  "low"),
        SelectChoice("High", "high", default = true),
    ),
)
```

Returns `ModalField<String>` — the chosen choice's `value`.

### `checkboxGroup` — pick zero or more

```kotlin
val flags by checkboxGroup(
    label = "Flags",
    choices = listOf(
        SelectChoice("Delete messages", "del"),
        SelectChoice("Notify log",      "log"),
    ),
    allowedValues = 0..2,    // optional min/max
)
```

Returns `ModalField<List<String>>` — the `value`s of the checked choices.

### `checkbox` — single boolean

```kotlin
val notify by checkbox("Send DM notification", default = true)
```

Returns `ModalField<Boolean>`. A single checkbox always has a value, so `.optional()` doesn't make sense here.

### `textDisplay` — non-input static text

```kotlin
class MyModal : Modal("Title") {
    val header = textDisplay("Section header")
    val field1 by textField("Field 1")
    val sep    = textDisplay("More options below:")
    val field2 by textField("Field 2")
}
```

Not a field — purely visual. The convention is to assign it to a `val` so its position in the class body fixes the rendering order. Returns `TextDisplay` (a `ModalElement`, not a `ModalField`).

---

## Wrappers

The framework keeps the factory surface small by using a **wrapper pattern** for variants. Two wrappers ship today:

### `.optional()`

```kotlin
fun <T : Any> ModalField<T>.optional(): ModalField<T?>
```

Applies to: text inputs, all selects, radio groups, checkbox groups.

At build time it disables the "required" constraint appropriate to the wrapped component:

| Wrapped component | What changes |
|-------------------|--------------|
| Text input | `required = false` |
| Select menu | `allowedValues = 0..maxValues` |
| Radio group | `required = false` |
| Checkbox group | `required = false`, `minValues = 0` |

At read time it returns `null` whenever the field has no submitted value.

### `.multiple(allowedValues = 1..25)`

```kotlin
fun <T> ModalField<T>.multiple(allowedValues: IntRange = 1..25): ModalField<List<T>>
```

Only valid on selects. Calling it on anything else throws at runtime.

Composes with `.optional()`:

```kotlin
val tags by stringSelect("Tags", choices).multiple(0..5).optional()
// ModalField<List<String>?>
```

---

## Localization

Modals are sent on demand (not registered globally with Discord at startup), so the locale plumbing is simpler than for slash commands: you just resolve all strings up front and pass them to the modal's constructor.

The recommended pattern is to take a `ResourceBundle` in your modal's constructor:

```kotlin
class BanModal(bundle: ResourceBundle) : Modal(title = bundle.l("modal.ban.title")) {
    val reason by textField(
        label = bundle.l("modal.ban.reason_label"),
        placeholder = bundle.l("modal.ban.reason_placeholder"),
    ).optional()
}

// inside a command's execute():
val modal = BanModal(bundle)
val reply = interaction.showModal(modal) ?: return
```

This sidesteps the registration-time/runtime split that slash commands have to deal with.

---

## What's not supported

Discord and Kord put a few constraints on modals:

| Feature | Why it's not supported |
|---------|------------------------|
| Buttons inside modals | Discord doesn't allow them. Modals have a built-in Submit and Cancel — no custom buttons. |
| File uploads | Skipped on purpose; we don't need them yet. The underlying Kord builder supports them, so this is easy to add when needed. |
| Containers / sections | Modals are a flat list of labels and text displays. Sections are messages-only. |

If you need to associate visually grouped fields, use `textDisplay(...)` as a divider — that's the only structural tool the modal API gives us.

---

## Where to look in the source

```
interaction/modal/
├── Modal.kt           — the Modal class, delegate operators, ModalReply
├── ModalField.kt      — ModalElement, ModalField/Impl, WrapperField, OptionalField, TextInputField + textField()/optional()
├── SelectField.kt     — SelectChoice, SelectFieldImpl, all select fields, MultipleField + factories + multiple()
├── ChoiceField.kt     — RadioGroupField, CheckboxGroupField, CheckboxField + factories
└── ModalExtensions.kt — ModalParentInteractionBehavior.showModal(form, waitFor)
```

A few internals worth knowing if you're extending this:

- `ModalField<T>` is sealed and implements `ModalElement`. Each concrete impl extends the internal `ModalFieldImpl<T>` which adds methods the framework uses (`buildIn`, `hasValue`, `extractValue`).
- `WrapperField<R, T>` is the analog of `WrapperOption` for the chainable transformations. `OptionalField` and `MultipleField` extend it.
- The `provideDelegate` operator on `ModalField` captures the property name and stamps it onto the field's `id` via `withId(...)`. Same trick the option system uses in `Interaction.kt`.
- Modal customIds are `"${modal::class.simpleName}:${UUID.randomUUID()}"` to guarantee uniqueness across concurrent invocations.
