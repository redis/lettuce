---
name: version-migration
description: Upgrade code across Lettuce major versions — find deprecated/removed APIs
  and map them to replacements. Use when migrating a project to a newer Lettuce major.
allowed-tools: Read, Grep
---

# Version Migration

Help move consumer or library code to a newer Lettuce major.

## Procedure
1. Establish source and target versions (e.g. 5.x → 6.x, 6.x → 7.x). Confirm the JDK/Redis
   baseline for the target version (check its pom.xml maven.compiler.source and RELEASE-NOTES.md).
2. Find usages of APIs likely to have changed:
   - `Grep` the codebase for symbols marked `@Deprecated` in the target version.
   - Cross-check against `RELEASE-NOTES.md` and the docs "New & Noteworthy" page for the target.
3. For each hit, map deprecated/removed API → replacement, citing the `@since`/`@deprecated`
   Javadoc on the symbol as the source of truth (read the actual class, don't guess).
4. Report a migration list: file:line, old API, new API, and whether the change is mechanical
   or behavioral.

## Rules
- Ground every mapping in the repo's own source/Javadoc or `RELEASE-NOTES.md` — never invent a
  replacement API from memory.
- Flag behavioral changes (defaults, threading, timeouts) separately from signature renames.
- Note any change that crosses a public-API boundary so the user can review it before applying.
