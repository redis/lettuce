# Lettuce Coding Conventions

Essential rules for working on the Lettuce codebase — a scalable, thread-safe
Redis client for the JVM built on netty and Project Reactor. Detailed guidance
for specific tasks lives in `.agents/skills/`; consult the relevant skill when
performing that type of work.

## General Principles

- **Update documentation.** When changes affect user-facing behavior, config, or
  public API, update the relevant pages under `docs/` (MkDocs).
- **Add or update tests.** Bug fixes need a reproducer test; new features need
  tests. See `docs/integration-testing.md` for unit vs. integration conventions.
- **You are responsible for what you submit.** Validate all changes. Do not submit
  AI-generated code without human oversight.

## Requirements & Toolchain

- Builds and runs on the JDK baseline declared in `pom.xml`
  (`maven.compiler.source`/`target`); targets Redis 2.6+.
- **The test build pins a specific JDK to match CI** — check `.github/workflows/`
  (or `docs/integration-testing.md`) and use that JDK to build and reproduce CI.
- Build tool is **Maven** (`mvn`). Integration tests require **Docker**.

## Project Structure

```
src/main/java/io/lettuce/core/                Core client, connections, commands
src/main/java/io/lettuce/core/cluster/        Redis Cluster support
src/main/java/io/lettuce/core/masterreplica/  Master/Replica & Sentinel
src/main/java/io/lettuce/core/pubsub/         Pub/Sub
src/main/java/io/lettuce/core/support/        Connection pooling (ConnectionPoolSupport, BoundedAsyncPool)
src/main/java/io/lettuce/core/api/            Sync/async/reactive command interfaces
src/test/java/io/lettuce/                      Tests (see docs/integration-testing.md §5)
formatting.xml                                 Eclipse formatter rules
Makefile                                       Docker-backed test lifecycle
```

Where to look: connect → `core/`; pooling → `core/support/`; sharding →
`core/cluster/`; HA/Sentinel → `core/masterreplica/`.

## Build & Test Commands

**Unit tests** (pure logic, no server — named `*UnitTests`, run by Surefire):

```bash
mvn clean test                       # build + run unit tests, no Redis needed
mvn test -Dtest=FooUnitTests         # a single unit test
```

**Integration tests** (need a running Redis — named `*IntegrationTests`, run by
Failsafe; **off by default**, `make test` enables them via `-DskipITs=false`):

```bash
make start                           # boot the Dockerized Redis topology (add version=<v> to pick one)
make test                            # full build incl. integration tests
make stop                            # tear the environment down
```

Run one integration test fast (env already started):

```bash
TEST_WORK_FOLDER=./work/docker mvn -DskipITs=false -Dtest=FooIntegrationTests verify -Pci
```

**Apply formatting** (required before committing):

```bash
mvn formatter:format                 # applies formatting.xml; do NOT submit formatting-only diffs
```

| Flag | Purpose |
|------|---------|
| `-DskipITs=false` | Enable integration tests (`make test` sets this) |
| `-DskipUnitTests=true` | Run integration tests only |
| `-Pci` | CI profile used by `make test` |
| `make start version=<v>` | Pick Redis version; supported list and default are defined in the `Makefile` (`SUPPORTED_TEST_ENV_VERSIONS` / `DEFAULT_TEST_ENV_VERSION`) |

## Testing Rules

- **File name decides the runner, not the folder.** `*UnitTests` → Surefire (no
  server). `*IntegrationTests` → Failsafe (needs Redis). Prefer those suffixes.
- Integration tests obtain host/port/TLS coordinates from `TestSettings` /
  `TlsSettings` (`src/test/java/io/lettuce/test/settings/`) — never hard-code them.
- Full testing infrastructure (Docker image, topologies, TLS/mTLS, CI mapping) is
  documented in **`docs/integration-testing.md`**.

## Coding Style Essentials

- 4-space indentation, enforced by `formatter-maven-plugin` — run
  `mvn formatter:format`. Don't submit formatting-only changes (they create noise
  and get PRs rejected).
- **Add `@since <version>` to all new public API.** Determine the version from the
  current build — run `mvn help:evaluate -Dexpression=project.version -q -DforceStdout`
  (or read the top-level `<version>` in `pom.xml`) and drop the `-SNAPSHOT` suffix
  (e.g. a `7.7.0-SNAPSHOT` build → `@since 7.7`).
- **Add yourself as `@author` to every file you touch.**
- No wildcard imports.

## Commit & PR

- Open a feature-request issue first; reference its number in the PR
  (`<text> #<ticket>`).
- Include unit and/or integration tests for every change.
- Split feature/bugfix commits from formatting/polishing commits.
- Follow `.github/PULL_REQUEST_TEMPLATE.md`.

## Key References

| Document | What it covers |
|----------|----------------|
| `.github/CONTRIBUTING.md` | How to build, test, and contribute |
| `docs/integration-testing.md` | Test environment, topologies, unit vs. integration, CI |
| `.github/PULL_REQUEST_TEMPLATE.md` | PR checklist |
| `formatting.xml` | Eclipse code-formatting rules |
| [Javadoc](https://redis.github.io/lettuce/overview/) | Public API reference |

## On-Demand Skills

Detailed task guidance lives in `.agents/skills/`. Consult the relevant skill
before doing that type of work.

| Skill | When to use |
|-------|-------------|
| `building-and-testing` | Build commands, unit vs. integration tests, Docker test env |
| `writing-tests` | Adding or changing tests; `*UnitTests` vs. `*IntegrationTests` rules |
| `idiomatic-usage` | Writing code that uses Lettuce (thread-safety, pooling, async/reactive) |
| `version-migration` | Upgrading across Lettuce major versions |
| `pr-helper` | Opening a PR: diff summary, PR-template checklist, conventions |
