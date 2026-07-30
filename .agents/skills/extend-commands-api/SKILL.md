---
name: extend-commands-api
description: Add or extend Redis commands in the Lettuce client API end-to-end — a new core command, a family of new commands, an extension to an existing command's options, or a module/area command (Search/JSON/Bloom/VectorSet). Gathers evidence first (HLD document, the server-side PR in the repo owning the command family, live verification against the Dockerized test environment with redis-cli), plans the full implementation matrix in plan mode, then implements across all API flavors with unit and integration tests. Trigger on "add support for the <X> command", "implement <REDIS COMMAND> in Lettuce", "extend <command> with <option>", or adding a new argument/overload to an existing command.
allowed-tools: Bash(mvn *), Bash(make *), Bash(redis-cli *), Bash(gh *)
---

# Extend the Lettuce Commands API

Implement a new Redis command (or extend an existing one) in Lettuce, following the
conventions maintainers enforce in review. The workflow is evidence-first: prove the
command's behavior on a real server before designing the Java API.

Read [.agents/docs/architecture.md](../../../.agents/docs/architecture.md) first for
the model behind this flow — especially that the sync/async/reactive/Kotlin command
**interfaces are hand-edited source files kept in lockstep by the API consistency
test suite** (see [.agents/docs/api-consistency.md](../../../.agents/docs/api-consistency.md)).

> **Historical caveat when reading old PRs**: PRs merged before the generator removal
> also edit `src/test/java/io/lettuce/core/api/<Group>Commands.java` — the old
> template source files. Those files **no longer exist**; do not recreate them. The
> flavor interfaces are now edited directly.

## Phase 0 — Gather evidence BEFORE planning

Do all of the following before writing any plan or code:

1. **Ask the user for the HLD.** Interactively ask for the path to a markdown file
   containing the High-Level Design for the command(s) (or confirm none exists). If
   a path is given, read it fully — it is the primary source for syntax, semantics,
   reply shape per RESP2/RESP3, and edge cases.

2. **Find the server-side PR in the repo that owns the command.** Route the
   search by command family — module command families are developed in their
   owning repositories, not in `redis/redis`:

   | Command family | Repository | Syntax source |
   |----------------|------------|---------------|
   | Core commands, vector sets (`VADD`, …) | `redis/redis` | `src/commands/*.json` |
   | Search (`FT.*`) | `RediSearch/RediSearch` | PR diff + command docs (no `src/commands/*.json`) |
   | JSON (`JSON.*`) | `RedisJSON/RedisJSON` | PR diff + command docs |
   | Probabilistic (`BF.*`, `CF.*`, `CMS.*`, `TOPK.*`, `TDIGEST.*`) | `RedisBloom/RedisBloom` | PR diff + command docs |
   | Time series (`TS.*`) | `RedisTimeSeries/RedisTimeSeries` | PR diff + command docs |

   If the search comes up empty in the routed repo, fall back to `redis/redis`
   (and vice versa) before concluding there is no server PR.

   First verify that `gh` works in the current (sandboxed) environment:
   ```bash
   gh auth status
   ```
   Sandboxes often block access to credential files, so `gh` may report
   unauthenticated here even though it works on the user's machine. If so, ask the
   user for permission to run these specific read-only `gh` commands outside the
   sandbox; only fall back to the unauthenticated GitHub REST API if they decline.

   Then search for the PR that adds/extends the command on the server:
   ```bash
   gh search prs --repo <owning-repo> "<COMMAND NAME>" --limit 10
   gh pr view <num> --repo <owning-repo>
   gh pr diff <num> --repo <owning-repo>  # in redis/redis: src/commands/*.json has exact syntax
   ```
   Extract: exact wire syntax (argument order and optionality), reply type per
   **RESP2 and RESP3** (they can differ — this determines the `CommandOutput` and
   the reactive `Mono`/`Flux` mapping), error conditions, and the **first server
   version carrying the feature** (drives `@EnabledOnCommand` gating and the test
   env version). Note whether the server marks the feature *experimental/preview*.

