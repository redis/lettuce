---
name: adding-a-redis-command
description: Use when adding a new Redis command — or a new overload/variant of one — to the Lettuce client. Covers the full flow: writing the command specification, designing the Lettuce API, adding argument types and the API template, running the interface generators, adding the builder/async/reactive/Kotlin implementations, and adding unit + integration tests. Trigger on requests like "add support for the <X> command", "implement <REDIS COMMAND> in Lettuce", "wire up a new command", or adding a new argument/overload to an existing command.
---

# Adding a Redis command to Lettuce

Read [.agents/docs/architecture.md](../../../.agents/docs/architecture.md) first for the model
behind this flow — especially that the sync/async/reactive/Kotlin command
**interfaces are generated from templates** and must never be hand-edited, while the
top-level aggregate interfaces (`RedisCommands`, …) are hand-written compositions.

## The flow

```
1. Specification   → 2. API design → 3. Types + template → 4. Generate
                   → 5. Implementations → 6. Tests → 7. Verify
```

Two things are front-loaded on purpose: you write the **spec before code**, and you
add **argument types + templates before running the generator** (the generated
interfaces reference those types, so they must exist to compile).

Copy this checklist into your working notes and tick items off as you go:

```
Add-a-command progress:
- [ ] 1. Spec: syntax, args, RESP2 + RESP3 replies, errors, version
- [ ] 2. API design: method(s), return type, target group, arg/response types
- [ ] 3. Types + template: arg & response types first, then the template method + Javadoc
- [ ] 4. Generate: run the api_generator tests; do NOT hand-edit generated interfaces
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

Source of truth: the Redis command reference (`https://redis.io/commands/<name>`)
plus the issue. Do not guess reply shapes — confirm them.

### 2. Design the Lettuce API from the spec

Decide, from the spec:

- The method name(s) and any overloads.
- Parameter types and the **Java return type** (which the generator maps to
  `RedisFuture<T>` / `Mono<T>` / `Flux<T>`).
- Which command group it belongs to (STRING → `RedisStringCommands`, HASH →
  `RedisHashCommands`, generic-key → `RedisKeyCommands`, …).
- What **argument/wrapper types** the signature needs for optional arguments.

### 3. Add argument types + the template

**Add the types the signature references first — both argument and response
types.** The generated interfaces reference every type in the method signature, so
any *new* type must exist and compile **before** you generate:

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

**Then add the method + Javadoc to the template**
`src/main/templates/io/lettuce/core/api/<Group>Commands.java`. Declare the **sync**
return type; this one edit feeds every generated flavor. Follow the
[writing-javadoc](../writing-javadoc/SKILL.md) skill — new public API needs
`@since <version>` plus `@param`/`@return`.

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

Do not write the class-level `${intent}` placeholder — the generators substitute it.

**No suitable group?** Create a new one: add the template file under
`src/main/templates/…`, register its name in
`src/test/java/io/lettuce/apigenerator/Constants.java` (`TEMPLATE_NAMES`), and wire
the new group interface into the hand-written aggregate interfaces (`RedisCommands`,
`RedisAsyncCommands`, `RedisReactiveCommands`, and the cluster variants) so they
`extend` it. Mirror an existing group end-to-end and verify the aggregate wiring.

### 4. Run the generators

Run the `api_generator`-tagged generators so the interfaces are (re)written —
`CreateSyncApi`, `CreateAsyncApi`, `CreateReactiveApi`, `CreateKotlinCoroutinesApi`,
and the cluster `Create*NodeSelectionClusterApi` classes. They overwrite these
checked-in, never-hand-edited files:

- `src/main/java/io/lettuce/core/api/{sync,async,reactive}/<Group>*Commands.java`
- `src/main/kotlin/io/lettuce/core/api/coroutines/<Group>CoroutinesCommands.kt`
- cluster NodeSelection interfaces

```bash
mvn -Dtest=CreateSyncApi -Dsurefire.failIfNoSpecifiedTests=false test
```

For an **unusual return type** (e.g. `Flux<Value<Long>>`, or `Mono<List<Double>>`
because Redis returns nulls), register it in `RESULT_SPEC` / `FORCE_FLUX_RESULT` /
`VALUE_WRAP` at the top of `CreateReactiveApi.java` before generating.

### 5. Add the implementations

After generation the interfaces declare the new method; now make it real:

**Keyword** — `src/main/java/io/lettuce/core/protocol/CommandType.java`; add the
command name (wire bytes derive from the enum name):

```java
public enum CommandType implements ProtocolKeyword {
    ..., APPEND, GET, GETDEL, ..., STRLEN, LCS, ...
    CommandType() { command = name(); }
}
```

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

**Kotlin coroutine impl** (hand-written, NOT generated) —
`src/main/kotlin/io/lettuce/core/api/coroutines/<Group>CoroutinesCommandsImpl.kt`:

```kotlin
override suspend fun strlen(key: K): Long = ops.strlen(key).awaitSingle()
```

**Cluster** — `RedisClusterAsyncCommands` simply `extends` the group interfaces, so a
single-key command flows through automatically. Only touch
`RedisAdvancedClusterAsyncCommandsImpl` (and its reactive sibling) for special
multi-node routing (fan-out/aggregation).

### 6. Add tests

How tests are named, placed, and run is owned by
[.agents/docs/integration-testing.md](../../../.agents/docs/integration-testing.md) — follow it.
What is specific to a **command**:

- **Builder unit test** (no server): assert the constructed command and its encoded
  args — including the RESP2/3 output shape from §1 — in
  `src/test/java/io/lettuce/core/Redis<Group>CommandBuilderUnitTests.java`.
- **Integration tests**: write the base test against the sync `RedisCommands` API,
  then add the overloads (RESP2 / cluster / reactive / Tx) that matter for the
  command. That base+overload structure is owned by
  the "Command test scope" section of
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
2. **Adding Args/wrapper types after generation.** The generated interfaces reference
   them; if they don't exist first, the project won't compile. Add them upfront.
3. **Editing a generated interface instead of the template.** Anything with
   `@generated` is overwritten. Edit the template and regenerate. (New group? also
   wire it into the hand-written aggregate interfaces.)
4. **Forgetting a dispatch layer.** Update *both* `AbstractRedisAsyncCommands` *and*
   `AbstractRedisReactiveCommands`, plus the Kotlin `*Impl.kt` (not generated).
5. **Wrong `CommandArgs` order / wrong `CommandOutput`**, missing `@since` in the
   template, or missing tests / version gate (`@EnabledOnCommand`).
```
