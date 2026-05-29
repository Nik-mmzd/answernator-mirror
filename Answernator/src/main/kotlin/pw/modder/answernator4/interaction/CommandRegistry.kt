package pw.modder.answernator4.interaction

/**
 * A snapshot of the commands deployed to Discord, for commands that need to introspect the command
 * set (count, listing, lookup by name, …).
 *
 * A [Command] is itself a member of the `Set<Command>` multibinding, and Kodein throws a
 * `DI.DependencyLoopException` if a set member resolves its own set — this holds for lazy, direct,
 * provider retrieval and even via an intermediate holder, and the loop is thrown even after the set
 * has been fully built once. Commands therefore cannot inject `Set<Command>` directly.
 *
 * [interactionCommandService] resolves the set from outside the set (where it is safe) and publishes
 * the snapshot here at deploy time, re-publishing it on every (re)deploy. Commands inject this
 * registry instead.
 */
class CommandRegistry {
    var commands: List<Command> = emptyList()
        internal set
}