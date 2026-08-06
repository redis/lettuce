---
name: adding-a-redis-command
description: Use when adding a new Redis command — or a new overload/variant of one — to the Lettuce client. Covers the full flow: writing the command specification, designing the Lettuce API, adding argument types, editing all API interface flavors (sync/async/reactive/Kotlin/node-selection), adding the builder/async/reactive/Kotlin implementations, and adding unit + integration tests with the API consistency suite as the safety net. Trigger on requests like "add support for the <X> command", "implement <REDIS COMMAND> in Lettuce", "wire up a new command", or adding a new argument/overload to an existing command.
allowed-tools: Bash(mvn *)
---

# Adding a Redis command to Lettuce

Read [.agents/docs/architecture.md](../../../.agents/docs/architecture.md) first for the model
behind this flow — especially that the sync/async/reactive/Kotlin command
**interfaces are hand-edited source files kept in lockstep by the API consistency
test suite** (see [.agents/docs/api-consistency.md](../../../.agents/docs/api-consistency.md)),
while the top-level aggregate interfaces (`RedisCommands`, …) are hand-written
compositions.

## The flow

```
1. Specification → 2. API design → 3. Types + sync interface → ⛔ human review
                 → 4. Mirror to all flavors → 5. Implementations → 6. Tests → 7. Verify
```

Two things are front-loaded on purpose: you write the **spec before code**, and you
add **argument/response types before editing the interfaces** (every flavor
references those types, so they must exist to compile).

Copy this checklist into your working notes and tick items off as you go:

```
Add-a-command progress:
- [ ] 1. Spec: syntax, args, RESP2 + RESP3 replies, errors, version
- [ ] 2. API design: method(s), return type, target group, arg/response types
- [ ] 3. Types + sync interface: arg & response types first, then the sync method + Javadoc
- [ ] ⛔ HUMAN REVIEW: maintainer approves the sync interface change (the API contract)
- [ ] 4. Mirror: async, reactive, Kotlin, NodeSelection×2 — consistency tests must pass
- [ ] 5. Implementations: CommandType, RedisCommandBuilder, async, reactive, Kotlin impl
- [ ] 6. Tests: builder unit test + integration base/overloads (@EnabledOnCommand)
- [ ] 7. Verify: unit (mvn test) + a single integration test
```

---

### 1. Start from a specification

Before touching code, have a clear spec (HLD/PRD, usually attached to the
feature-request issue). It must state:

- **Syntax and arguments** — the full command grammar, mandatory vs optional args,
  ordering, and any subcommands/flags.
- **Response types in RESP2 *and* RESP3** — these can differ (e.g. a reply that is a
  flat array in RESP2 but a map or set in RESP3). This directly determines the
  `CommandOutput` you choose and the reactive `Mono`/`Flux` mapping.
- **Error scenarios** — when the server returns an error, and any client-side
  validation the command needs.
- **Version availability** — the first server version that supports it (drives
  `@EnabledOnCommand` gating and `@since`).

Source of truth: the linked HLD/issue (Confluence, Jira, or a GitHub issue) and the
Redis command reference (`https://redis.io/commands/<name>`). Read them with whatever
tools are available (web fetch, an Atlassian/Jira connector, `gh`); only ask the
maintainer if you genuinely cannot access the source. Do not guess reply shapes —
confirm them.

**The spec is your working context, not a deliverable.** Hold it in mind to drive the
work — do **not** paste it back to the maintainer, write it to a review file, or pause
for "spec approval." Work straight through §2 and §3 on your own judgement; the
**only** place to stop for the maintainer is the ⛔ checkpoint before mirroring. If a
design choice is genuinely ambiguous, make a reasonable call and surface it *there*,
with the sync interface change, rather than interrupting earlier.

### 2. Design the Lettuce API from the spec

Decide, from the spec:

- The method name(s) and overloads. **For every varargs parameter, also declare a
  single-argument overload** — e.g. both `foo(K key, V value)` and
  `foo(K key, V... values)`. (Existing commands like `hdel`/`rpush`/`sadd` are
  varargs-only; the single-arg overload is the convention for **new** commands.)
- The **Java return type**, mapping the reply to an idiomatic type: a **`1/0` integer
  reply → `Boolean`** (established repo convention — cf. `copy`, `expire`, `hsetnx`);
  a count/number → `Long`; a status → `String`; a bulk value → `V`. The other flavors
  wrap it (`RedisFuture<T>` / `Mono<T>` / `Flux<T>` / `suspend`/`Flow`).
- Which command group it belongs to (STRING → `RedisStringCommands`, HASH →
  `RedisHashCommands`, generic-key → `RedisKeyCommands`, …).
