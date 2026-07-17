---
name: adding-a-redis-command
description: Use when adding a new Redis command — or a new overload/variant of one — to the Lettuce client. Covers wiring it across the command builder, the async and reactive implementations, the API template that generates the sync/async/reactive/Kotlin interfaces, the Kotlin coroutine impl, cluster variants, and integration tests. Trigger on requests like "add support for the <X> command", "implement <REDIS COMMAND> in Lettuce", "wire up a new command", or adding a new argument/overload to an existing command.
---

# Adding a Redis command to Lettuce

This skill is the step-by-step recipe. Read [docs/architecture.md](../../../docs/architecture.md)
first for the model behind it — especially that the sync/async/reactive/Kotlin
**interfaces are generated from templates** and must never be hand-edited.

## Mental model (why the steps are what they are)

- You hand-edit **five** source-of-truth places: the keyword enum, the command
  builder, the async impl, the reactive impl, and the API **template**.
- The sync/async/reactive/Kotlin **interfaces are generated** from the template by
  running JUnit tests — you regenerate them, you don't write them.
- Sync has no impl (it's a dynamic proxy over async); reactive is a *separate*
  hand-written impl. So async and reactive are two impls you must both update.

## Checklist

### 1. Register the command keyword — hand-edit
`src/main/java/io/lettuce/core/protocol/CommandType.java`. Add the command name to
the enum; the wire bytes are derived from the enum name.

```java
public enum CommandType implements ProtocolKeyword {
    ..., APPEND, GET, GETDEL, ..., STRLEN, LCS, ...
    CommandType() { command = name(); }   // bytes come from the enum name
}
```

Argument **modifiers/sub-keywords** (e.g. `REPLACE`, `MATCH`, `WITHVALUES`) go in
`src/main/java/io/lettuce/core/protocol/CommandKeyword.java` instead — **not** in
`CommandType`.

### 2. Add the command builder method — hand-edit
`src/main/java/io/lettuce/core/RedisCommandBuilder.java`. Build the `Command` with
the right `CommandOutput` and, if needed, `CommandArgs`. **Argument order is the
wire order.**

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

- Choose the correct output: `ValueOutput`, `IntegerOutput`, `BooleanOutput`,
  `StatusOutput`, `KeyListOutput`, `MapOutput`, `KeyValueStreamingOutput`, …
- For optional flags, model them as a `*Args implements CompositeArgument` class
  (e.g. `io.lettuce.core.CopyArgs`) whose `build(args)` appends its tokens; the
  tokens are `CommandKeyword` values.

### 3. Add the async dispatch — hand-edit
`src/main/java/io/lettuce/core/AbstractRedisAsyncCommands.java`. Thin delegate:

```java
public RedisFuture<Long> strlen(K key) { return dispatch(commandBuilder.strlen(key)); }
```

### 4. Add the reactive dispatch — hand-edit
`src/main/java/io/lettuce/core/AbstractRedisReactiveCommands.java`. Use `createMono`
for scalar results, `createDissolvingFlux` for `List`/`Set` results (must match the
`Mono`/`Flux` the reactive generator will produce — see step 6):

```java
public Mono<Long> strlen(K key) { return createMono(() -> commandBuilder.strlen(key)); }
public Flux<K> hrandfield(K key, long count) {
    return createDissolvingFlux(() -> commandBuilder.hrandfield(key, count));
}
```

### 5. Add the method to the API template + Javadoc — hand-edit
`src/main/templates/io/lettuce/core/api/<Group>Commands.java` (STRING →
`RedisStringCommands`, HASH → `RedisHashCommands`, generic-key like COPY/DEL →
`RedisKeyCommands`; full list in `apigenerator/Constants.TEMPLATE_NAMES`).

Declare the **sync** return type and write complete Javadoc — this one edit feeds
every generated flavor. Follow the [writing-javadoc](../writing-javadoc/SKILL.md)
skill; new public API needs `@since <version>` plus `@param`/`@return`.

```java
/**
 * Get the length of the value stored in a key.
 *
 * @param key the key.
 * @return Long integer-reply the length of the string at {@code key}, or {@code 0}
 *         when {@code key} does not exist.
 * @since 7.7
 */
Long strlen(K key);
```

Do **not** write the class-level `${intent}` placeholder — the generators substitute it.

### 6. Only for unusual return types — register reactive mapping
Default reactive mapping is `T → Mono<T>`, `List<T>/Set<T> → Flux<T>`. For anything
else (e.g. `Flux<Value<Long>>`, or `Mono<List<Double>>` because Redis returns
nulls), register it in `RESULT_SPEC` / `FORCE_FLUX_RESULT` / `VALUE_WRAP` at the top
of `src/test/java/io/lettuce/apigenerator/CreateReactiveApi.java`.

### 7. Regenerate the interfaces — do NOT hand-edit the outputs
Run the `api_generator`-tagged generators so the interfaces are rewritten:
`CreateSyncApi`, `CreateAsyncApi`, `CreateReactiveApi`, `CreateKotlinCoroutinesApi`,
and the cluster `Create*NodeSelectionClusterApi` classes. These regenerate and
overwrite (all checked in, none hand-edited):

- `src/main/java/io/lettuce/core/api/{sync,async,reactive}/<Group>*Commands.java`
- `src/main/kotlin/io/lettuce/core/api/coroutines/<Group>CoroutinesCommands.kt`
- cluster NodeSelection interfaces

Run a single generator with (adjust class name):
```bash
mvn -Dtest=CreateSyncApi -Dsurefire.failIfNoSpecifiedTests=false test
```

### 8. Add the Kotlin coroutine impl — hand-edit (NOT generated)
`src/main/kotlin/io/lettuce/core/api/coroutines/<Group>CoroutinesCommandsImpl.kt`.
The `.kt` *interface* is generated, but the `*Impl.kt` is hand-written. Bridge from
the reactive result:

```kotlin
override suspend fun strlen(key: K): Long = ops.strlen(key).awaitSingle()
override suspend fun hrandfield(key: K, count: Long): List<K> =
    ops.hrandfield(key, count).asFlow().toList()
```

Use `.awaitFirstOrNull()`/`.awaitSingle()` for `Mono`, `.asFlow().toList()` for `Flux`.

### 9. Cluster variants — only if needed
`RedisClusterAsyncCommands` simply `extends` the per-group async interfaces, so a new
single-key command flows through automatically. Only touch
`RedisAdvancedClusterAsyncCommandsImpl` (and its reactive sibling) if the command
needs special multi-node routing (fan-out / aggregation).

### 10. Integration tests — hand-write
`src/test/java/io/lettuce/core/commands/<Group>CommandIntegrationTests.java`, with
parallel suites under `reactive/`, `transactional/`, and cluster
`io/lettuce/core/cluster/commands/`. Constructor-inject the connection; gate
version-specific commands with `@EnabledOnCommand`:

```java
@Test
void strlen() {
    assertThat((long) redis.strlen(key)).isEqualTo(0);
    redis.set(key, value);
    assertThat((long) redis.strlen(key)).isEqualTo(value.length());
}

@Test
@EnabledOnCommand("COPY")
void copy() {
    redis.set(key, value);
    assertThat(redis.copy(key, key + "2")).isTrue();
}
```

Run one integration test (env already started via `make start`):
```bash
TEST_WORK_FOLDER=./work/docker mvn -DskipITs=false -DskipUnitTests=true \
  -Dit.test=StringCommandIntegrationTests verify -Pci
```

## Top pitfalls

1. **Editing a generated interface instead of the template.** Anything with
   `@generated` is overwritten on the next generator run. Edit the template
   (`src/main/templates/…`) and regenerate.
2. **Forgetting a dispatch layer.** You must update *both* `AbstractRedisAsyncCommands`
   *and* `AbstractRedisReactiveCommands`, plus the Kotlin `*Impl.kt` (not generated).
3. **Wrong `CommandArgs` order or wrong `CommandOutput`.** Builder order is the wire
   order; a mismatched output type yields protocol/type errors.
4. **Missing `@since` / incomplete Javadoc in the template** — it multiplies across
   every generated flavor.
5. **Missing tests / no version gate.** Add tests in the relevant suites and gate
   commands not present on all supported servers with `@EnabledOnCommand`.