3. **Verify the command exists on the "next" Redis OSS version.** Start the
   integration environment using the **highest** version the Makefile supports
   (check `SUPPORTED_TEST_ENV_VERSIONS` in the `Makefile`, or the
   `src/test/resources/docker-env/.env.vX.XX` files — pick numerically, `8.10` >
   `8.8`):
   ```bash
   grep SUPPORTED_TEST_ENV_VERSIONS Makefile
   make start version=8.10
   ```
   Then probe the running servers. Test connection defaults live in
   `src/test/java/io/lettuce/test/settings/TestSettings.java` — the main standalone
   node is `localhost:6479` (no auth); module commands (`FT.*`, `JSON.*`, `BF.*`,
   `VADD`, …) run on the stack node at `localhost:16379`:
   ```bash
   redis-cli -p 6479 INFO server | grep redis_version
   redis-cli -p 6479 COMMAND INFO <COMMAND>   # non-empty → command exists
   redis-cli -p 6479 COMMAND DOCS <COMMAND>   # arity/args — compare with the server PR
   ```
   For a NEW command, `COMMAND INFO` must return a non-empty reply. For an EXTENDED
   command, additionally invoke it with the new syntax against a scratch key and
   confirm the server accepts it (no `ERR syntax error` / `ERR unknown argument`).

   **If the command/option is missing on the latest env version**, the image tag
   pinned in the `.env.vX.XX` file is too old. Ask the user for a
   `redislabs/client-libs-test` image tag that contains it (e.g. an RC/edge build —
   tags are listed at https://hub.docker.com/r/redislabs/client-libs-test/tags; you
   may check and suggest candidates), then:
   ```bash
   make stop
   CLIENT_LIBS_TEST_IMAGE_TAG=<tag> make start
   ```
   If the PR should also move CI to that build, update the newest `.env.vX.XX`
   pin as part of the change — command PRs do this when they need a fresh server
   (cf. the hotkeys PR bumping `.env.v8.6`). If no tag carries the feature, report
   it and proceed: implementation continues, integration tests stay gated until an
   image ships the change. Keep the environment running for later test runs.

4. **Create redis-cli showcase test cases.** Once the command is available, derive
   a small set of redis-cli scenarios from the HLD and the server PR and run them.
   These serve two purposes:
   - **Smoke test**: prove the documented behavior holds — happy path, each new
     option/keyword, reply shape (run with `redis-cli -3` too when RESP3 replies
     differ), edge cases and error conditions (missing key, out-of-range args,
     conflicting options) — so the Java implementation is built against observed
     replies, not assumptions.
   - **Showcase**: each scenario should read as a mini use-case explaining WHY the
     command/option exists. Prefer realistic data over `foo`/`bar`.

   Save the scenarios as a commented script in a scratch file (one block per
   use-case: a "what this demonstrates" comment, the commands, the observed reply
   pasted back as comments). Carry this forward: it feeds the Phase 1 plan, the
   test assertions, and the PR description. If the command could not be made
   available on any image, still write the scenarios as *expected* transcripts and
   mark them unverified.

5. **Read the testing and consistency docs**:
   [.agents/docs/integration-testing.md](../../../.agents/docs/integration-testing.md)
   (environment, `*UnitTests` vs `*IntegrationTests` naming, base/overload test
   structure, running one test) and
   [.agents/docs/api-consistency.md](../../../.agents/docs/api-consistency.md) (the
   flavors and their return-type mapping rules).

