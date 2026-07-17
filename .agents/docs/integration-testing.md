# Integration testing — agent notes

The canonical integration-testing guide is
[docs/integration-testing.md](../../docs/integration-testing.md) (published): the
Docker test image, the environment and how it starts, how tests discover their
servers, the Makefile lifecycle (`make start` / `make test` / `make stop`), test
layout, and CI. **Read that first.** This file adds only the extras that matter when
an agent works on the codebase.

## Running one test correctly

Integration tests run under **Failsafe**, which filters on **`-Dit.test`**, *not*
`-Dtest` (a `-Dtest=` value narrows the Surefire/unit phase and leaves the whole
integration suite running). Narrow to a single class or a single method:

```bash
-Dit.test=StringCommandIntegrationTests            # one class
-Dit.test='StringCommandIntegrationTests#append'   # one method
-Dit.test='StringCommandIntegrationTests#append+get'   # several (+-separated)
```

## Local gotchas

A few environment issues that stop the build before any test runs — especially in a
**git worktree** or on a machine whose default JDK is newer than 8:

- **Pin `JAVA_HOME` to Java 8.** With `JAVA_HOME` unset, the Kotlin compile step can
  pick up a newer JDK via `/usr/libexec/java_home` and fail (its bundled tooling
  can't parse newer version strings). Use the JDK CI pins:
  `JAVA_HOME=$(/usr/libexec/java_home -v 1.8)`.
- **Git worktree + `git-commit-id-plugin`.** In a worktree `.git` is a file, not a
  directory, and the plugin fails with *"Could not get HEAD Ref"*. Skip it with
  `-Dmaven.gitcommitid.skip=true` (it has no effect on tests).
- **Corrupted Kotlin incremental cache** after an aborted build
  (`PersistentEnumerator storage corrupted …/target/kotlin-ic/…`): delete it with
  `rm -rf target/kotlin-ic`, or run `mvn clean`.
- **`TEST_WORK_FOLDER` must match `make start`.** Point it at the directory
  `make start` prints as its work dir (TLS certs and sockets are resolved from
  there); a mismatch surfaces as TLS/socket test failures.

A full single-test invocation from a worktree, combining the above:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 1.8) TEST_WORK_FOLDER=$PWD/work \
  mvn -DskipITs=false -DskipUnitTests=true -Dmaven.gitcommitid.skip=true \
  -Dit.test=YourIntegrationTests verify -Pci
```

## Command test scope: base tests and their overloads

Command coverage is written **once** against the synchronous `RedisCommands` API in a
**base** integration test, then **re-run** through several execution paths by thin
subclasses ("overloads"). Each overload extends the base and passes a
differently-backed `RedisCommands` facade to the base constructor, so the same
`@Test` methods exercise a different code path without being duplicated.

### The base test

- Targets `io.lettuce.core.api.sync.RedisCommands<String, String>`, injected through a
  **`protected`** constructor so overloads can substitute the backend.
- Holds all the `@Test` methods and gates the group with `@EnabledOnCommand("<CMD>")`,
  `@Tag(INTEGRATION_TEST)`, `@ExtendWith(LettuceExtension.class)`,
  `@TestInstance(PER_CLASS)`.

```java
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
@EnabledOnCommand("ARSET")
public class RedisArrayIntegrationTests extends TestSupport {

    protected final RedisCommands<String, String> redis;

    @Inject
    protected RedisArrayIntegrationTests(RedisCommands<String, String> redis) {
        this.redis = redis;
    }
    // @Test methods live here
}
```

### The overloads

Each overload `extends` the base and supplies a `RedisCommands` facade with a
different backend:

| Overload | Backing facade | What it verifies |
|----------|----------------|------------------|
| **RESP2** | `client.setOptions(protocolVersion(RESP2))`, then `client.connect().sync()` | the command under RESP2 (the base runs the RESP3 default) |
| **Cluster** | `ClusterTestUtil.redisCommandsOverCluster(clusterConnection)` | behavior and slot routing over Redis Cluster |
| **Reactive** | `ReactiveSyncInvocationHandler.sync(connection)` (`io.lettuce.test`) | every call routed through the reactive API |
| **Transactional (Tx)** | `TxSyncInvocationHandler.sync(connection)` (`core/commands/transactional/`) | the command inside `MULTI`/`EXEC` |

The reactive and Tx facades are **JDK dynamic proxies** that implement `RedisCommands`
by delegating to the reactive/transactional API and blocking — the same
sync-over-async idea as the production `sync()` proxy (see
[architecture.md](architecture.md)). That is what lets one base test run unchanged
through every path.

```java
// RESP2 — re-run all base tests under RESP2
public class RedisArrayResp2IntegrationTests extends RedisArrayIntegrationTests {
    @Inject
    RedisArrayResp2IntegrationTests(RedisClient client) { super(connectWithResp2(client)); }

    private static RedisCommands<String, String> connectWithResp2(RedisClient client) {
        client.setOptions(ClientOptions.builder().protocolVersion(ProtocolVersion.RESP2).build());
        return client.connect().sync();
    }
}

// Cluster
public class RedisArrayClusterIntegrationTests extends RedisArrayIntegrationTests {
    @Inject
    RedisArrayClusterIntegrationTests(StatefulRedisClusterConnection<String, String> connection) {
        super(ClusterTestUtil.redisCommandsOverCluster(connection));
    }
}

// Reactive
public class RedisArrayReactiveIntegrationTests extends RedisArrayIntegrationTests {
    @Inject
    RedisArrayReactiveIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(ReactiveSyncInvocationHandler.sync(connection));
    }
}

// Transactional (MULTI/EXEC)
class ArrayTxCommandIntegrationTests extends RedisArrayIntegrationTests {
    @Inject
    ArrayTxCommandIntegrationTests(StatefulRedisConnection<String, String> connection) {
        super(TxSyncInvocationHandler.sync(connection));
    }
}
```

### Overriding when a path's semantics differ

An overload can `@Override` an individual base test when the API contract genuinely
differs on that path. For example, the reactive Array tests override `armget` /
`argetrange` because the reactive API returns `Flux<Value<V>>` (nulls wrapped as
`Value.empty()`) while the sync API returns `List<V>` with raw nulls.

### Where the files go

Placement varies by area. Newer command areas keep the overloads together in the
area package (e.g. all of `RedisArray*IntegrationTests` under `core/array/`); the
classic layout splits them out (`core/commands/` for base + RESP2, `core/commands/reactive/`,
`core/commands/transactional/`, and `core/cluster/commands/` for cluster). Naming
follows the `*IntegrationTests` → Failsafe convention (see the published guide).

Not every command needs every overload — add the ones that carry real risk for that
command (e.g. RESP2 when the reply shape differs between protocols, cluster when
routing matters). Provide the base test at minimum.
