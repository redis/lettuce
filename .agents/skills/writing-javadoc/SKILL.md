---
name: writing-javadoc
description: Use when writing or editing Javadoc for Lettuce public API — new methods, classes, deprecations, or when a reviewer asks to fix or improve doc comments. Points to the full house ruleset in docs/javadoc.md (third-person summaries, fixed tag order, @param/@return/@throws/@since/@deprecated house forms) and the rule that command-interface Javadoc is edited in the template, not the generated files. Trigger on "write javadoc for", "document this method", "add a deprecation notice", "fix the doc comment".
---

# Writing Javadoc for Lettuce

The complete, authoritative ruleset is **[docs/javadoc.md](../../../docs/javadoc.md)** —
read it before writing. This skill is the operating checklist; the doc wins on any
detail.

## Before you write

- **Is it a command interface?** The interfaces under
  `src/main/java/io/lettuce/core/api/{sync,async,reactive}/` are **generated**
  (`@generated` marker). Edit the Javadoc in the **template** at
  `src/main/templates/io/lettuce/core/api/<Group>Commands.java`, not the generated
  file — the template comment feeds every flavor. See [architecture.md](../../../docs/architecture.md).

## The rules most often gotten wrong

1. **Golden rule:** document the caller-facing contract (inputs, outputs, effects,
   errors, nullability) — never implementation details or refactor rationale.
2. **First sentence:** third-person verb for methods ("Returns…", "Creates…"),
   noun phrase for types. Not imperative, not "This method…". Ends in a clean period.
3. **Tag order:** `@param` → `@return` → `@throws` → `@author` → `@since` → `@see`
   → `@deprecated`.
4. **`@param`** for every parameter (type params `<K>`/`<V>` first); state
   nullability with the house phrases `must not be {@code null}.` / `can be {@code null}.`
5. **`@return`** for non-void; describe the value and its meaningful states, not the
   type.
6. **`@since`** is a bare version — `@since 7.7` (see docs/javadoc.md for deriving
   the version from the build).
7. **`@deprecated`** — keep the `@Deprecated` annotation and the tag in sync; house
   form: `@deprecated since <version>, use {@link Replacement} instead; scheduled for
   removal in a future major release.`
8. **`@author`** on types only; append your name, don't reorder existing authors.
9. Use `{@code null}` for literals; `{@link}` only to types resolvable on the
   compile classpath (else `{@code TypeName}`).

## Verify, don't guess

Match the surrounding file, and consult `docs/javadoc.md` for any subtle case
rather than inventing a rule. Javadoc lint is off in the build (`<doclint>none</doclint>`),
so review is the only check — get it right by hand.
