---
name: building-and-testing
description: Build Lettuce and run its tests — unit vs. integration, the Dockerized
  Redis environment, and the relevant flags. Use when building, running, or
  debugging the test suite.
allowed-tools: Bash, Read
---

# Building & Testing

**Source of truth:** `docs/integration-testing.md` (test environment, topologies,
TLS/mTLS, test discovery, CI mapping) and `.github/CONTRIBUTING.md`. Read those for
anything not covered here — do not duplicate or guess.

## Toolchain
- Maven build; integration tests require Docker.
- Build with the JDK baseline in `pom.xml` (`maven.compiler.source`). The test build
  pins the CI JDK — see `.github/workflows/`.

## Unit tests (no Redis; `*UnitTests` → Surefire)
```bash
mvn clean test                 # all unit tests
mvn test -Dtest=FooUnitTests   # one
```

## Integration tests (Dockerized Redis; `*IntegrationTests` → Failsafe; off by default)
```bash
make start            # add version=<v>; supported list + default are in the Makefile
make test             # full build incl. ITs (passes -DskipITs=false)
make stop
```
Single IT, env already up:
```bash
TEST_WORK_FOLDER=./work/docker mvn -DskipITs=false -Dtest=FooIntegrationTests verify -Pci
```

## Rules
- Filename decides the runner (`*UnitTests` vs `*IntegrationTests`), not the folder.
- Integration tests read coordinates from `TestSettings`/`TlsSettings` — never hard-code.
- Run `mvn formatter:format` before committing; never submit formatting-only diffs.
