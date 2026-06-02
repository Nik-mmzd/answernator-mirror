# Answernator v4 — Anti-spam

This document describes the v4 anti-spam subsystem: how it detects spam, how it decides what to do
about it, and how the pieces are wired together. Unlike `commands.md` / `modals.md` / `buttons.md`
(which document the *framework*), this is a **feature** doc — but it follows the same conventions,
so it lives here.

The code lives under `pw.modder.answernator4.antispam` (pure detection logic), `…db` (persistence)
and `…kord.antiSpamService` (the runtime Kord glue). It replaces the legacy text-prefix anti-spam in
`pw.modder.answernator`.

## Mental model

The subsystem is split into three layers, deliberately, so the hard part — deciding *whether*
something is spam — is pure, deterministic and unit-testable, with no Kord or DB in sight:

**1. Pure detection (`pw.modder.answernator4.antispam`).** Given a message reduced to a canonical
string, decide whether it is spam and, if so, what severity. No I/O, no suspend, fully covered by
unit tests. Holds only ephemeral in-memory state (per-process, lost on restart).

**2. Persistence (`pw.modder.answernator4.db`).** The per-guild config (thresholds, texts, the log
channel) and the *durable escalation state*: how many times a user has been muted / flagged recently.
This is what survives a restart and drives the mute-vs-ban decision.

**3. Runtime glue (`kord.antiSpamService`).** Subscribes to `MessageCreateEvent`, feeds messages
through the detection layer, reads/writes the persistence layer, and performs the actual Discord
side effects (timeout, ban, message cleanup, log embed). Every REST call is wrapped in try/catch —
a failure to mute must never crash the handler or skip the rest of the logic.

```
Answernator/src/main/kotlin/pw/modder/answernator4/
├── antispam/                                — pure detection logic (no Kord, no DB)
│   ├── AntiSpamDefaults.kt                  — engine knobs (window, thresholds, caps)
│   ├── MessageCanonicalizer.kt             — message → aggressively normalized canonical string
│   ├── Similarity.kt                        — Jaccard similarity over word shingles
│   ├── SpamTracker.kt                       — sliding-window per-(guild,user) grouping + decision
│   └── MrBeastDetector.kt                   — image-dump ("MrBeast lover") pattern detector
├── db/
│   ├── tables/AntiSpamConfig.kt             — AntiSpamConfigs table + entity (per-guild config)
│   ├── tables/SpamMutes.kt                  — audit + escalation state for normal spam mutes
│   ├── tables/MrBeastLovers.kt             — audit + escalation state for MrBeast violations
│   └── cache/                               — repositories (config cache + the two stores)
├── kord/AntiSpam.kt                         — antiSpamService(): the runtime enforcement service
└── di/AntiSpamModule.kt                     — Kodein wiring (repos, migrations, intents, service)
```

## The detection pipeline

### Canonicalization (`MessageCanonicalizer`)

A message is first reduced to a **canonical string** so that "similar" — not just byte-identical —
messages collapse together. `canonicalize(content, attachments)`:

- NFKC-normalizes and lowercases the text;
- replaces structured tokens with placeholders *before* stripping digits, so their embedded ids
  don't leak: custom emoji → `:e:`, mentions (`<@…>`, `<@!…>`, `<@&…>`, `<#…>`) → `@u`, URLs → just
  their host;
- strips diacritics (NFD + drop combining marks), pictographs / VS16 / ZWJ, and all digits;
- collapses runs of a repeated character (`aaaa` → `aa`) and runs of whitespace;
- appends a stable **attachment signature** — Discord exposes no content hash, so each attachment
  contributes `filename:size`, sorted, as `att|name:size,…`.

There is also `commandCanon(name)` → `"cmd:<lowercased name>"`, used for command spam (see below).
The `cmd:` prefix guarantees it can never collide with a normalized text canon.

### Similarity (`Similarity`)

