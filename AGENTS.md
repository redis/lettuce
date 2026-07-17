# Lettuce Coding Conventions

Essential rules for working on the Lettuce codebase — a scalable, thread-safe
Redis client for the JVM built on netty and Project Reactor. Detailed guidance
for specific tasks lives in `.agents/skills/`; consult the relevant skill when
performing that type of work.

## How this guidance is organized

Agent guidance is **self-contained under agent-owned locations** and never mixed into
the published documentation site:

- **Agent-facing** (this material) lives at the repo root (`AGENTS.md`, `CLAUDE.md`),
  under `.agents/` (tool-neutral: `.agents/docs/`, `.agents/skills/`), and `.claude/`
  (Claude-specific tooling). None of it is published by MkDocs.
- **User-facing** contributor/user documentation lives under `docs/` and is published
  to the docs site. Agents read the agent-owned copies, not `docs/`.

Within the agent material it's a small graph: **each topic has one owner file**, and
everything else **links** to it rather than restating it. When a topic changes, edit
the owner only.

| Topic | Owner |
|-------|-------|
| Always-on facts, guardrails, index (this file) | `AGENTS.md` |
| Command flow & code generation | `.agents/docs/architecture.md` |
| Testing — environment, running, naming, layout, CI | `.agents/docs/integration-testing.md` |
| Javadoc conventions (incl. `@since` derivation) | `.agents/docs/javadoc.md` |
| Task procedures (add a command, write Javadoc, PR description) | `.agents/skills/*` |

Rule: state a fact in its owner and link from elsewhere; don't copy it. This file
keeps only a minimal always-on quick-reference. Skills own the *sequence* of a task
and link out for the mechanics.

## General Principles

- **Update documentation.** When changes affect user-facing behavior, config, or
  public API, update the relevant pages under `docs/` (MkDocs).
- **Add or update tests.** Bug fixes need a reproducer test; new features need
  tests. See `.agents/docs/integration-testing.md` for unit vs. integration conventions.
- **You are responsible for what you submit.** Validate all changes. Do not submit
  AI-generated code without human oversight.

## Guardrails (agents)

These are hard rules for automated agents. A human maintainer performs these
actions, not the agent.

- **Never `git commit` or `git push`.** Stage and describe changes; leave
  committing and pushing to the maintainer.
- **Never create or merge pull requests, and never reply to GitHub issues or PRs**
  (no `gh pr create`/`merge`/`comment`/`review`, no `gh issue create`/`comment`).
  Draft text for the maintainer to post if asked — for a PR description, use the
  `create-pr-description` skill, which writes to `prDescription.md`.

> These rules are also enforced by a `PreToolUse` hook (`.claude/settings.json` →
> `.claude/hooks/block-git-write-ops.sh`) that blocks the matching commands.

## Requirements & Toolchain

- Builds and runs on the JDK baseline declared in `pom.xml`
  (`maven.compiler.source`/`target`); targets Redis 2.6+.
- **The test build pins a specific JDK to match CI** — check `.github/workflows/`
  (or `.agents/docs/integration-testing.md`) and use that JDK to build and reproduce CI.
- Build tool is **Maven** (`mvn`). Integration tests require **Docker**.

## Project Structure

```
src/main/java/io/lettuce/core/                Core client, connections, commands
src/main/java/io/lettuce/core/cluster/        Redis Cluster support
src/main/java/io/lettuce/core/masterreplica/  Master/Replica & Sentinel
src/main/java/io/lettuce/core/pubsub/         Pub/Sub
src/main/java/io/lettuce/core/support/        Connection pooling (ConnectionPoolSupport, BoundedAsyncPool)
src/main/java/io/lettuce/core/api/            Sync/async/reactive command interfaces
src/test/java/io/lettuce/                      Tests (see .agents/docs/integration-testing.md §5)
formatting.xml                                 Eclipse formatter rules
Makefile                                       Docker-backed test lifecycle
```

Where to look: connect → `core/`; pooling → `core/support/`; sharding →
`core/cluster/`; HA/Sentinel → `core/masterreplica/`.

**How a command flows** through the API interfaces, code generation, command
builder, codec, and the netty/RESP wire layer is documented in
`.agents/docs/architecture.md`. Read it before adding commands or touching the protocol
layer — note the sync/async/reactive command interfaces are **generated** from
templates in `src/main/templates/` and must not be hand-edited.

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
TEST_WORK_FOLDER=./work/docker mvn -DskipITs=false -DskipUnitTests=true \
  -Dit.test=FooIntegrationTests verify -Pci
```

One caveat worth remembering: `-Dit.test=` filters Failsafe (a single integration
test); `-DskipUnitTests=true` runs integration only. The full flag reference,
Redis-version matrix, topologies, TLS/mTLS and CI mapping live in
[`.agents/docs/integration-testing.md`](.agents/docs/integration-testing.md).

**Apply formatting** (required before committing):

```bash
mvn formatter:format                 # applies formatting.xml; do NOT submit formatting-only diffs
```

## Testing Rules

- **The file-name suffix picks the runner:** `*UnitTests` (Surefire, no server),
  `*IntegrationTests` (Failsafe, needs Redis). Prefer those suffixes.
- Integration tests read host/port/TLS from `TestSettings` / `TlsSettings` — never
  hard-code them.
- Everything else about testing (topologies, TLS/mTLS, layout, CI) lives in
  [`.agents/docs/integration-testing.md`](.agents/docs/integration-testing.md).

## Coding Style Essentials

- 4-space indentation, enforced by `formatter-maven-plugin` — run
  `mvn formatter:format`. Don't submit formatting-only changes (they create noise
  and get PRs rejected).
- **Add `@since <version>` to all new public API** — see
  [`.agents/docs/javadoc.md`](.agents/docs/javadoc.md) for the version-derivation recipe.
- **Add yourself as `@author` to every file you touch.**
- **Prefer imports over fully-qualified class names** in code; add an `import`
  rather than inlining a package-qualified name (except where needed to
  disambiguate a genuine name clash). No wildcard imports.
- Javadoc for public API follows house conventions — see `.agents/docs/javadoc.md`, or use
  the `writing-javadoc` skill.

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
| `.agents/docs/architecture.md` | How a command flows: API shapes, code generation, command model, codec, RESP/netty wire layer |
| `.agents/docs/integration-testing.md` | Test environment, topologies, unit vs. integration, CI |
| `.agents/docs/javadoc.md` | Javadoc house conventions for public API |
| `.github/PULL_REQUEST_TEMPLATE.md` | PR checklist |
| `formatting.xml` | Eclipse code-formatting rules |
| [Javadoc](https://redis.github.io/lettuce/overview/) | Public API reference |

## On-Demand Skills

Detailed task guidance lives in `.agents/skills/`. Consult the relevant skill
before doing that type of work.

| Skill | When to use |
|-------|-------------|
| `adding-a-redis-command` | Add a new Redis command (or overload) end-to-end: builder, async/reactive impls, API template + generated interfaces, Kotlin, tests |
| `writing-javadoc` | Write or fix Javadoc for public API following Lettuce house conventions |
| `create-pr-description` | Generate a GitHub PR title and description from the diff between two local branches |
