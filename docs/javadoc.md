# Writing Javadoc for Lettuce

How to write Javadoc for Lettuce's public API. These are the conventions as
practiced in the codebase, grounded in Oracle's *How to Write Doc Comments for the
Javadoc Tool*. When adding or changing public API, follow this page.

> **Enforcement:** Javadoc lint is disabled in the build (`<doclint>none</doclint>`
> in `pom.xml`), so malformed Javadoc will **not** fail compilation. These
> conventions are upheld by code review, not tooling — get them right by hand.

## First principle: document the caller-facing contract

Javadoc describes **what the API does and how to call it** — the behavior a caller
can rely on. It is **not** the place for implementation details or the reasoning
behind a change.

- ✅ "Append a value to a key."
- ❌ "Append a value to a key, added as part of making Reactor an optional
  dependency." — refactor rationale is noise to callers; it belongs in the commit
  message, design doc, or ticket, never on the API surface.

The first sentence must be a **concise, standalone summary** — Javadoc copies it
verbatim into summary tables. It must **add information beyond the method name**,
not restate it.

## Mood and voice

Command methods in Lettuce use the **imperative mood**, matching the Redis command
reference:

```java
/**
 * Append a value to a key.
 */
Long append(K key, V value);
```

Other real examples: *"Count set bits in a string."*, *"Delete one or more hash
fields."*, *"Open a new connection to a Redis server…"*.

> Oracle's guide recommends third-person declarative ("Appends a value…"). Lettuce
> diverges from this for command methods and uses the imperative to mirror Redis
> documentation. Keep new command Javadoc imperative for consistency with the
> surrounding code.

## Tags

**Order:** `@param` → `@return` → `@throws` → `@since` → `@deprecated`.
`@author` lives at the class/interface level (see below).

### `@param`

Required for every parameter, including type parameters (`<K>`, `<V>`), documented
in declaration order. Name then a short lowercase phrase, no trailing period for
phrases. Null-contracts use `{@code null}`:

```java
 * @param key the key, must not be {@code null}.
 * @param <K> Key type.
 * @param <V> Value type.
```

### `@return`

Required for non-void methods. Prefer something specific, and encode the Redis
reply type and its special cases:

```java
 * @return Long integer-reply the length of the string after the append operation.
 * @return V bulk-string-reply the value of {@code key}, or {@code null} when {@code key} does not exist.
```

### `@throws`

Document checked exceptions, and unchecked ones a caller might reasonably act on.
Exception type first, then the condition:

```java
 * @throws IllegalArgumentException if {@code clientOptions} is {@code null}.
```

### `@since`

Bare version number — `@since <major>.<minor>[.<patch>]`, no "v" prefix. Real
examples: `@since 5.0`, `@since 6.1.3`, `@since 7.4`. Put it at class level once;
add it to a member only if the member was introduced later than its class.

**Determine the version from the current build:** run
`mvn help:evaluate -Dexpression=project.version -q -DforceStdout` (or read the
top-level `<version>` in `pom.xml`) and drop `-SNAPSHOT` (a `7.7.0-SNAPSHOT` build →
`@since 7.7`).

### `@author`

Class/interface level only, never on methods. One author per line, full name, in
contribution order. **Add yourself when you touch a file** (per `.github/CONTRIBUTING.md`):

```java
 * @author Will Glozer
 * @author Mark Paluch
```

### `@deprecated`

Pair the `@Deprecated` annotation on the declaration with an `@deprecated` Javadoc
tag. The first sentence states **when** it was deprecated and **what to use
instead**, with a `{@link}` to the replacement. Canonical Lettuce form:

```java
 * @deprecated since 6.0 in favor of {@link #zrange(java.lang.Object, Range)}.
```

- Use exactly **one** `@deprecated` tag per comment. (Some older comments in the
  repo carry two — Javadoc renders only the last; do not copy that pattern.)
- A "why" is allowed only in later sentences and must be caller-relevant
  (semantic), not project narrative.

## Inline tags: `{@link}` and `{@code}`

- `{@code}` for keywords, literals, and identifiers — especially `{@code null}` in
  null-contracts.
- `{@link}` **economically**: only where a reader would click for more, and
  typically only the first occurrence of a name. Skip `java.lang` and well-known
  APIs. Use display text for readability: `{@link RedisURI uri}`.

## Comment inheritance

An overriding/implementing method with **no** doc comment inherits the supertype's
text. Adding any comment or tag keeps the "Overrides"/"Specified by" link but stops
the text copy. Don't re-document a method when the inherited contract already
applies.

## File header

Every source file starts with the standard header. The copyright start year tracks
the file's creation year; everything else is identical:

```java
/*
 * Copyright <year>-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
```

## Generated files

The command interfaces under `src/main/java/io/lettuce/core/api/{sync,async,reactive}/`
are **generated** and carry `@generated by io.lettuce.apigenerator.*` in the class
comment. **Do not edit their Javadoc directly** — write it once in the template at
`src/main/templates/io/lettuce/core/api/<Group>Commands.java` and re-run the
generator. The template Javadoc propagates verbatim into all generated flavors, so
an omission there multiplies across sync/async/reactive/Kotlin. See
[architecture.md](architecture.md) for the generation pipeline.
