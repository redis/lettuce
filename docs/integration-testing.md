# Integration Testing Infrastructure

This document describes how Lettuce integration tests are wired up: the Redis
test environment, how it is started, how tests discover their servers, where to
place unit vs. integration tests, and how this maps to CI.

## Tips & tricks

Tests do not start Redis. They connect to servers that are already running. The
servers are defined by `src/test/resources/docker-env/docker-compose.yml`,
started via `make start` (`Makefile`), and addressed by tests through fixed
host/port/socket values resolved by `TestSettings`
(`src/test/java/io/lettuce/test/settings/TestSettings.java`) and overridable with
Maven `-D` properties.

- **Run integration tests locally**: `make start version=8.6 && make test && make stop`.
- **Run one test fast**: `make start version=8.6` then
  `TEST_WORK_FOLDER=./work/docker mvn -DskipITs=false -Dit.test=YourIntegrationTests verify -Pci`
  (integration tests run under Failsafe, which filters on `-Dit.test`, **not** `-Dtest`)
  (then `make stop`).
- **Add a new server**: add a service to
  `src/test/resources/docker-env/docker-compose.yml` (set
  `REDIS_CLUSTER`/`TLS_ENABLED`/`TLS_CLIENT_CNS`/ports/volumes), then point tests
  at it via `TestSettings` / the relevant `-D` property.
- **Unit vs. integration is decided by the file name**: `*UnitTests` → Surefire
  (no server), `*IntegrationTests` → Failsafe (needs a server). See §5.
- **TLS in a test**: server/CA certs come from `<TEST_WORK_FOLDER>/<container>/work/tls/`
  (`ca.crt`, `redis.crt`); mTLS clients use the generated `*.p12` (see
  `TlsSettings`).
- **Integration tests didn't run**: they default to skipped — pass
  `-DskipITs=false` (which `make test` already does).
- **Reproducing CI**: the test build pins **Java 8**.

### Local gotchas

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

---

## 1. The test environment is provided by a shared container

