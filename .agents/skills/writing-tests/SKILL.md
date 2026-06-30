---
name: writing-tests
description: Add or change JUnit 5 tests for Lettuce — follow the repo's unit vs.
  integration conventions, tags, and run commands. Use when writing or modifying tests.
allowed-tools: Bash, Read, Edit
---

# Writing Tests

Lettuce uses JUnit 5 + AssertJ. Tests split into unit vs. integration by filename.
**Full conventions and infrastructure: `docs/integration-testing.md`.**

## Unit tests (`*UnitTests.java`)
- Run in isolation, no Redis required. Tag with `@Tag(UNIT_TEST)` (`import static io.lettuce.TestTags.UNIT_TEST`).
- Package-private class, package-private `void should…()` methods, `@org.junit.jupiter.api.Test`.
- Assert with AssertJ: `import static org.assertj.core.api.Assertions.*`.
- Add a class Javadoc with `@author <you>`.
- Run: `mvn test -Dtest=CopyArgsUnitTests` (single) or `mvn clean test` (all unit).

      @Tag(UNIT_TEST)
      class CopyArgsUnitTests {
          @Test
          void shouldRenderFullArgs() {
              CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);
              CopyArgs.Builder.destinationDb(1).replace(true).build(args);
              assertThat(args.toCommandString()).isEqualTo("DB 1 REPLACE");
          }
      }

## Integration tests (`*IntegrationTests.java`)
- Require a running Redis. Tag with `@Tag(INTEGRATION_TEST)`.
- Use `@ExtendWith(LettuceExtension.class)`, extend `TestSupport`, and `@Inject` connections/clients.
- Run via the Dockerized env: `make start`, then `make test`, then `make stop`.

## Conventions
- Reuse existing test sample/domain classes; prefer NEW test cases over amending existing ones.
- Test as locally (unit) as possible; add integration coverage when behavior needs a live Redis.
- Place tests by area: connect/pool → `core/`; cluster → `core/cluster/`; HA → `core/masterreplica/`.
- Never delete or weaken a failing test just to make the build pass.
