# Writing Javadoc for Lettuce

A complete, enforceable ruleset for writing Javadoc in this codebase. It combines the Oracle
*"How to Write Doc Comments for the Javadoc Tool"* guidance with the house style used throughout
Lettuce.

> **Golden rule — document the contract, not the construction.**
> Public-API Javadoc describes **how to use the API and what it guarantees** — inputs, outputs,
> effects, errors, threading, nullability. It must **not** narrate implementation details, refactor
> history, or *why the code was written this way*. If a fact isn't useful to a caller, it doesn't
> belong in the doc comment.

---

## 1. When a doc comment is required

- **Required** on every `public` and `protected` type, method, constructor, and field — these are
  the API surface.
- **Recommended** on package-private members when the behaviour is non-obvious.
- **Optional** on `private` members; use a normal `//` comment unless the logic genuinely needs it.
- **Overrides**: prefer `{@inheritDoc}` or omit the comment entirely (Javadoc inherits it) unless the
  override strengthens or changes the contract.
- `package-info.java` **must** document every public package (one-paragraph summary of the package's
  responsibility).

---

## 2. Anatomy of a doc comment

```java
/**
 * {First sentence — the summary.}  {Optional additional description, one or more paragraphs.}
 *
 * @param  ...
 * @return ...
 * @throws ...
 * @author ...
 * @since  ...
 * @see    ...
 * @deprecated ...
 */
```

Order is fixed: **summary → body → block tags**, and block tags follow the ordering in §7.

- Open with `/**` (two stars) on its own line; every subsequent line starts with ` * ` (space-star-space).
- The comment sits **directly above** the element it documents, below any annotations is fine but the
  comment conventionally comes first, then annotations, then the declaration.
- The license/copyright header is a **separate** `/* ... */` block comment at the top of the file and
  is **not** part of any Javadoc.

---

## 3. The first sentence (summary)

The first sentence becomes the summary in generated docs and in IDE tooltips. It is the most
important line you write.

- It ends at the first period followed by whitespace — **avoid periods mid-sentence** (`e.g.`, `i.e.`,
  `Mr.`) or escape with `{@literal}`; prefer rewording ("for example").
- Make it a **self-contained summary fragment**, not a restatement of the name.
- **Methods**: start with a **third-person verb** ("Returns", "Creates", "Sends", "Resolves"), not
  "This method…" and not the imperative ("Return").
- **Classes/interfaces**: start with a **noun phrase** describing what the type *is*.
- **Fields/constants**: a noun phrase for what the value represents.

```java
// GOOD (method)   Returns the {@link CredentialsProvider} used to authenticate connections.
// GOOD (class)    Redis URI holding connection details for Redis and Sentinel connections.
// BAD             This method returns the credentials provider.   // "This method" / weak
// BAD             getCredentialsProvider.                          // restates the name
// BAD             Return the provider                             // imperative, no period
```

---

## 4. Style

- Write in **US English**, present tense, third person.
- Method/constructor descriptions describe **behaviour and contract** ("Loads credentials from…",
  "Fails exceptionally when…"), not step-by-step internals.
- Be concise. Prefer short declarative sentences over long compound ones.
- Refer to parameters by name in `{@code}` when discussed in prose: "when {@code count} is negative".
- Do **not** document the obvious (`@param name the name`). Add value or omit the phrase.
- Do **not** reference private helpers, internal classes, commit history, ticket numbers, or
  "previously this did X" in public Javadoc.
- Keep line length consistent with the project formatter; let the formatter wrap — never hand-pack
  to a different width.

---

## 5. Inline tags & HTML

| Use | For |
|---|---|
| `{@link Type}` / `{@link Type#member}` / `{@link #member}` | A cross-reference the reader can click. The target **must** be resolvable on the compile classpath. |
| `{@linkplain ...}` | Same as `{@link}` but rendered in normal (not code) font. |
| `{@code x}` | Inline code, literals, keywords, `null`, `true`/`false`, type names you don't want linked, and anything containing generics/`<`/`>`/`&`. |
| `{@literal x}` | Text with `<`, `>`, `&`, or a stray `.` that must render verbatim without code font. |
| `{@value}` | Inline the value of a constant field. |
| `{@inheritDoc}` | Pull in the superclass/interface comment. |

- Use `null`, `true`, `false` as **`{@code null}`**, never bare or `<code>null</code>`.
- Prefer `{@code}` over the legacy `<code>`/`<tt>` HTML tags.
- Paragraphs after the first are separated by `<p>` on its own line (Lettuce uses a leading `<p>`,
  not a closing `</p>`).
