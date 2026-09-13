# Java rules

Conventions for Java source that agents most often get wrong. The build does **not**
enforce these — the Eclipse formatter (`formatting.xml`) reformats whitespace but does
not add missing braces or convert fully-qualified names to imports, and there is no
Checkstyle — so apply them by hand.

- **Use imports, not fully-qualified names.** Reference a type by its simple name and
  add an `import`. Do not inline a package-qualified name in code
  (e.g. write `RedisCommands`, not `io.lettuce.core.api.sync.RedisCommands`). The only
  exception is disambiguating a genuine clash between two same-named types in one file.
- **No wildcard imports** (`import foo.bar.*;`). Import each type explicitly.
- **Always use braces on control statements.** `if`, `else`, `for`, `while`, and `do`
  take a block even for a single-statement body — never a braceless one-liner:

  ```java
  // NO
  if (key == null)
      return;

  // YES
  if (key == null) {
      return;
  }
  ```

- **One statement per line.**

Run `mvn formatter:format` before finishing — it fixes layout, but not the rules above.