- What **argument/wrapper types** the signature needs for optional arguments.

### 3. Add argument types + the sync interface method

**Add the types the signature references first — both argument and response
types.** Every interface flavor references the types in the method signature, so
any *new* type must exist to compile:

- **Argument types.** If the signature takes an options object, create a
  `*Args implements CompositeArgument` class (e.g. `io.lettuce.core.CopyArgs`) whose
  `build(args)` appends its tokens, and register any argument modifiers in
  `src/main/java/io/lettuce/core/protocol/CommandKeyword.java`.
- **Response / return types.** If the command returns a shape with no existing
  model, create the value/result type it returns. Reuse existing types where
  possible — `Value`, `KeyValue`, `ScoredValue`, `GeoCoordinates`, `GeoWithin`,
  `StreamMessage`, `KeyScanCursor` (all in `io.lettuce.core`) — and add a new one
  only when the reply genuinely doesn't map to any of them.

(This is the Java type that appears in the signature. The matching reply *parser* —
the `CommandOutput` — is added later with the builder in §5.)

**Then add the method + Javadoc to the sync interface**
`src/main/java/io/lettuce/core/api/sync/<Group>Commands.java`. The sync signature is
the reference the other flavors are derived from and its Javadoc is the reference
text they mirror. Follow the [writing-javadoc](../writing-javadoc/SKILL.md) skill —
new public API needs `@since <version>` plus `@param`/`@return`.

```java
/**
 * Returns the length of the string value stored at {@code key}.
 *
 * @param key the key, must not be {@code null}.
 * @return the length of the string at {@code key}, or {@code 0} when {@code key} does
 *         not exist.
 * @since 7.7
 */
Long strlen(K key);
```

**No suitable group?** Create a new one: add the flavor interfaces (mirror an
existing group end-to-end), register the group in the `CommandInterfaces` enum
(`src/test/java/io/lettuce/core/api/consistency/`), and wire the new group interface
into the hand-written aggregate interfaces (`RedisCommands`, `RedisAsyncCommands`,
`RedisReactiveCommands`, and the cluster variants) so they `extend` it — the
consistency tests enforce the aggregate wiring.

### ⛔ Checkpoint — human review of the API contract

The sync interface change is the human-authored **API contract**; every other flavor
is derived from it and it's costly to change once mirrored. **Stop here.** Present
the sync interface change — the new method signature and its Javadoc — to the
maintainer and get approval **before mirroring it across flavors.** Do not proceed
to §4 until the maintainer approves.

### 4. Mirror the method to every flavor

Add the method by hand to each flavor of the group — async, reactive, Kotlin
coroutines, and the two cluster node-selection interfaces (unless the group has no
node-selection flavor). The per-flavor return-type mapping rules are owned by
[.agents/docs/api-consistency.md](../../../.agents/docs/api-consistency.md) — apply
them from there; they are deliberately not restated here.

Mirror the Javadoc with the flavor-appropriate `@return` phrasing. Then run the
consistency suite — it names exactly the flavor/signature you missed:

```bash
mvn -Dtest='*ConsistencyUnitTests,CommandBuilderCoverageUnitTests' \
    -Dsurefire.failIfNoSpecifiedTests=false test
```

For an **unusual return type** (e.g. `Flux<Value<Long>>`, or `Mono<List<Double>>`
because Redis returns nulls), register it in the registry that owns the flavor —
`src/test/java/io/lettuce/core/api/consistency/KnownApiDeviations.java` for the
Java flavors, `src/test/kotlin/io/lettuce/core/api/consistency/KnownKotlinApiDeviations.kt`
for the coroutine flavor — **with a comment justifying it**. Never use a deviation entry to paper over a sync/async
signature mismatch — that breaks the sync-over-async runtime proxy.

### 5. Add the implementations

The interfaces declare the new method; now make it real:

**Keyword** — `src/main/java/io/lettuce/core/protocol/CommandType.java`; add the
command name (wire bytes derive from the enum name):

```java
public enum CommandType implements ProtocolKeyword {
    ..., APPEND, GET, GETDEL, ..., STRLEN, LCS, ...
    CommandType() { command = name(); }
}
```

**Subcommand tokens** (e.g. `PREPARE`) go in `CommandKeyword`, added the same way.
But if the token name **already exists in `CommandType`** (e.g. `SET`, `DISCARD`), do
**not** add a duplicate to `CommandKeyword` — the builder static-imports both enums,
so the bare name becomes ambiguous and won't compile. Reference `CommandType.SET` /
`CommandType.DISCARD` directly in the builder instead (same wire bytes).