- Lists use `<ul>`/`<ol>` + `<li>`. Keep HTML minimal and valid.
- **Never** put a cross-package `{@link}` to a type that isn't on the module's compile classpath —
  it breaks the doc build and creates a hidden compile dependency. Use `{@code TypeName}` instead.

```java
/**
 * Resolves credentials asynchronously.
 * <p>
 * Retrieval must complete within the connection timeout; otherwise the connection fails. If the
 * lookup fails, the returned {@link CompletionStage} completes exceptionally with the cause.
 */
```

---

## 6. Common block tags — quick reference

| Tag | Applies to | Rule |
|---|---|---|
| `@param` | methods, constructors, generics | One per parameter/type-parameter, **in declaration order**. |
| `@return` | non-void methods | Required unless the method returns `void`. Omit for `void`. |
| `@throws` / `@exception` | methods, constructors | One per documented exception. Prefer `@throws`. |
| `@deprecated` | any deprecated element | Required whenever `@Deprecated` is present. |
| `@since` | any element | The version the element was introduced. |
| `@author` | types (classes/interfaces) | Contributor names; one tag per author. Not on methods/fields. |
| `@see` | any element | Related references. |
| `{@inheritDoc}` | overrides | Inline; pulls in inherited text. |

---

## 7. Block-tag ordering

