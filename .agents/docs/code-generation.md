# Running the API interface generators

The sync/async/reactive/Kotlin command interfaces are **generated** from the templates
in `src/main/templates/io/lettuce/core/api/`. This is the runbook for running the
generators **without making a mess**. For how generation fits the overall command
flow, see [architecture.md](architecture.md).

## The generators

All live in `src/test/java/io/lettuce/apigenerator/` and are JUnit tests tagged
`api_generator`:

- `CreateSyncApi`, `CreateAsyncApi`, `CreateReactiveApi`, `CreateKotlinCoroutinesApi`
- `CreateSyncNodeSelectionClusterApi`, `CreateAsyncNodeSelectionClusterApi`

Each is a `@ParameterizedTest` whose source is
`Arrays.asList(Constants.TEMPLATE_NAMES)` — so it runs **once per group** listed in
`Constants.java`, reading `<Group>Commands.java` from the templates and writing the
generated interface.

## The problem: a run regenerates *every* group

`Constants.TEMPLATE_NAMES` lists all ~23 command groups, and the generators iterate
the whole list. So running a generator rewrites the interfaces for **every** group,
not just the one you changed. Generation is **not idempotent** in practice, and the
churn is *not only imports*: committed generated files have drifted from a fresh
generation, so re-emitting them also resurfaces **reordered imports, deprecations,
and signature changes** unrelated to your work. The **reactive** and **Kotlin**
interfaces are especially prone (their generators add `@Deprecated`/`Flow` mappings
that diverge from the committed files). Run naively and you get a large, noisy diff —
and some of it will silently change the public API surface.

## Safe workflow: scope → run → review → revert

1. **Scope `Constants.TEMPLATE_NAMES` to only the group(s) you changed.** Temporarily
   reduce the array to the interface you touched, so generation only rewrites those
   files:

   ```java
   // src/test/java/io/lettuce/apigenerator/Constants.java — TEMPORARY, do not commit
   public static final String[] TEMPLATE_NAMES = { "RedisStringCommands" };
   ```

2. **Run the generators** for the flavors you need. Generation runs the build, so the
   same environment prerequisites as the tests apply — pin **JDK 8** (the CI JDK; the
   Kotlin compile fails on newer JDKs), skip the worktree-hostile git plugin, and
   clear any stale Kotlin incremental cache first (see the "Local gotchas" in
   [integration-testing.md](integration-testing.md)):

   ```bash
   rm -rf target/kotlin-ic
   JAVA_HOME=$(/usr/libexec/java_home -v 1.8) \
     mvn -Dmaven.gitcommitid.skip=true \
         -Dtest='CreateSyncApi,CreateAsyncApi,CreateReactiveApi,CreateKotlinCoroutinesApi,CreateSyncNodeSelectionClusterApi,CreateAsyncNodeSelectionClusterApi' \
         -Dsurefire.failIfNoSpecifiedTests=false test
   ```

   (On Linux, point `JAVA_HOME` at a JDK 8 install instead of `/usr/libexec/java_home`.)

3. **Keep the diff surgical — revert, then re-apply.** A regenerated file usually
   carries changes beyond your method (reordered imports, resurfaced deprecations,
   signature drift — see above). Cleaning it hunk-by-hunk is error-prone and easy to
   get half-right. The reliable approach: for **each** regenerated file,
   `git checkout HEAD -- <file>` and then **re-apply only your new method(s)** by hand
   (interfaces are trivial to hand-edit). Afterwards `git diff <file>` should show
   *only* your command.

4. **Revert `Constants.java`** back to the full list.

## What to commit — and what not to

- **Commit:** the template edit, the regenerated interface(s) for *your* group
  (sync/async/reactive/Kotlin, and cluster NodeSelection if relevant), and your
  hand-written impls/tests.
- **Do NOT commit:** the `Constants.TEMPLATE_NAMES` scoping change; regenerated files
  for groups you didn't intend to touch; and any regenerated change *other than* your
  new method(s) — import reordering, resurfaced deprecations, or signature drift that
  crept in from the non-idempotent generation.

## Reminders

- Edit **templates** (`src/main/templates/...`), never the `@generated` interface
  files — they're overwritten on the next run.
- The end-to-end command recipe (spec → template → generate → implement → test) is in
  the [adding-a-redis-command](../skills/adding-a-redis-command/SKILL.md) skill; this
  doc is just the generation step.