**Builder** — `src/main/java/io/lettuce/core/RedisCommandBuilder.java`; build the
`Command` with the `CommandOutput` chosen from the spec's RESP2/3 reply.
**Argument order is the wire order.**

```java
public Command<K, V, Long> strlen(K key) {
    notNullKey(key);
    return createCommand(STRLEN, new IntegerOutput<>(codec), key);
}

public Command<K, V, Boolean> copy(K source, K destination, CopyArgs copyArgs) {
    LettuceAssert.notNull(source, "Source " + MUST_NOT_BE_NULL);
    LettuceAssert.notNull(destination, "Destination " + MUST_NOT_BE_NULL);
    CommandArgs<K, V> args = new CommandArgs<>(codec).addKey(source).addKey(destination);
    copyArgs.build(args);
    return createCommand(COPY, new BooleanOutput<>(codec), args);
}
```

**Async dispatch** — `src/main/java/io/lettuce/core/AbstractRedisAsyncCommands.java`:

```java
public RedisFuture<Long> strlen(K key) { return dispatch(commandBuilder.strlen(key)); }
```

**Reactive dispatch** — `src/main/java/io/lettuce/core/AbstractRedisReactiveCommands.java`
(`createMono` for scalars, `createDissolvingFlux` for `List`/`Set`, matching the
generated `Mono`/`Flux`):

```java
public Mono<Long> strlen(K key) { return createMono(() -> commandBuilder.strlen(key)); }
```

**Kotlin coroutine impl** —
`src/main/kotlin/io/lettuce/core/api/coroutines/<Group>CoroutinesCommandsImpl.kt`:

```kotlin
override suspend fun strlen(key: K): Long = ops.strlen(key).awaitSingle()
```

**Cluster** — a single-key command flows through automatically
(`RedisClusterAsyncCommands` extends the group interfaces). For a **broadcast /
all-shards** command, hand-code the fan-out override in
`RedisAdvancedClusterAsyncCommandsImpl` *and* its reactive sibling using
`executeOnUpstream` + a `MultiNodeExecution` aggregator (add a helper if no existing
aggregator matches the response policy). See the "Cluster routing" section of
[.agents/docs/architecture.md](../../../.agents/docs/architecture.md).

**Format before you build.** After hand-editing (here and after §6), run
`mvn formatter:format` — the build's `formatter:validate` step fails the compile on
any unformatted new code, so do it before every compile/test run.

### 6. Add tests

How tests are named, placed, and run is owned by
[.agents/docs/integration-testing.md](../../../.agents/docs/integration-testing.md) — follow it.
What is specific to a **command**:

- **Builder unit test** (no server): assert the constructed command and its encoded
  args — including the RESP2/3 output shape from §1 — in
  `src/test/java/io/lettuce/core/Redis<Group>CommandBuilderUnitTests.java`.
- **Integration tests**: write the base test against the sync `RedisCommands` API,
  then add the overloads (RESP2 / cluster / reactive / Tx) that matter for the
  command. Base tests only run under overloads that **exist** — check the group isn't
  missing one its peers have (e.g. the hash group has no RESP2 class) and **create it**
  if so. That base+overload structure is owned by the "Command test scope" section of
  [.agents/docs/integration-testing.md](../../../.agents/docs/integration-testing.md). Gate
  version-specific commands with `@EnabledOnCommand("<NAME>")`:

```java
@Test
@EnabledOnCommand("COPY")
void copy() {
    redis.set(key, value);
    assertThat(redis.copy(key, key + "2")).isTrue();
}
```

### 7. Verify

Run the unit and integration tests as documented in
[.agents/docs/integration-testing.md](../../../.agents/docs/integration-testing.md) (`mvn clean test`
for unit; `make start` + a single `*IntegrationTests` run for integration).

---

## Top pitfalls

1. **Skipping the spec / not checking RESP2 vs RESP3.** The reply shape can differ
   between protocols; it determines the `CommandOutput` and reactive mapping. Confirm
   it against `redis.io`, don't assume.
2. **Adding Args/wrapper types after the interface edits.** Every flavor references
   them; if they don't exist first, the project won't compile. Add them upfront.
3. **Editing only one flavor, or silencing the consistency suite.** Mirror the method
   to *every* flavor; if a test fails, fix the signature rather than adding a
   deviations-registry entry — deviations are for genuinely unusual reply shapes,
   with a justifying comment, never for sync/async mismatches.
4. **Forgetting a dispatch layer.** Update *both* `AbstractRedisAsyncCommands` *and*
   `AbstractRedisReactiveCommands`, plus the Kotlin `*Impl.kt`.
5. **Wrong `CommandArgs` order / wrong `CommandOutput`**, missing `@since`, or
   missing tests / version gate (`@EnabledOnCommand`).
```