Always emit tags in this order (skip any that don't apply):

```
@param        (type params first, then value params, each in declaration order)
@return
@throws       (most-specific / most-likely first)
@author       (types only)
@since
@see
@deprecated   (last)
```

---

## 8. Tag rules in detail

### `@param`
- One per parameter, in the **exact declaration order**; type parameters (`<K>`, `<V>`) first.
- Description is a **lowercase phrase**, no leading article required, no trailing period necessary
  (be consistent within a file).
- State nullability explicitly with the house phrasing:
  - `must not be {@code null}.`
  - `can be {@code null}.`
- Document constraints and units: ranges, non-negative, "in milliseconds", "must not be empty".

```java
 * @param username can be {@code null}.
 * @param password must not be {@code null}.
 * @param timeout  connection timeout, must be positive.
 * @param <K>      Key type.
 * @param <V>      Value type.
```

### `@return`
- Required for every non-`void` method. Describe **what** is returned and any meaningful states
  (empty, `null`, a `CompletionStage` that completes exceptionally, etc.).
- Do not merely restate the type: `@return a String` is useless; say what the string *is*.
- For `CompletionStage`/`Mono`/`Flux`, describe the emitted value(s) and terminal/error behaviour.

```java
 * @return a {@link CompletionStage} completing with the resolved {@link RedisCredentials}; completes
 *         exceptionally if credentials cannot be loaded.
```

### `@throws` / `@exception`
- Document every checked exception, and every unchecked exception that is **part of the contract**
  (e.g. `IllegalArgumentException` for invalid input, `IllegalStateException` for wrong state).
- Start with the condition: `@throws IllegalArgumentException if {@code count} is negative.`
- Do not document unchecked exceptions that are incidental and not part of the guarantee.

### `@since`
- Format matches releases: `@since 6.2`, `@since 7.7`. One version, no prose.
- Add `@since` to **new** public elements when introduced. Do not backfill mechanically, but new
  additions in a release must carry it.

### `@author`
- On **types only**, not methods or fields. Append your name as a new `@author` line; do not remove
  or reorder existing authors.

### `@see`
- Reference genuinely related types/methods or external material. Forms:
  - `@see OtherType`
  - `@see OtherType#method(ArgType)`
  - `@see <a href="https://redis.io/commands/...">SET</a>`

### `@deprecated`
- Required whenever the `@Deprecated` **annotation** is present (annotation drives the compiler; tag
  drives the docs — always keep both in sync).
- House phrasing: **`@deprecated since <version>, use {@link Replacement} instead; scheduled for
  removal in a future major release.`**
- Point to the replacement with `{@link}`. If there is no replacement, say so explicitly.
- Place `@deprecated` **last** among block tags.

```java
/**
 * Interface for loading {@link RedisCredentials}.
 *
 * @author Mark Paluch
 * @since 6.2
 * @deprecated since 7.7, use {@link CredentialsProvider} instead; scheduled for removal in a future
 *             major release.
 */
@Deprecated
public interface RedisCredentialsProvider { /* ... */ }
```

---

## 9. Element-specific conventions

### Types (class / interface / enum / annotation)
- Noun-phrase first sentence describing the type's responsibility.
- Follow with usage guidance, invariants, thread-safety, and lifecycle where relevant.
- Include `@author` and `@since`. Add `@param <K>`/`@param <V>` for generic type parameters.
- For SPI/extension interfaces, describe **what an implementor must provide** and any contract
  (idempotency, blocking behaviour, timeout expectations).

### Methods & constructors
- Third-person verb summary. Document parameters, return, exceptions, side effects, and threading.
- Note nullability of parameters and of the return value.
- For async/reactive returns, document terminal signals and error propagation.
- Constructors: describe what a valid instance requires; document argument constraints.

### Fields & constants
- Public constants: describe the meaning and, where helpful, use `{@value}`.
- Avoid documenting mutable non-final public fields — prefer accessors (but if present, document the
  invariant).

### `package-info.java`
- One clear paragraph: what the package contains and its role. Note if the package is internal/SPI.

---

## 10. Nullability & contracts

- Always state whether parameters and return values may be `{@code null}`.
- Prefer the standard phrases (`must not be {@code null}.`, `can be {@code null}.`) so the codebase
  reads uniformly.
- If the project uses nullability annotations (e.g. `@Nullable`), the annotation and the Javadoc must
  agree; the Javadoc still spells out the behaviour in prose.

---

## 11. Anti-patterns (do not do these)

- ❌ **Brainflow / rationale**: "We split this out during the reactor-optional refactor so that…",
  "Previously this returned a Mono but now…". Callers don't care; delete it.
- ❌ **Restating the signature**: `@param name the name`, `@return the result`.
- ❌ **"This method/class…"** openings. Start with the verb/noun.
- ❌ Documenting private helpers or internal wiring in a public element's comment.
- ❌ `{@link}` to internal/moved/absent types (breaks the doc build; use `{@code}`).
- ❌ Bare `null`/`true`/`false` or `<code>` HTML — use `{@code ...}`.
- ❌ Mismatched `@deprecated` tag and `@Deprecated` annotation (one without the other).
- ❌ Empty or placeholder tags (`@param x`, `@return`).
- ❌ Editing generated width by hand instead of letting the formatter wrap.

---

## 12. Worked examples

### Interface + SPI method

```java
/**
 * Loads {@link RedisCredentials} used to authenticate a Redis connection, resolving credentials
 * asynchronously as a {@link CompletionStage}.
 * <p>
 * Credentials are requested by the driver after connecting, so retrieval must complete within the
 * connection creation timeout to avoid connection failures.
 *
 * @author Jane Doe
 * @since 7.7
 */
@FunctionalInterface
public interface CredentialsProvider {

    /**
     * Resolves the {@link RedisCredentials} used to authorize a connection.
     *
     * @return a {@link CompletionStage} completing with the resolved credentials; completes
     *         exceptionally if the credentials cannot be loaded.
     */
    CompletionStage<RedisCredentials> resolveCredentialsAsync();
}
```

### Method with params, return, throws

```java
/**
 * Creates a provider that always returns the given static credentials.
 *
 * @param credentials the credentials to serve, must not be {@code null}.
 * @return a provider serving a snapshot of {@code credentials}.
 * @throws IllegalArgumentException if {@code credentials} is {@code null}.
 */
public static CredentialsProvider from(RedisCredentials credentials) { /* ... */ }
```

### Override

```java
/**
 * {@inheritDoc}
 * <p>
 * Bridges the reactive stream to the callback-based contract.
 */
@Override
public Subscription subscribeToCredentials(Consumer<RedisCredentials> onNext,
        Consumer<Throwable> onError) { /* ... */ }
```

---

## 13. Checklist before committing

- [ ] First sentence is a proper summary (verb for methods, noun for types), ends with a clean period.
- [ ] Every `public`/`protected` element is documented.
- [ ] `@param` for each parameter (and type parameter), in order; nullability stated.
- [ ] `@return` present for non-void; describes the value, not just the type.
- [ ] Contractual exceptions documented with `@throws … if …`.
- [ ] Tags in the correct order; `@deprecated` last and paired with `@Deprecated`.
- [ ] `@since` on new elements; `@author` on new types only.
- [ ] `{@code}` for `null`/literals/generics; `{@link}` targets resolve on the classpath.
- [ ] No implementation/refactor rationale, no signature restatement, no "This method…".
- [ ] File passes the formatter without manual reflow.
```