# Commands

A command is a class. You pick the right base class for the kind of Discord interaction you want to expose, override `name`/`bundleName`, declare any options as `val`s, and put your logic inside `execute()`. The framework handles registration, locale lookup, and event dispatch.

This document walks through all three command types, the option system that slash commands use, localization, and how to attach buttons to a command. For modals (which are a separate input mechanism, not a kind of command), see [`modals.md`](modals.md).

---

## Three kinds of command

Discord has three "application command" types and we have one base class per type:

| Base class | Discord type | Invocation |
|------------|--------------|------------|
| `ChatInputCommand` | Slash command | `/yourname` in the chat box |
| `UserCommand` | User context menu | Right-click a user → Apps |
| `MessageCommand` | Message context menu | Right-click a message → Apps |

All three inherit from `Command`, which carries the bits common to every command:

```kotlin
abstract class Command {
    abstract val name: String
    abstract val bundleName: String
    open val defaultMemberPermissions: Permissions? = null
    open val dmPermission: Boolean? = null
    open val guildIds: List<Snowflake> = emptyList()

    // Optional button support — see buttons.md
    open val buttons: ButtonGroup? = null
    open suspend fun ButtonInteractionCreateEvent.onButtonClick(button: ButtonField) {}

    val InteractionCreateEvent.bundle: ResourceBundle
        get() = /* resolve based on interaction.locale */
}
```

A few notes on the common bits:

