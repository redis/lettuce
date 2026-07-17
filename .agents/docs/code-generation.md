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
not just the one you changed. Generation is **not idempotent** in practice — re-emitted
files can differ from what's committed (import ordering, formatting), and imports in
the files you *did* change can come out wrong. Run naively, you get a large, noisy diff
across unrelated interfaces.

## Safe workflow: scope → run → review → revert

1. **Scope `Constants.TEMPLATE_NAMES` to only the group(s) you changed.** Temporarily
   reduce the array to the interface you touched, so generation only rewrites those
   files:

   ```java
   // src/test/java/io/lettuce/apigenerator/Constants.java — TEMPORARY, do not commit
   public static final String[] TEMPLATE_NAMES = { "RedisStringCommands" };
   ```

2. **Run the generators** for the flavors you need (scoped by the step above):

   ```bash
   mvn -Dtest='CreateSyncApi,CreateAsyncApi,CreateReactiveApi,CreateKotlinCoroutinesApi' \
     -Dsurefire.failIfNoSpecifiedTests=false test
   ```

3. **Review the diff and keep only the relevant changes.** Confirm the generated
   interfaces contain your new method and nothing else; **fix any import churn** the
   generator introduced (reordering, duplicate/missing imports). Discard unrelated
   changes.

4. **Revert `Constants.java`** back to the full list.

## What to commit — and what not to

- **Commit:** the template edit, the regenerated interface(s) for *your* group
  (sync/async/reactive/Kotlin, and cluster NodeSelection if relevant), and your
  hand-written impls/tests.
- **Do NOT commit:** the `Constants.TEMPLATE_NAMES` scoping change; regenerated files
  for groups you didn't intend to touch; formatting-only import churn.

## Reminders

- Edit **templates** (`src/main/templates/...`), never the `@generated` interface
  files — they're overwritten on the next run.
- The end-to-end command recipe (spec → template → generate → implement → test) is in
  the [adding-a-redis-command](../skills/adding-a-redis-command/SKILL.md) skill; this
  doc is just the generation step.