All Redis servers used in tests run inside the **`redislabs/client-libs-test`**
Docker image, published publicly on
[Docker Hub](https://hub.docker.com/r/redislabs/client-libs-test). The same image
is used across several Redis client libraries (Lettuce, Jedis, redis-py,
node-redis, go-redis, …).

The image wraps a chosen Redis version and bootstraps a **standalone**,
**replication + sentinel**, or **cluster** topology — optionally with
**TLS / mTLS** and modules — entirely from environment variables. Tags follow the
Redis version (e.g. `redislabs/client-libs-test:8.6`). It is **not** for
production use.

You normally never run it by hand — `make start` brings up the whole topology via
Docker Compose (see §4). But the image is self-contained, so you can launch a
single instance directly to experiment:

```bash
# standalone on :6479 (Lettuce's default test port)
docker run --rm -p 6479:6479 -e PORT=6479 redislabs/client-libs-test:8.6
# 3-node cluster on :7000-7002
docker run --rm -p 7000-7002:7000-7002 \
  -e REDIS_CLUSTER=yes -e NODES=3 -e PORT=7000 redislabs/client-libs-test:8.6
```

### 1.1 Container behaviour

| Concern          | How it works                                                                                                                                                                                                                                   |
|------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Base image       | `redis:<tag>` (OSS) or `redis/redis-stack:<tag>` (with modules), selectable at build time via `BASE_IMAGE` / `BASE_IMAGE_TAG`.                                                                                                                 |
| Topology         | `REDIS_CLUSTER=yes` → cluster (auto-bootstrapped with `redis-cli --cluster create`); otherwise standalone/replicated. `NODES=N` sets node count (cluster default 3, standalone 1). `REPLICAS=N` sets replicas per cluster master.              |
| Ports            | Non-TLS nodes: `PORT, PORT+1, …` (default `PORT=3000`). TLS nodes: `TLS_PORT, TLS_PORT+1, …` (default `TLS_PORT=4430`).                                                                                                                        |
| Auth             | `REDIS_PASSWORD` → `requirepass`+`masterauth`. `REDIS_CLIENT_USER`/`REDIS_CLIENT_PASSWORD` are the credentials the container itself uses to create/check the cluster.                                                                          |
| Protected mode   | `PROTECTED_MODE=yes` sets Redis `protected-mode` (default `no`).                                                                                                                                                                               |
| TLS              | `TLS_ENABLED=yes` auto-generates a self-signed CA, a server cert, and client certs into `/redis/work/tls/`, and enables a TLS port per node.                                                                                                   |
| mTLS             | `TLS_CLIENT_CNS="cn1 cn2 …"` generates one client cert (+ `.p12`) per CN. `TLS_AUTH_CLIENTS_USER=CN` maps the cert CN to a Redis ACL user (Redis ≥ 8.6); `off` disables client-cert auth.                                                      |
| Modules          | When the base image ships modules (e.g. `redis/redis-stack:<tag>`), they are auto-loaded; matching `…-stack` style tags exist for module testing.                                                                                              |
| Extra directives | Any trailing args become `redis-server` flags (e.g. `--maxmemory 256mb`). A few are container-managed and cannot be overridden: `--port`, `--dir`, `--logfile`, `--pidfile`, `--cluster-enabled`, `--cluster-config-file`, `--protected-mode`. |

### 1.2 Volumes & directory conventions

| Mount                              | Purpose                                                                                                                                                                                                                                                                     |
|------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `-v <host>/config:/redis/config:r` | **Input.** Pre-baked per-node config. Node dirs must be named `node-<port>[-<tlsport>]`; a sentinel node dir must be named `node-sentinel-<...>`. Each may contain `redis.conf` and (for cluster) `nodes.conf`. Optional `tls/` subdir supplies pre-generated certificates. |
| `-v <host>/work:/redis/work:rw`    | **Output.** Runtime state: `node-<i>/{redis.conf,redis.log,redis.pid,nodes.conf}` and generated `tls/` (`ca.crt`, `ca.key`, `redis.crt`, `redis.key`, `<cn>.p12`, …). **Tests read TLS certs from here.**                                                                   |

### 1.3 Generated TLS material

When `TLS_ENABLED=yes`, the container writes these files to its
`/redis/work/tls/` directory (surfaced on the host under the `work/` mount — see
§1.2 — which is where the tests read them from):

| File                               | What it is                                              |
|------------------------------------|---------------------------------------------------------|
| `ca.crt`, `ca.key`                 | Self-signed test CA (signs everything else).            |
| `redis.crt`, `redis.key`           | Server certificate (CN `localhost`).                    |
| `<cn>.crt`, `<cn>.key`, `<cn>.p12` | One client cert per name in `TLS_CLIENT_CNS`, for mTLS. |

The PKCS#12 keystores (`*.p12`) use the password **`changeit`**. Certs are
generated once and reused on container restart.

### 1.4 "Endpoint" definition

An **endpoint** is a `host:port` pair for one Redis node (`127.0.0.1:6479`
non-TLS, `127.0.0.1:6443` TLS in Lettuce's default standalone setup). For
cluster, every node is an endpoint, and the client discovers the rest via
`CLUSTER NODES`.

---

## 2. How Lettuce defines its environment

Everything lives under **`src/test/resources/docker-env/`**.

```
src/test/resources/docker-env/
├── docker-compose.yml         # all Redis services
├── .env                       # base vars (REDIS_VERSION, REDIS_STACK_VERSION, REDIS_ENV_WORK_DIR)
├── .env.v7.2 … .env.v8.8      # per-version overrides (pin REDIS_VERSION)
└── <env>/config/node-<port>/redis.conf   # pre-baked per-node configs (mounted :r)
```

Base `.env`:

```
REDIS_VERSION=8.8.0
REDIS_STACK_VERSION=8.8.0
REDIS_ENV_WORK_DIR=../../../../work/docker
```

The compose file provides the full matrix: several standalone instances (plain,
TLS, and `redis-standalone-5-client-cert` for mTLS), a sentinel-controlled
replication set, Redis-Stack (standalone + clustered), a TLS/mTLS cluster
(`ssl-test-cluster`), a plain `test-cluster`, Toxiproxy (network fault
injection), and a one-shot `cleanup` service (compose profile `cleanup`) that
wipes the work dir before a run.

> **Unix sockets.** Some tests use Unix-domain sockets. The Makefile passes their
> paths as Maven properties:
> `-Ddomainsocket="$WORK/socket-6482" -Dsentineldomainsocket="$WORK/socket-26379"`.

---

## 3. How tests discover servers

There is no endpoints registry file; tests read fixed connection coordinates from
helper classes under `src/test/java/io/lettuce/test/settings/`, every value
overridable with a system property (`-D…`):

| Setting         | Accessor (`TestSettings`)         | Default                             | Property                            |
|-----------------|-----------------------------------|-------------------------------------|-------------------------------------|
| Host            | `host()` / `hostAddr()`           | `localhost`                         | `-Dhost`                            |
| Port            | `port()`                          | `6479`                              | `-Dport`                            |
| TLS port        | `sslPort()`                       | `6443`                              | `-Dsslport`                         |
| Password        | `password()`                      | `foobared`                          | `-Dpassword`                        |
| ACL user / pass | `aclUsername()` / `aclPassword()` | `lettuceTest` / `lettuceTestPasswd` | `-Dacl.username` / `-Dacl.password` |
| Unix socket     | `socket()`                        | `work/socket-6482`                  | `-Ddomainsocket`                    |
| Sentinel socket | `sentinelSocket()`                | `work/socket-26379`                 | `-Dsentineldomainsocket`            |

Cluster- and sentinel-specific ports live in `ClusterTestSettings` and
`SentinelTestSettings`.

**TLS** is handled by `TlsSettings`
(`src/test/java/io/lettuce/test/settings/TlsSettings.java`): it builds a PKCS#12
truststore from the certs the container generated under
`<TEST_WORK_FOLDER>/<container>/work/tls/` (`ca.crt`, `redis.crt`), where
`TEST_WORK_FOLDER` defaults to `work/docker`. For **mTLS**, the `ClientCertificate`
enum maps client `*.p12` files (e.g. `Client-test-cert.p12`, `Client-test-2.p12`,
`client.p12`) to ACL/no-ACL test scenarios; the relevant containers are
`redis-standalone-5-client-cert` and `ssl-test-cluster`.

When changing ports or adding a server, update **both** the compose service and
the matching `TestSettings`/`-D` value so they stay in sync.

---

## 4. Running the tests (Makefile)

| Command                  | Effect                                                                                                                                                        |
|--------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `make start version=8.6` | runs the `cleanup` container, then `docker compose … up -d --wait` with `.env` + `.env.v8.6`. Supported: 7.2, 7.4, 8.0, 8.2, 8.4, 8.6, 8.8 (default **8.8**). |
| `make stop`              | `docker compose … down`.                                                                                                                                      |
| `make test`              | `TEST_WORK_FOLDER=$WORK mvn -DskipITs=false <socket args> clean compile verify -P$(PROFILE)` (`PROFILE` default `ci`). Assumes the env is already started.    |
| `make test-coverage`     | as `make test` plus `jacoco:report`.                                                                                                                          |

Custom image instead of a version: `make start CLIENT_LIBS_TEST_IMAGE_TAG=<tag>`.
Extra Maven args: `make test MVN_EXTRA_ARGS="-DskipUnitTests=true"`.

### Test selection (file-name conventions + Maven, see `pom.xml`)

Lettuce splits unit and integration tests by **file name**, not by JUnit tag:

| Group       | File name                                             | Runner   | Default                                                                   |
|-------------|-------------------------------------------------------|----------|---------------------------------------------------------------------------|
| Unit        | `*UnitTests` (preferred) or `*Tests`                  | Surefire | run (`skipUnitTests=${skipTests}`)                                        |
| Integration | `*IntegrationTests` (preferred) or `*Test` (singular) | Failsafe | **off by default** (`skipITs=true`); `make test` passes `-DskipITs=false` |

Surefire additionally excludes the `integration` JUnit group; a few JUnit
categories exist for special runs (e.g. `@SlowTests`, and `entraid`/`scenario`
selected by dedicated Maven profiles).

Useful flags: `-DskipUnitTests=true` (run integration only),
`-DskipITs=false` (enable integration tests), `-Pci`.

---

## 5. Test source layout & where to put tests

All test code lives under **`src/test/java/`**, predominantly in the
`io.lettuce` tree:

```
src/test/java/io/lettuce/
├── core/              # the bulk of the tests, grouped by area:
│   ├── commands/      #   command coverage
│   ├── cluster/       #   Redis Cluster
│   ├── sentinel/      #   Sentinel
│   ├── masterreplica/ masterslave/   #   topology / replication
│   ├── pubsub/  reactive/  dynamic/  #   API surfaces
│   ├── json/  search/  vector/  bf/  datastructure/   # data types & modules
│   ├── protocol/ codec/ output/ resource/ metrics/ event/ tracing/
│   └── reliability/ failover/ support/ models/ internal/
├── test/              # test infrastructure — NOT tests themselves
│   ├── settings/      #   TestSettings, TlsSettings, …
│   ├── resource/  server/  env/  condition/
├── authx/             # authentication / token-based auth
├── scenario/          # long-running scenario tests (dedicated profile)
├── category/          # JUnit category markers (e.g. SlowTests)
├── examples/          # runnable examples — not part of the normal test run
├── apigenerator/      # API code generation
└── codec/
io/redis/examples/                # doctest-style async/reactive examples
biz/paluch/redis/extensibility/   # legacy extensibility demos/tests
```

### Unit vs. integration: the rule

What decides whether a test is run by Surefire (unit) or Failsafe (integration)
is its **file-name suffix** (see §4 and `pom.xml`), not which folder it sits in.

| Test needs…                       | Name it…                                                  | Runner   |
|-----------------------------------|-----------------------------------------------------------|----------|
| **No server** (pure logic, mocks) | `FooUnitTests` (preferred) or `FooTests`                  | Surefire |
| **A running Redis**               | `FooIntegrationTests` (preferred) or `FooTest` (singular) | Failsafe |

Guidance for new tests:

- **Unit test** → name it `FooUnitTests`, keep it free of any running-server
  dependency, and place it next to the area it covers (e.g.
  `io/lettuce/core/protocol/…`). It must pass with no Redis running and must not
  carry the `integration` JUnit group.
- **Integration test** → name it `FooIntegrationTests`, place it in the area
  package it exercises (`core/commands/`, `core/cluster/`, `core/sentinel/`, …),
  and obtain connection coordinates from `TestSettings` / `TlsSettings` (§3)
  rather than hard-coding hosts and ports.
- Shared fixtures and helpers belong under `io/lettuce/test/…`; they are not
  collected as tests. Keep runnable samples in `examples/` (excluded from the
  normal test run).

---

## 6. Command test scope: base tests and their overloads

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
follows §4/§5 (`*IntegrationTests` → Failsafe).

Not every command needs every overload — add the ones that carry real risk for that
command (e.g. RESP2 when the reply shape differs between protocols, cluster when
routing matters). Provide the base test at minimum.

---

## 7. CI workflows (`.github/workflows/`)

| Workflow                   | What it does                                                                                                                                                                                                                             |
|----------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `integration.yml`          | Matrix over Redis 7.2 → 8.8. Calls the reusable `run-tests.yml`. A separate "custom image" job passes `client_libs_test_image_tag` with `skip_unit_tests: true`, `upload_coverage: false`.                                               |
| `run-tests.yml` (reusable) | Java 8 (Temurin); `make start version=<v>`; then `TEST_WORK_FOLDER=$REDIS_ENV_WORK_DIR` and either `make test-coverage` (default) or `make test MVN_EXTRA_ARGS="-DskipUnitTests=true"` when `skip_unit_tests=true`; finally `make stop`. |

The test build pins **Java 8** — use it when reproducing CI locally.
