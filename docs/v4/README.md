# Answernator v4 — Interaction Framework

This is the documentation for the v4 interaction framework: the modern slash/user/message command system built on top of [Kord](https://github.com/kordlib/kord), living under the `pw.modder.answernator4` package.

The framework is the successor to the legacy text-prefix command system (`pw.modder.answernator`). New commands should be written against v4; the old system is kept around only until everything has been migrated.

## What's in here

| Document | What it covers |
|----------|----------------|
| [`commands.md`](commands.md) | The three command base classes, how to declare options, register a command, and localize its text. |
| [`modals.md`](modals.md) | The modal framework — a typed wrapper around Kord's modal API that lets you collect structured input from users. |
| [`buttons.md`](buttons.md) | Two ways to add clickable buttons to interaction responses: a short-lived "wait for a click" pattern, and a command-bound stateless pattern. |

If you're new to the codebase, read them in that order — modals and buttons both build on the command framework.

## Mental model

The framework is intentionally small and revolves around three ideas:

**Commands are classes.** You subclass `ChatInputCommand`, `UserCommand`, or `MessageCommand`, override a couple of properties, and put your logic inside an `execute()` extension method. The framework takes care of registering the command with Discord and routing incoming interactions to your `execute()`.

**Discord components are declared, not built imperatively.** Whether you're declaring command options, modal fields, or buttons, the pattern is the same: declare them as `val`s on a class, get typed references back, read submitted values via a typed lookup. No string-keyed maps in user code; the framework handles `customId`s under the hood.

**Wrappers compose.** Variants like "this option is optional", "this select supports multi-selection", "this button is disabled" are expressed by chaining methods on the base declaration, not by spawning new factory functions. Same pattern across commands, modals, and selects.

## Where the code lives

```
Answernator/src/main/kotlin/pw/modder/answernator4/
├── main.kt                                      — bot entry point
├── command/                                     — concrete command implementations
└── interaction/                                 — the framework itself
    ├── Command.kt                               — abstract base class (name, bundle, permissions, button hooks)
    ├── ChatInputCommand.kt                      — base for slash commands
    ├── UserCommand.kt                           — base for user-context commands
    ├── MessageCommand.kt                        — base for message-context commands
    ├── Option.kt, BaseOption.kt, WrapperOption.kt — options for ChatInputCommand
    ├── Interaction.kt                           — Discord registration helpers
    ├── InteractionCommandList.kt                — ServiceLoader-backed command registry
    ├── InteractionCommandService.kt             — wires commands into Kord events
    ├── LocalizableString.kt                     — small wrapper for locale keys
    ├── modal/                                   — modal framework (see modals.md)
    └── button/                                  — button framework (see buttons.md)
```

## A note on style

The framework is opinionated: it prefers a few well-defined extension points (override `execute()`, declare `val`s for options/fields/buttons) over a giant builder DSL. When in doubt, look at an existing command in `pw.modder.answernator4.command/` for the canonical shape.
