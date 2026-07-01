# Add AI-agent operating manual and integration-testing docs

## Summary
Introduces repository guidance aimed at both AI coding agents and human
contributors. Adds a root `AGENTS.md` operating manual (with a thin `CLAUDE.md`
that imports it), a single reusable PR-description skill, and a new
`docs/integration-testing.md` that documents how Lettuce's Dockerized Redis test
infrastructure works. No production code or public API is touched.

## Key Decisions & Assumptions
- `AGENTS.md` is the single source of truth; `CLAUDE.md` imports it rather than
  duplicating content.
- One skill is shipped — `creating-description-for-gh-pr` — authored under
  `.agents/skills/` and exposed to Claude via a `.claude/skills/` symlink plus a
  `/createPRDescription` slash command. It references Lettuce's own
  `PULL_REQUEST_TEMPLATE.md`/`CONTRIBUTING.md` conventions so generated PRs match
  house style.
- Build/test facts (JDK 8 baseline, unit vs. integration naming, `make` lifecycle)
  are documented once and cross-referenced from `CONTRIBUTING.md`.

## Behavioral / Conceptual Changes
- `CONTRIBUTING.md` now separates the no-Redis unit build from the Dockerized
  integration flow (`make start version=8.6` / `make test` / `make stop`) and links
  the new guide.
- `mkdocs.yml` gains a "Development guide" nav section (Contributing, Integration
  Testing, Benchmarks).
- `.gitignore` excludes the skill's generated `prDescription.md`.

## Notes
- No feature-request issue is referenced yet — open one and add `#<ticket>` to the
  title before merging (Lettuce requires it).
- Docs/tooling-only change: no production code, so no unit/integration tests apply.

## Checklist
- [ ] You have read the [contribution guidelines](https://github.com/lettuce-io/lettuce-core/blob/main/.github/CONTRIBUTING.md).
- [ ] You have created a feature request first to discuss your contribution intent. Please reference the feature request ticket number in the pull request.
- [x] You applied code formatting rules using the `mvn formatter:format` target. Don’t submit any formatting related changes. <!-- docs/tooling only — no Java touched -->
- [ ] You submit test cases (unit or integration tests) that back your changes. <!-- N/A — docs/tooling only; see Notes -->