Two canonical strings are compared with the **Jaccard index over word shingles** (size
`SHINGLE_SIZE = 2`, i.e. word bigrams; texts shorter than the shingle size degrade to the unigram
token set so short messages still compare). Two empty sets are treated as identical (`1.0`); one
empty and one not is `0.0`. Messages are "the same" when similarity reaches
`SIMILARITY_THRESHOLD = 0.8`.

### Tracking & the decision (`SpamTracker`)

`SpamTracker` holds the ephemeral state, counted **per (guild, user)** over a sliding window
(`DETECTION_WINDOW = 10 min`). Each *distinct* message forms its own **group** with its own counter,
so a user cycling through several different spam messages is tracked per message rather than as one
blurred stream. A new message either joins the first group whose canon matches (exact or
Jaccard ≥ threshold) or starts a new group.

`record(...)` returns a `SpamDecision`:

| `SpamAction` | When | What the caller does |
|--------------|------|----------------------|
| `NONE`       | below all thresholds | nothing |
| `WARN`       | group count **equals** `warningThreshold` (fires once) | reply with the guild's warning text |
| `ESCALATE`   | group count **reaches** `muteThreshold` | mute or ban (decided from DB history) |

Two important correctness properties, both guaranteed under a single lock:

- **Atomic reset on escalation.** When a group reaches `muteThreshold`, the offending group's tracked
  message ids are captured into `SpamDecision.trackedMessages` *and the user's whole state is cleared*
  in the same critical section, so concurrent triggers from a burst can't double-fire.
- **Bounded memory.** Groups expire out of the window; per-user groups are capped
  (`MAX_GROUPS_PER_USER = 16`) and tracked ids per group are capped (`MAX_TRACKED_PER_GROUP = 100`).

REST side effects are intentionally **not** done under the lock — they're the caller's job.

A threshold of `0` disables that stage (`warningThreshold = 0` → never warn; `muteThreshold = 0` →
never escalate). The engine knobs (window, similarity, shingle size, caps) live in `AntiSpamDefaults`
and are deliberately *not* per-guild; only the counts and texts are configurable.

### MrBeast image-dump detection (`MrBeastDetector`)

A separate detector for the "MrBeast lover" pattern: image dumps with little or no text. A message
qualifies only when its description is **trivial** (blank or a single whitespace-separated token).
Then:

- **≥ 4 attachments in one message** (`SINGLE_ATTACHMENT_THRESHOLD`) → immediate `Triggered`.
- **≥ 2 attachments** (`PAIR_ATTACHMENT_THRESHOLD`) → `Suspicious`; an ephemeral marker is stored.
  A second such message within `DETECTION_WINDOW` → `Triggered` (both messages returned for cleanup).
- otherwise → `None`.

Like `SpamTracker`, the only state is an ephemeral per-(guild, user) marker, held under a lock and
cleared on trigger. The 90-day repeat-offender accounting lives in the DB, not here — the detector
only decides whether a *violation* occurred.

## Configuration (`AntiSpamConfigs` / `AntiSpamConfigData`)

Per-guild config is one row in `ANTISPAMCONFIGS`, keyed by the guild snowflake (the `id` column —
supplied externally, not auto-incremented). It is read through `AntiSpamConfigRepository`, a cached
repository that hands back an immutable `AntiSpamConfigData` snapshot safe to hold outside a
transaction.