- **`name`** is what Discord sees and the user types. It also doubles as the lookup key in the global button handler (see [`buttons.md`](buttons.md)), so keep it unique across commands.
- **`bundleName`** points at a resource bundle under `src/main/resources/locale/`. For a command with `bundleName = "v4.ban_author"`, the framework loads `locale/v4/ban_author.properties` for `en-US` and `locale/v4/ban_author_ru.properties` for `ru`.
- **`defaultMemberPermissions`** maps to Discord's "this command is hidden unless the member has these permissions" feature. Set it to `Permissions(Permission.BanMembers)` for a ban command, etc.
- **`dmPermission = false`** disables the command in DMs. Most moderation commands want this. Note: ignored for guild-scoped commands (Discord doesn't allow that flag there).
- **`guildIds`** controls scope. By default it's empty — the command is registered globally and propagates to every guild the bot is in (with up to an hour of Discord-side cache lag). Override with a non-empty list of `Snowflake`s and the command is registered per-guild instead, appearing instantly in those guilds only. Useful for testing in a dev server, or for commands that only make sense in specific communities.

The `bundle` extension property is the bridge between an incoming interaction and the localized strings: inside `execute()`, `bundle.l("some.key")` gives you the user's locale-appropriate text. We'll see this used everywhere below.

### Discord-side limits

Discord enforces a few hard limits on application commands. The framework doesn't validate these for you — they show up as 400-level errors at registration time — so it's worth keeping them in mind when picking names and writing copy:

| Limit | Value | Where it applies |
|-------|-------|------------------|
| Command name length | 1..32 characters | All three command types |
| Description length | 1..100 characters | `ChatInputCommand` only |
| Description for context-menu commands | must be empty | `UserCommand`, `MessageCommand` |
| Options per command | at most 25 | `ChatInputCommand` only |

In practice this means: chat-input commands always need a non-empty `${name}.description` key in their bundle, while user/message commands don't have a description at all — the framework's `registerUser` / `registerMessage` deliberately don't pass one through.

---

## Slash commands (`ChatInputCommand`)

Slash commands are the workhorse type. They have a name, a (required) description, and up to 25 **options** — the typed arguments the user fills in. Here's the smallest useful slash command:

```kotlin
class BanInfo : ChatInputCommand() {
    override val name = "ban_info"
    override val bundleName = "v4.ban_info"
    override val dmPermission = false

    val target: Option<Snowflake> by userId().description("ban_info.target")

    override suspend fun ChatInputCommandInteractionCreateEvent.execute() {
        val reply = interaction.deferPublicResponse()
        val target by option(target)
        // ... do something with target ...
        reply.respond { content = bundle.l("command.ban_info.response").format(target) }
    }
}
```

Three things to notice:

1. `target` is declared as a property of the class, using a delegated property (`by`). The delegate is what registers the option with Discord at startup.
2. Inside `execute()`, `target` is read with `by option(target)` — a *separate* delegate that pulls the submitted value out of the interaction. The same name is reused because at the read site it's a local variable, while at the class level it's the option declaration.
3. Strings come from a resource bundle via `bundle.l(...)`. The framework picks the right locale automatically.

### Declaring options

Options are declared with factory functions that return `Option<T>`, then chained with descriptor methods. The first call always picks the **type**:

| Factory | Type |
|---------|------|
| `string()` | `Option<String>` |
| `long()` | `Option<Long>` |
| `userId()` | `Option<Snowflake>` |

(These are the ones currently exposed in `BaseOption.kt`; add more as needed.)

After the type, `.description("locale.key")` is **required** — Discord rejects options without a description, and the framework enforces this at build time. The argument is a key inside your command's resource bundle.

You can then chain optional modifiers:

| Modifier | What it does |
|----------|--------------|
| `.optional()` | Wraps the option type as nullable. `Option<T>` becomes `Option<T?>`. The user may omit the argument. |
| `.map { value, _ -> ... }` | Transforms the parsed value. Useful for converting a `String` into a domain type, parsing a duration, etc. |
| `.choice(value, "locale.key", bundleName)` | Adds a Discord choice. Multiple `.choice(...)` calls accumulate. |
| `.minValue(n)` / `.maxValue(n)` | Numeric range bounds. |
| `.minLength(n)` / `.maxLength(n)` | String length bounds. |

Everything chains and returns a new immutable `Option`, so `userId().description("...").optional()` works as you'd expect.

### Reading option values

Inside `execute()`, you'd write:

```kotlin
val target by option(target)         // Snowflake
val reason by option(reason)         // String? (because reason was declared .optional())
```

The right-hand side is the option declaration; the left-hand side is whatever local name you want. Read inside the function body just like any local.

### A more involved example

Here's the actual `Clean` command from the codebase. It deletes recent messages, optionally filtered by user and time window:

```kotlin
class Clean : ChatInputCommand() {
    override val name = "clean"
    override val bundleName = "v4.clean"
    override val defaultMemberPermissions = Permissions(Permission.ManageMessages)
    override val dmPermission = false

    val limit:   Option<Long>      by long().description("clean.limit")
    val minutes: Option<Long?>     by long().description("clean.minutes").optional()
    val user1:   Option<Snowflake?> by userId().description("clean.user").optional()
    val user2:   Option<Snowflake?> by userId().description("clean.user").optional()
    val reason:  Option<String?>   by string().description("clean.reason").optional()

    override suspend fun ChatInputCommandInteractionCreateEvent.execute() {
        val reply = interaction.deferEphemeralResponse()
        val limit by option(limit)
        val minutes by option(minutes)
        val reason by option(reason)
        // ... etc ...

        if (limit !in 1..1000) {
            reply.respond { content = bundle.l("command.clean.invalid_limit").format(limit) }
            return
        }
        // ... do the work ...
    }
}
```

A couple of practical notes from this example:

- The command has four `user` options with distinct property names (`user1`, `user2`, ...) because Discord doesn't have a "variadic" option type. If you need a list, repeat the declaration.
- Validation happens inside `execute()` after reading values. There's no built-in declarative "must be in 1..1000" — you write the `if` yourself and respond with the appropriate error. This keeps the framework small.
- `deferEphemeralResponse()` acknowledges the interaction immediately, so you have up to 15 minutes to actually `respond { }`. Use it whenever the command might take more than a couple of seconds.

---

## User and Message commands

User and message commands are simpler — they have no options and no description (Discord forbids one for context-menu commands; the framework registers them with an empty description automatically). The user picks the command from a context menu, and the framework gives you the target user or message.

```kotlin
class GetCommandIssuer : MessageCommand() {
    override val name = "get_author"
    override val bundleName = "v4.get_author"
    override val defaultMemberPermissions = Permissions(Permission.ManageMessages)
    override val dmPermission = false

    override suspend fun MessageCommandInteractionCreateEvent.execute() {
        val reply = interaction.deferEphemeralResponse()
        val message = interaction.getTargetOrNull() ?: run {
            reply.respond { content = bundle.l("command.get_author.no_message") }
            return
        }
        // ... do something with message ...
    }
}
```

For `UserCommand`, the target is reached via `interaction.target` (a `User`). For `MessageCommand`, it's `interaction.getTargetOrNull()` (a nullable `Message`). Otherwise, the shape is identical to a slash command: defer, do the work, respond.

---

## Registration and discovery

Commands are discovered through Java's `ServiceLoader`, which means:

1. Your command must be a `class` (not `object`), because `ServiceLoader` constructs instances reflectively.
2. The fully qualified class name must appear in `META-INF/services/pw.modder.answernator4.interaction.Command`, one per line.
3. The compiled JAR must end up in the `./commands/` directory next to the bot's working directory. This is the same directory the legacy plugin system uses — both systems coexist.

At startup, `InteractionCommandList.load()` reads all JARs from `./commands/`, instantiates every command via `ServiceLoader`, and then `interactionCommandService()` in `main.kt` registers each one with Discord and wires up event dispatch.

For commands inside the main `Answernator` module, the services file lives at `Answernator/src/main/resources/META-INF/services/pw.modder.answernator4.interaction.Command`. Add your new command's FQCN to that file when you create it.

---

## Localization

The framework supports `en-US` (the root/default) and `ru`. Every command declares a `bundleName`, which translates to a path inside `src/main/resources/locale/`:

| `bundleName` | English bundle | Russian bundle |
|--------------|----------------|----------------|
| `v4.ban_info` | `locale/v4/ban_info.properties` | `locale/v4/ban_info_ru.properties` |
| `v4.clean` | `locale/v4/clean.properties` | `locale/v4/clean_ru.properties` |

The bundles are loaded through a custom `UTF8Control` so you can use Cyrillic (or anything else non-Latin) directly.

Two places use the bundle:

1. **Registration time** (once at startup). The framework calls `getAllLocalizations(bundleName, key)` for the command name, description, option names, and option descriptions, and ships the whole locale map to Discord. Discord then picks the right text for each user without further round-trips.

2. **Runtime** (per invocation). Inside `execute()`, `bundle.l("key")` reads the bundle for the user's locale (taken from `interaction.locale`) and returns the right string. Use this for your response content, error messages, etc.

### Keys you'll typically define

For a slash command `Foo` with options `bar` and `baz`, the bundle usually contains:

```properties
foo = foo                       # command name (the slash command itself)
foo.description = A foo command # command description
bar = bar                       # option name
bar.description = ...           # option description (required by Discord)
baz = baz
baz.description = ...

command.foo.some_response = Done: %s   # used in execute() via bundle.l("command.foo.some_response")
command.foo.error = Something went wrong
```

You're not strictly required to localize option names — if a `_ru` bundle is missing a key, the framework falls back to the root bundle. Russian translation is encouraged but not enforced.

---

## Permissions and DM availability

Two properties on `Command` map directly onto Discord registration:

- `defaultMemberPermissions: Permissions?` — if set, the command is invisible to users without these permissions by default. Server admins can override this in the integration settings.
- `dmPermission: Boolean?` — `false` to disable in DMs. Important for guild-only commands (anything that touches members, channels, bans, etc.).

Both are *defaults shipped to Discord at registration time*. They don't replace per-invocation checks: if you have business logic that should still be checked at runtime (e.g. "only the original message author can use this"), do that inside `execute()`.

---

## Buttons in commands

A command can optionally declare a `ButtonGroup` and an `onButtonClick` handler. The framework's global listener routes button clicks for the right command, so the command's `execute()` can render a button view and return immediately — no coroutine left hanging.

```kotlin
class StatusCommand : ChatInputCommand() {
    override val name = "status"
    override val bundleName = "v4.status"

    override val buttons = StatusButtons()

    override suspend fun ChatInputCommandInteractionCreateEvent.execute() {
        interaction.respondWithCommandButtons(this@StatusCommand)
    }

    override suspend fun ButtonInteractionCreateEvent.onButtonClick(button: ButtonField) {
        when (button) {
            buttons.refresh -> interaction.updateEphemeralMessage {
                content = "Status refreshed at ${Clock.System.now()}"
            }
        }
    }
}
```

The full story (the suspending alternative, how `customId`s are managed, what `respondWithCommandButtons` actually does) lives in [`buttons.md`](buttons.md).

---

## Where to look in the source

- `interaction/Command.kt` — the base class. Read this if you want to know what every command shares.
- `interaction/ChatInputCommand.kt` — slash command base. The `option(...)` extension that you use to read values inside `execute()` is defined here.
- `interaction/Interaction.kt` — the actual Kord registration calls. Useful if you're debugging "why isn't my command showing up".
- `interaction/InteractionCommandService.kt` — the event dispatcher. Read this if a command isn't receiving its event.
- `command/` — concrete commands. The best place to see real examples.