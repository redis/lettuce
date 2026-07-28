# API consistency: hand-edited interfaces, test-enforced parity

The sync/async/reactive/Kotlin-coroutine command interfaces are **hand-edited
source files** — there is no code generation. Consistency between the flavors is
enforced by a reflection-based unit-test suite that runs in every build. This doc
owns the mapping rules and the workflow for editing the interfaces.

> Historical note: until 2026 these interfaces were generated from templates in
> `src/main/templates/` by JUnit "generator" classes in
> `src/test/java/io/lettuce/apigenerator/`. The generators drifted from the
> committed files and were removed; the consistency tests below replace them.

## The interface flavors

Each command group (STRING, HASH, …) exists in up to six parallel interfaces:

| Flavor | Package | Example | Return shape |
|--------|---------|---------|--------------|
| sync | `io.lettuce.core.api.sync` | `RedisStringCommands` | `T` |
| async | `io.lettuce.core.api.async` | `RedisStringAsyncCommands` | `RedisFuture<T>` |
| reactive | `io.lettuce.core.api.reactive` | `RedisStringReactiveCommands` | `Mono<T>` / `Flux<E>` |
| Kotlin coroutines | `io.lettuce.core.api.coroutines` (src/main/kotlin) | `RedisStringCoroutinesCommands` | `suspend fun …: T?` / `Flow<E>` |
| cluster node-selection sync | `io.lettuce.core.cluster.api.sync` | `NodeSelectionStringCommands` | `Executions<T>` |
| cluster node-selection async | `io.lettuce.core.cluster.api.async` | `NodeSelectionStringAsyncCommands` | `AsyncExecutions<T>` |

The SENTINEL group lives under `io.lettuce.core.sentinel.api.*`; SENTINEL and
TRANSACTIONAL have no node-selection flavor.

**The sync interface is the reference.** Async/reactive/coroutine/node-selection
declarations are derived from it by the mapping rules below. (Two methods —
`setAutoFlushCommands`, `flushCommands` — exist on async/reactive/coroutines but
intentionally not on sync.)

## Mapping rules (what the tests enforce)

- **async** = same method, return wrapped in `RedisFuture<T>` (primitives boxed).
  Exceptions (`shutdown`, `close`, `isOpen`, …) keep the sync type.
- **reactive** = `Mono<T>` for scalars; `List<E>`/`Set<E>` → `Flux<E>`; a few
  methods force `Flux` (`eval`, `dispatch`, …) or wrap elements in `Value<…>`
  (`geopos`, `bitfield`, …); streaming-channel variants are `@Deprecated`.
- **coroutines** = `suspend fun` returning the sync type; collection-streaming
  methods return `Flow<E>` (not suspend); deprecated and streaming-channel
  methods are omitted. The suite verifies presence and the suspend/`Flow` shape
  only — scalar return types and nullability are **not** checked; review them by
  hand.
- **node-selection** = sync method with return wrapped in `Executions<T>` /
  `AsyncExecutions<T>`; connection-control methods (`shutdown`, `close`,
  `readOnly`, …) are omitted.
- **aggregates** — `RedisCommands`, `RedisAsyncCommands`, `RedisReactiveCommands`,
  `RedisCoroutinesCommands`, `RedisClusterCommands` (+async/reactive) and
  `NodeSelection(Async)Commands` must extend the per-group interface of every
  group they cover.
- **builder coverage** — every sync command has a same-named method on its
  group's command builder, and every command-producing builder method is
  reachable from an interface. Core groups map to `RedisCommandBuilder`; the
  module groups have dedicated builders (`RedisJsonCommandBuilder`,
  `RediSearchCommandBuilder`, `RedisArrayCommandBuilder`, …) and SENTINEL maps
  to `SentinelCommandBuilder` — the mapping is
  `CommandInterfaces#commandBuilderClassName()`.
- across the Java flavors (async, reactive, node-selection, aggregates) the
  suite also verifies **generic parameter signatures** (not just erased types)
  and **`@Deprecated` parity** with the sync method.

All exceptions to these rules live in **one registry**:
`src/test/java/io/lettuce/core/api/consistency/KnownApiDeviations.java`.

## The test suite

`src/test/java/io/lettuce/core/api/consistency/` (plus one Kotlin test in
`src/test/kotlin/.../consistency/`):

| Class | Checks |
|-------|--------|
| `CommandInterfaces` | the catalog: one enum entry per command group → its six flavor classes |
| `KnownApiDeviations` | the exceptions registry (ported from the former generators) |
| `TypeSignatures` | shared reflection/normalization helpers |
| `SyncAsyncConsistencyUnitTests` | presence both directions + `RedisFuture` wrapping |
| `SyncReactiveConsistencyUnitTests` | presence both directions + `Mono`/`Flux` mapping + streaming deprecation |
| `NodeSelectionConsistencyUnitTests` | presence both directions + `Executions` wrapping + exclusion-list freshness |
| `AggregateInterfaceConsistencyUnitTests` | umbrella interfaces extend every group |
| `CommandBuilderCoverageUnitTests` | interface ↔ per-group command builder coverage, both directions |
| `KotlinCoroutinesConsistencyUnitTests` | presence both directions + suspend/`Flow` shape (nullability is not checked) |

Run them (they are plain `*UnitTests`, included in `mvn test`):

```bash
mvn -Dtest='*ConsistencyUnitTests,CommandBuilderCoverageUnitTests' \
    -Dsurefire.failIfNoSpecifiedTests=false test
```

## Editing workflow

1. **Edit the sync interface first** — signature + Javadoc. This is the API
   contract; the sync file's Javadoc is the reference text for the other flavors
   (see [javadoc.md](javadoc.md)).
2. **Mirror to every flavor** (async, reactive, Kotlin, node-selection ×2 unless
   the group has none), applying the mapping rules above and mirroring the
   Javadoc with the flavor-appropriate `@return` phrasing.
3. **Run the consistency tests** — they tell you exactly which flavor/signature
   you missed. Iterate until green.
4. For a genuinely unusual shape (e.g. `Mono<List<Double>>` because Redis
   returns null elements), add an entry to `KnownApiDeviations` **with a comment
   justifying it**. Never add an entry to paper over a sync/async signature
   mismatch — the sync API is a runtime proxy over async
   (`FutureSyncInvocationHandler`), so that mismatch throws at runtime.

**New command group?** Create the flavor interfaces (mirror an existing group),
register the group in `CommandInterfaces`, and wire it into the aggregate
interfaces; the aggregate test enforces the wiring.

## Rules

- Interfaces are ordinary source files: edit them directly, keep `@since` tags
  and house Javadoc (see [javadoc.md](javadoc.md)).
- Javadoc is duplicated across flavors by design; the tests do **not** check
  Javadoc — keep the flavors in sync by hand, using the sync file as reference.
- PubSub interfaces (`io.lettuce.core.pubsub.api.*`) are out of scope for the
  consistency suite (they were never template-generated and follow their own
  shape).