| Field | Meaning |
|-------|---------|
| `isEnabled` | master switch for the whole subsystem |
| `isMrBeastEnabled` (`filter_mrbeast`) | enable the image-dump detector |
| `warningText` / `muteText` / `banText` | user-facing reply texts (already localized at config time) |
| `warningThreshold` | group count that triggers a (single) warning; `0` disables |
| `muteThreshold` | group count that triggers enforcement |
| `mutesBeforeBan` | repeat-offender policy — see below |
| `muteDuration` | mute timeout length, **minutes** (clamped to Discord's 28-day cap) |
| `muteValidity` | **days** a mute keeps counting toward the ban threshold |
| `logChannel` | optional channel for log embeds |

### `mutesBeforeBan` semantics

This single integer encodes the whole mute-vs-ban policy:

- **`-1`** → never ban; always mute (mute-only mode).
- **`0`** → ban immediately on the first escalation (no mute stage).
- **`N > 0`** → mute until the user has `N` mutes within `muteValidity` days, then ban.

The count comes from `SpamMuteRepository.countMutes(guild, user, within = muteValidity days)`. When
`mutesBeforeBan == 0` the count query is skipped entirely.

## Enforcement flow (`antiSpamService`)

`antiSpamService(di)` is a `suspend fun Kord.()` extension, contributed as a `KordConfiguration` (see
wiring below). It resolves the repositories, creates **one** `SpamTracker` and **one**
`MrBeastDetector` for the process lifetime, and registers a single `on<MessageCreateEvent>` handler.

The handler dispatches in this order:

1. **Guards.** Require a guild; require `message.author`; load config; require `isEnabled`.
2. **Command spam (interactions).** If `message.interaction != null`, the message is a slash-command
   response. Our *own* responses are ignored (`messageAuthor.id == kord.selfId`); another bot's
   response is attributed to the user who invoked the command (`interaction.user`, resolved to a full
   `User`), keyed by `commandCanon(interaction.name)`, and run through the shared similarity path.
   Then `return` — MrBeast does not apply to command responses.
3. **Plain messages.** Ignore *all* bots (including ourselves) and content-less messages.
4. **MrBeast** (if `isMrBeastEnabled` and there are attachments). On `Triggered`, enforce and return,
   short-circuiting the normal path.
5. **Normal similarity tracking** on the canonicalized content + attachment signature.

Steps 2 and 5 funnel into one shared `handleSpam(gid, offender, canon, …)`: track → `NONE` /
`WARN` (reply) / `ESCALATE`. On escalate it computes `priorMutes`, decides `shouldBan` from
`mutesBeforeBan`, and calls `banUser` or `muteUser`.

- **Mute** (`muteUser`): timeout the member via `member.edit { communicationDisabledUntil = now +
  min(muteDuration, 28 days) }`, reply with `muteText`, persist the mute (`insertMute` — audit +
  escalation state), delete the offending group's tracked messages, and post a log embed.
- **Ban** (`banUser`): `guild.ban { deleteMessageDuration = DETECTION_WINDOW }` (Discord sweeps the
  recent messages itself), no channel reply (the user is gone), log embed only.
- **MrBeast** (`enforceMrBeast`): count violations in the **90-day** window; first hit → 7-day mute +
  delete the offending message(s); a prior hit in the window → ban. The violation is always recorded.

### Member resolution caveat

In the command-spam path, the `MessageCreateEvent`'s `member` is the *responding bot*, not the
offender. `muteUser` therefore only trusts `event.member` when its id matches the offender, otherwise
it resolves the offender's member explicitly:

```kotlin
val member = member?.takeIf { it.id == author.id } ?: message.getGuildOrNull()?.getMemberOrNull(author.id)
```

## Persistence & escalation

Two append-only tables back the repeat-offender logic; both are audit logs *and* escalation state.

| Table | Written by | Read by | Window |
|-------|-----------|---------|--------|
| `SPAMMUTES` | `SpamMuteRepository.insertMute` on every normal-spam mute | `countMutes` → `mutesBeforeBan` decision | `muteValidity` (days, per guild) |
| `MRBEASTLOVERS` | `MrBeastRepository.insertViolation` on every MrBeast violation | `countViolations` → mute vs ban | 90 days (hardcoded) |

Both stores follow the same shape: `withContext(Dispatchers.IO) { transaction(database) { … } }`,
with `count()` queries built from the **top-level** Exposed 1.x operators
(`org.jetbrains.exposed.v1.core.eq` / `and` / `greaterEq` — these are no longer members of
`SqlExpressionBuilder` in Exposed 1.x).

The migration is `db/migrations/antispam/V1__AntiSpam_Init.sql`. It uses the dialect-portable
placeholders (`${datatype:…}`, etc.) described in the root `CLAUDE.md`; the three tables are
registered into the `"tables-in-use"` bind-set so the runtime drift-check covers them.

## Localization

User-facing texts come from two sources:

- **Config columns** (`warningText` / `muteText` / `banText`) — localized once, when the guild's
  config is written.
- **The resource bundle** `locale/v4/anti_spam[_ru].properties` — used for MrBeast texts (which have
  no config columns) and for all **log-channel embeds**, resolved per call in the guild's
  `preferredLocale` (root bundle as fallback) via the `antiSpamBundle()` helper.

Bundle keys: `antispam.{warning,mute,ban}`, `antispam.mrbeast.{mute,ban}`,
`antispam.log.{mute,ban}.title`, `antispam.log.mrbeast.{mute,ban}.title`,
`antispam.log.field.{channel,duration,recent_mutes,violations}`, and the unit strings
`antispam.log.{minutes,days}` (printf `%d`).

## DI wiring & intents (`AntiSpamModule`)

`AntiSpamModule` is a `KodeinModuleProvider`. It:

- `importOnce(DatabaseModule)`;
- binds `AntiSpamConfigRepository`, `SpamMuteRepository`, `MrBeastRepository` as singletons;
- adds `AntiSpamConfigs`, `MrBeastLovers`, `SpamMutes` to the `"tables-in-use"` bind-set;
- contributes its migration location to `"flyway-migrations"` (it owns the **`1.x`** version
  namespace; Logs owns `2.x`);
- contributes the service: `inBindSet<KordConfiguration> { addSingleton { KordConfiguration { antiSpamService(di) } } }`;
- contributes the gateway intents it needs:
  `inBindSet<Intents> { addSingleton { Intents { +Intent.MessageContent; +Intent.GuildMessages } } }`.

`MessageContent` is a **privileged** intent (hence `@OptIn(PrivilegedIntent::class)` on the module) and
must be enabled in the Discord Developer Portal. `main.kt` folds every contributed `Intents` into the
login builder (`intentsList.forEach { intents += it }`), so the subscription is automatic.

### Legacy migration mapping

`LegacyConfigMigrationModule` maps the old single-table config into the v4 split. For anti-spam it
sets `muteThreshold = legacy.antiSpamBan` (legacy fired straight to a ban at that count) and
`mutesBeforeBan = 0` to preserve the old "ban immediately" behaviour, with `muteDuration = 60` and
`muteValidity = 30` as inert defaults.

## Known limitations & gotchas

- **Ephemeral tracking.** `SpamTracker` / `MrBeastDetector` state is per-process and lost on restart.
  Durable escalation (mute/violation counts) is in the DB and survives; the short-window counters do
  not. This is intentional.
- **Ban + command spam.** When a *ban* results from command spam, Discord's `deleteMessageDuration`
  only deletes the **offender's own** messages — the spam *responses* from the other bot remain. The
  *mute* path does clean them (`cleanupTracked` deletes the tracked response messages).
- **`message.interaction` is deprecated** in Kord (in favour of `interactionMetadata`), but the rest
  of the project uses `message.interaction`, so the anti-spam path follows that convention.
- **Timeout cap.** Discord caps a member timeout at 28 days; `muteDuration` is clamped, and the
  MrBeast 7-day mute is safely under it.

## Testing

The pure detection layer is covered by unit tests under `pw.modder.answernator4.antispam`
(canonicalization, Jaccard thresholds, `SpamTracker` windowing/grouping/reset incl. an 8-thread
concurrency test, `MrBeastDetector` single/pair patterns and trivial-description handling). The Kord
glue (`antiSpamService`) is verified by compilation; the warn→mute→ban progression, `mutesBeforeBan`
variants, bulk cleanup, the MrBeast 1→2 escalation and command-spam attribution are checked by manual
runs against a live bot (which needs the privileged intent enabled and a populated guild config).
