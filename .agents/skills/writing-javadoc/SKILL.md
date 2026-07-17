---
name: writing-javadoc
description: Use when writing or editing Javadoc for Lettuce public API — new methods, classes, deprecations, or when a reviewer asks to fix or improve doc comments. Covers Lettuce's house conventions (imperative first sentence, tag order, @since/@param/@return/@throws/@deprecated forms, file header) and the rule that command-interface Javadoc is edited in the template, not the generated files. Trigger on "write javadoc for", "document this method", "add a deprecation notice", "fix the doc comment".
---

# Writing Javadoc for Lettuce

The full conventions live in **[docs/javadoc.md](../../../docs/javadoc.md)** — read
it before writing. This skill is the quick operating checklist.

## Before you write

- **Is it a command interface?** The interfaces under
  `src/main/java/io/lettuce/core/api/{sync,async,reactive}/` are **generated**
  (`@generated` marker). Edit the Javadoc in the **template** at
  `src/main/templates/io/lettuce/core/api/<Group>Commands.java`, not the generated
  file. The template comment feeds every flavor. See [architecture.md](../../../docs/architecture.md).

## The rules that matter most

1. **Document the caller-facing contract**, not implementation or refactor
   rationale. Never write "as part of making X optional…" on the API surface — that
   belongs in the commit message.
2. **First sentence** = concise, standalone, adds info beyond the method name.
   Command methods use the **imperative** mood ("Append a value to a key.").
3. **Tag order:** `@param` → `@return` → `@throws` → `@since` → `@deprecated`.
   `@author` is class-level only; add yourself when you touch a file.
4. **`@param`** for every parameter (incl. `<K>`/`<V>`); null-contracts use
   `must not be {@code null}`.
5. **`@return`** encodes the Redis reply type and special cases
   ("Long integer-reply …, or {@code 0} when …").
6. **`@since`** is a bare version — `@since 7.7`. Derive it from the build:
   `mvn help:evaluate -Dexpression=project.version -q -DforceStdout`, drop `-SNAPSHOT`.
7. **`@deprecated`** — one tag, canonical form:
   `@deprecated since <X.Y> in favor of {@link #replacement(...)}.`, and pair it with
   the `@Deprecated` annotation.
8. Use `{@code null}` for literals and `{@link}` economically (first occurrence,
   worth-a-click references only).

## Verify, don't guess

Match the surrounding file's existing style. If a subtle case isn't covered here or
in `docs/javadoc.md`, look at how a nearby public method does it rather than
inventing a rule. Javadoc lint is off in the build, so review is the only check —
get it right by hand.