6. **Trace one analogous existing command** end-to-end (same command group, similar
   reply shape) so the plan mirrors real code, not guesswork. Good reference PRs,
   validated against git history:

   | Commit | What it exemplifies |
   |--------|---------------------|
   | `7ca3e9cc0` (CLIENT NO-TOUCH #3776) | Minimal new command: all 6 flavors + builder + 2 dispatch layers + Kotlin impl + builder unit test + integration test |
   | `312ecc7b4` (INCREX #3746) | New command with args: self-typed abstract args base, new value type, new `CommandOutput`s, per-type args/output unit tests, **creating a missing RESP2 overload class** |
   | `b009df37b` (XNACK #3728) | New command with an enum-valued argument |
   | `9a0899875` (BITOP extensions #3334) | New operations/overloads on an existing command |
   | `c603c8120` (HSCAN NOVALUES #2816) | New overload rippling into helpers (`ScanIterator`, `ScanStream`, `ScanFlow`) |
   | `aa7b4b0be` (stream idempotency #3637) | Option added to an existing `*Args` class flows through with **no interface change** (the `XAddArgs` part) |
   | `a209fba70` (CAS/CAD #3512) | Read-only command registered in `ReadOnlyCommands` + count test update |
   | `838a4d39a` (HOTKEYS #3638) | Map-shaped reply via `ComplexOutput` + a `*ReplyParser`, cluster interface additions, full integration overload set, `.env` image bump |
   | `6567f2d5e` (RediSearch #3375) | Whole new command area: own `Redis<Area>CommandBuilder`, `arguments`/reply-parser packages |

   **Conventions beat precedent.** Traced reference commands can predate the
   current conventions; when the reference code conflicts with a written rule in
   this skill or the linked docs, the rule wins. Example: `sintercard(K...)`
   ships without a single-key overload because it predates the
   one-overload-per-varargs rule — a new command mirroring it must still add the
   single-argument overload.

7. **Determine the `@since` version** — recipe owned by
   [.agents/docs/javadoc.md](../../../.agents/docs/javadoc.md):
   ```bash
   mvn help:evaluate -Dexpression=project.version -q -DforceStdout
   ```
   Drop `-SNAPSHOT` and the patch digit (`7.7.0-SNAPSHOT` → `@since 7.7`).

**The evidence is your working context, not a deliverable.** Hold it in mind to
drive the work — do not paste it back to the maintainer or pause for "spec
approval." The only stop for the maintainer is the Phase 1 plan approval; surface
genuinely ambiguous design choices *there*, with the proposed sync signatures,
rather than interrupting earlier.

## Phase 1 — Plan mode, then explicit approval

Enter **plan mode**. Using the evidence, classify the change with the decision tree
below and enumerate the exact file-by-file touch list, the test matrix, and the
gating annotations. The plan must contain:

- A short "what this feature enables" section built from the showcase scenarios
  (Phase 0 step 4), including one or two representative command/reply transcripts.
- **The proposed sync interface signature(s) with their Javadoc.** The sync
  interface is the human-authored **API contract**; every other flavor is derived
  from it and it is costly to change once mirrored. Plan approval is the
  maintainer's sign-off on that contract — if the signatures must deviate later
  during implementation, stop and re-confirm before mirroring.
- **The complete overload set, enumerated.** For every varargs parameter, list
  the matching single-argument overload (the house rule — see "Types & args
  conventions") or explicitly justify its absence so the maintainer signs off on
  the exception. An overload discovered missing in review means reworking all
  six flavors.

Present the plan and **explicitly ask permission to execute** before implementing.
Do not start editing files until the user approves.

Once approved, copy this checklist into your working notes and tick items off:

```
Extend-commands progress:
- [ ] 0. Evidence: HLD, server PR, live probe + showcase, RESP2/RESP3 replies, @since
- [ ] 1. Plan approved — incl. the sync signature(s), the API contract
- [ ] 2. Types: argument & response types (they must exist before any interface edit)
- [ ] 3. Sync interface: full overload set (varargs → single-arg too) + Javadoc
       (@param constraints + @throws for builder-validated preconditions)
- [ ] 4. Mirror: async, reactive, Kotlin, NodeSelection×2 — consistency tests pass
- [ ] 5. Implementations: CommandType/Keyword, builder, async, reactive, Kotlin impl
- [ ] 6. Tests: args/builder/output unit tests + integration base/overloads
- [ ] 7. Docs: entry in the current-release section of docs/new-features.md
- [ ] 8. Verify: mvn clean test + a single integration test run; then make stop
```

## Decision tree — what kind of change is this?

**A. Extension of an existing command that fits an existing `*Args` class**
(new option token / new field):
- Touch ONLY the `*Args` class (+ `CommandKeyword` for new tokens) + tests. Do NOT
  touch the command interfaces, builder signatures, or dispatch layers — the
  existing `args.build(commandArgs)` delegation carries the new option through
  automatically (cf. the `XAddArgs` part of the stream-idempotency PR).
- Add a fluent setter returning `this`; register new tokens in
  `src/main/java/io/lettuce/core/protocol/CommandKeyword.java`.
- If the reply shape grows, extend the response model/output backward-compatibly.

**B. New command(s) in an existing group — core or module area** — the FULL
matrix, in this order (types first — every flavor references them, so they must
exist to compile). For a command joining an **existing module area** (a new
`FT.*` method in the Search group, a new `JSON.*` method, …) the same matrix
applies with the area substitutions: the group is the area's flavor interfaces,
the builder is the area's `Redis<Area>CommandBuilder` (+ its
`Redis<Area>CommandBuilderUnitTests`), argument/reply types go in the area
package, and gating/tests follow the module rules in D (capability probe, stack
node). The dispatch layers are the same `AbstractRedisAsyncCommands` /
`AbstractRedisReactiveCommands` and Kotlin `*Impl.kt` as for core commands.
1. **Argument/response types** — see "Types & args conventions" below.
2. **Sync interface** `src/main/java/io/lettuce/core/api/sync/<Group>Commands.java`
   — pick the group by command family (STRING → `RedisStringCommands`, HASH →
   `RedisHashCommands`, generic-key → `RedisKeyCommands`, …). This is the reference
   signature + Javadoc all flavors mirror (see the
   [writing-javadoc](../writing-javadoc/SKILL.md) skill; `@since` mandatory):
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
   Two contract rules to apply while designing the signatures and their Javadoc:
   - **Every varargs parameter gets a single-argument overload** —
     `foo(K key, V value)` alongside `foo(K key, V... values)`. This is a hard
     convention for new commands and overrides any traced precedent that lacks
     it (older commands predate the rule).
   - **Validated preconditions are contract.** Any constraint the builder will
     enforce (null checks, non-empty varargs) must be stated in the `@param`
     text with the house phrases (`must not be {@code null}.`,
     `must not be empty.`) *and* documented with a matching
     `@throws IllegalArgumentException if …` tag — forms owned by
     [.agents/docs/javadoc.md](../../../.agents/docs/javadoc.md). Both are then
     mirrored to every flavor.
3. **Mirror to every flavor**: async, reactive, Kotlin coroutines, and the two
   cluster node-selection interfaces (`NodeSelection<Group>Commands` /
   `NodeSelection<Group>AsyncCommands`), applying the per-flavor return-type
   mapping rules from
   [.agents/docs/api-consistency.md](../../../.agents/docs/api-consistency.md).
4. **Protocol enums** — command name in
   `src/main/java/io/lettuce/core/protocol/CommandType.java` (wire bytes derive
   from the enum name); subcommand tokens in `CommandKeyword`. **Never add a
   `CommandKeyword` that duplicates a name already in `CommandType`** (e.g. `SET`,
   `DISCARD`) — the builder static-imports both enums and the bare name becomes
   ambiguous; reference `CommandType.<NAME>` directly instead.
5. **Builder** — `RedisCommandBuilder.java` (or the area builder, see D): null
   checks via `LettuceAssert`/`notNullKey`, `CommandArgs` in **wire order**, the
   `CommandOutput` chosen from the observed RESP2/RESP3 replies:
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
   Every `LettuceAssert` precondition written here is public contract: if the
   interface Javadoc (step 2) does not already state the constraint and its
   `@throws IllegalArgumentException`, go back and add it — on every flavor.
6. **Dispatch layers** — one-liners in *both* `AbstractRedisAsyncCommands` *and*
   `AbstractRedisReactiveCommands` (`createMono` for scalars,
   `createDissolvingFlux` for `List`/`Set` replies, matching the interface's
   `Mono`/`Flux`):
   ```java
   // AbstractRedisAsyncCommands
   public RedisFuture<Long> strlen(K key) { return dispatch(commandBuilder.strlen(key)); }
   // AbstractRedisReactiveCommands
   public Mono<Long> strlen(K key) { return createMono(() -> commandBuilder.strlen(key)); }
   ```
7. **Kotlin impl** —
   `src/main/kotlin/io/lettuce/core/api/coroutines/<Group>CoroutinesCommandsImpl.kt`:
   ```kotlin
   override suspend fun strlen(key: K): Long = ops.strlen(key).awaitSingle()
   ```
8. **Cluster** — pick the routing shape deliberately; there are three cases:
   - A **single-key** command flows through automatically (routed by slot).
   - A **broadcast/all-shards** command (its answer is the aggregate over the
     whole cluster — cf. `dbsize`, `flushall`) needs hand-coded fan-out
     overrides in `RedisAdvancedClusterAsyncCommandsImpl` *and* its reactive
     sibling (`executeOnUpstream` + a `MultiNodeExecution` aggregator), and may
     need methods on the cluster aggregate interfaces.
   - A **node-specific** command (keyless, but its result is only meaningful per
     node — cf. HOTKEYS) must **not** be fanned out: add `default` overrides on
     the cluster aggregate interfaces that throw `UnsupportedOperationException`
     and direct callers to the node-selection API or `getConnection(nodeId)`
     (cf. `RedisClusterCommands.hotkeysReset()`); only the node-selection
     flavors execute it.

   See "Cluster routing" in
   [.agents/docs/architecture.md](../../../.agents/docs/architecture.md).
9. **Read-only command?** Register it in
   `src/main/java/io/lettuce/core/protocol/ReadOnlyCommands.java` (`CommandName`
   enum) so replica-read routing knows, and bump the size assertion in
   `ClusterReadOnlyCommandsUnitTests`.

**C. Extension needing new overloads / a new `*Args` class** (hybrid): the new
methods go through the full matrix of B; the option plumbing follows A. Check
whether command helpers must follow (`ScanIterator`/`ScanStream`/`ScanFlow` for
scan-family commands).

**D. New command group / module area** (Search/JSON/Bloom/VectorSet-style):
- **Unlike Jedis, Lettuce module areas are full citizens**: every group gets all
  six flavors — sync, async, reactive, Kotlin coroutines, and both node-selection
  interfaces — plus Kotlin impls.
- Create the flavor interfaces by mirroring an existing area end-to-end, register
  the group in the `CommandInterfaces` enum
  (`src/test/java/io/lettuce/core/api/consistency/`), and wire the group into the
  hand-written aggregate interfaces (`RedisCommands`, `RedisAsyncCommands`,
  `RedisReactiveCommands`, and the cluster variants) so they `extend` it — the
  consistency tests enforce the aggregate wiring.
- Areas get their **own builder** (`Redis<Area>CommandBuilder`, cf.
  `RediSearchCommandBuilder`) with a matching
  `Redis<Area>CommandBuilderUnitTests`, and keep their argument types and reply
  parsers in an area package (e.g. `core/search/arguments/`).
- Module commands still gate on server capability, not version:
  `@EnabledOnCommand("FT.CREATE")`-style probes; integration tests target the
  stack node.

## Types & args conventions

- **Argument types**: an options object is a `*Args implements CompositeArgument`
  class (e.g. `io.lettuce.core.CopyArgs`) whose `build(CommandArgs)` appends its
  tokens. Fluent setters return `this`. If two overloads share options but differ
  in a typed field (long vs double), use a self-typed abstract base
  (`BaseFooArgs<T extends BaseFooArgs<T>>`) with concrete subclasses — cf.
  `BaseIncrexArgs`/`IncrexArgs`/`IncrexFloatArgs`.
- **`@since` goes on every new public element, not just the class.** A
  class-level `@since` is **not inherited**: the nested `Builder` type, each of
  its static factory methods, and each public fluent setter needs its own
  `@since` tag, or the generated API docs lose the release provenance for those
  members.
- Token-valued argument enums are plain enums whose values the builder/args class
  appends (cf. `XNackMode`).
- **Response types**: reuse existing models where possible — `Value`, `KeyValue`,
  `ScoredValue`, `GeoCoordinates`, `GeoWithin`, `StreamMessage`, `KeyScanCursor`
  (all in `io.lettuce.core`) — and add a new one only when the reply genuinely
  doesn't map. For a map-shaped/structured reply, pair a model class with a
  `ComplexDataParser` consumed via `ComplexOutput` (cf. `HotkeysReply` +
  `HotkeysReplyParser`).
- **Return-type idioms** (established conventions): `1/0` integer reply →
  `Boolean` (cf. `copy`, `expire`, `hsetnx`); count → `Long`; status → `String`;
  bulk value → `V`. And the overload rule from step B.2: every varargs
  parameter also gets a single-argument overload.
- The `CommandOutput` (the reply *parser*) is chosen at the builder step from the
  **observed** RESP2/RESP3 replies of Phase 0 — if none fits, add one under
  `io.lettuce.core.output` with a unit test (cf. `IncrexLongOutput`).

## The consistency suite is the safety net

After mirroring, run:

```bash
mvn -Dtest='*ConsistencyUnitTests,CommandBuilderCoverageUnitTests' \
    -Dsurefire.failIfNoSpecifiedTests=false test
```

It names exactly the flavor/signature you missed. For a genuinely unusual return
type (e.g. `Flux<Value<Long>>`, or `Mono<List<Double>>` because Redis returns
nulls), register it in the registry that owns the flavor —
`src/test/java/io/lettuce/core/api/consistency/KnownApiDeviations.java` for the
Java flavors, `src/test/kotlin/io/lettuce/core/api/consistency/KnownKotlinApiDeviations.kt`
for the coroutine flavor — **with a comment justifying it**. Never use a
deviation entry to paper over a sync/async signature mismatch — that breaks the
sync-over-async runtime proxy.

**Format before you build**: run `mvn formatter:format` after hand-editing — the
build's `formatter:validate` step fails the compile on unformatted code. Do not
submit formatting-only diffs.

## Test matrix — what to write

Naming, placement, and the base/overload structure are owned by
[.agents/docs/integration-testing.md](../../../.agents/docs/integration-testing.md)
— follow it. The established per-command layers (write all that apply):

1. **Args unit tests** (`*Args` classes): assert the exact encoded tokens and
   wire order, setter validation, and overload equivalence — e.g.
   `IncrexArgsUnitTests`, `XAddArgsUnitTests`. No server needed.
2. **Builder unit test**: assert the constructed command and encoded args —
   including the RESP2/RESP3 output shape observed in Phase 0. Core commands go
   in `src/test/java/io/lettuce/core/RedisCommandBuilderUnitTests.java`; area
   commands in their `Redis<Area>CommandBuilderUnitTests`.
3. **Output unit tests** when a new `CommandOutput` was added (cf.
   `IncrexOutputUnitTests`).
4. **Integration tests**: add methods to the sync base class
   (`<Group>CommandIntegrationTests`), gated per-test with
   `@EnabledOnCommand("<NAME>")`. Use assertions derived from the redis-cli
   showcase transcripts — real semantics, not just "no error", covering the whole
   family the option touches (with/without optional args, error cases):
   ```java
   @Test
   @EnabledOnCommand("COPY")
   void copy() {
       redis.set(key, value);
       assertThat(redis.copy(key, key + "2")).isTrue();
   }
   ```
5. **Overloads**: the base's `@Test` methods re-run automatically under the
   group's existing RESP2/cluster/reactive/Tx overload classes — but **only the
   ones that exist**. Check the target group against peer groups and **create a
   missing overload class** when it matters for the command (INCREX created
   `StringCommandResp2IntegrationTests` because its reply differs by protocol).
   Provide the base test at minimum; add overloads that carry real
   risk (RESP2 when replies differ, cluster when routing matters).

## Running the tests

The build pins a specific JDK to match CI — check `.github/workflows/` and the
local-gotchas section of
[.agents/docs/integration-testing.md](../../../.agents/docs/integration-testing.md)
(pin `JAVA_HOME`, worktree `git-commit-id-plugin` skip, `TEST_WORK_FOLDER`).

- Unit tests (Surefire, no server): `mvn clean test`, or `mvn -Dtest=FooUnitTests test`.
- Integration tests (Failsafe) need the Docker env from Phase 0 and the `verify`
  lifecycle — **`-Dit.test=` filters Failsafe, `-Dtest=` does not**:
  ```bash
  TEST_WORK_FOLDER=./work/docker mvn -DskipITs=false -DskipUnitTests=true \
    -Dit.test=FooIntegrationTests verify -Pci
  ```
  If the command isn't in any published `redislabs/client-libs-test` tag yet, say
  so: the integration tests will be skipped by `@EnabledOnCommand` (expected and
  acceptable), but they must still be written and compile.

**Tear the environment down when you are done.** The Docker topology started in
Phase 0 keeps running (and holds the test ports) until stopped. After the final
verification run — and equally when the task is aborted or fails partway — run:

```bash
make stop
```

## PR hygiene checklist (verify before finishing)

- [ ] Every layer of the chosen matrix updated consistently; the consistency suite
      and `CommandBuilderCoverageUnitTests` pass.
- [ ] Every varargs parameter has its single-argument overload, mirrored across
      all flavors (or a maintainer-approved justification from the plan).
- [ ] `@since` on **every** new public element — including the nested `Builder`,
      static factories, and fluent setters of new `*Args` classes; class-level
      tags are not inherited (see
      [.agents/docs/javadoc.md](../../../.agents/docs/javadoc.md)). Javadoc written on
      the sync interface and mirrored with flavor-appropriate `@return` phrasing.
- [ ] Builder-validated preconditions documented on all flavors: `@param`
      constraint phrases + `@throws IllegalArgumentException if …`.
- [ ] No dead `CommandKeyword` constants; no keyword duplicating a `CommandType`.
- [ ] Read-only commands registered in `ReadOnlyCommands` (+ count test bumped).
- [ ] `mvn formatter:format` run; no formatting-only noise in the diff.
- [ ] Tests at every applicable layer, gated with `@EnabledOnCommand`; missing
      integration overload classes created where the command needs them.
- [ ] `docs/` (MkDocs) updated — a new command or option **is** user-facing: add
      a one-line entry to the current-release section of `docs/new-features.md`
      (follow its existing "Support for [`X`](redis.io link) …" pattern), plus
      any feature page the change affects.
- [ ] `.env.vX.XX` image pin bumped if the feature needed a newer server build.
- [ ] PR description states: server PR link, HLD link, gating choice and why,
      behavior against older servers, and includes a showcase transcript. (Draft
      with the [draft-pr-description](../draft-pr-description/SKILL.md) skill;
      remember the guardrail — the agent never creates the PR itself.)
- [ ] Docker test environment stopped (`make stop`) after the final verification
      run.

## Top pitfalls

1. **Skipping the live verification / not checking RESP2 vs RESP3.** The reply
   shape can differ between protocols; it determines the `CommandOutput` and the
   reactive mapping. Confirm against a running server, don't assume.
2. **Adding Args/response types after the interface edits** — every flavor
   references them; the project won't compile. Types come first.
3. **Editing only some flavors, or silencing the consistency suite** with a
   deviations-registry entry instead of fixing the signature.
4. **Forgetting a dispatch layer** — both `AbstractRedisAsyncCommands` *and*
   `AbstractRedisReactiveCommands`, plus the Kotlin `*Impl.kt`.
5. **Recreating the removed generator source files** under
   `src/test/java/io/lettuce/core/api/` because an old reference PR touched them.
6. **Wrong `CommandArgs` order or `CommandOutput`**, missing `@since`, missing
   `@EnabledOnCommand` gating, or missing the read-only registry entry.
7. **Letting a traced precedent override a written convention** — e.g. skipping
   the single-argument overload because `sintercard(K...)` doesn't have one, or
   stopping at a class-level `@since` because an old `*Args` class did. Older
   code predates the rules; the conventions win.
