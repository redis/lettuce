#!/usr/bin/env bash
# Stop hook: refuse to end the turn while the project does not compile.
# Maven output is fully suppressed (>/dev/null 2>&1) so that ONLY the decision
# JSON ever reaches stdout — otherwise Maven's logs would corrupt the hook
# response and the gate could not block.
set -uo pipefail

# Lettuce's build pins Java 8 (see AGENTS.md). The hook subprocess inherits no
# JAVA_HOME, so Maven would otherwise default to whatever JDK is on PATH — and
# the Kotlin plugin 2.0.0 fails under JDK 25. Resolve a JDK 8 if one exists.
if [ -x /usr/libexec/java_home ]; then
  _jh="$(/usr/libexec/java_home -v 1.8 2>/dev/null || true)"
  [ -n "$_jh" ] && export JAVA_HOME="$_jh"
fi

if mvn -q compile -DskipTests >/dev/null 2>&1; then
  exit 0
fi

printf '{"decision":"block","reason":"mvn compile failed — leave the build green before stopping."}\n'
exit 0
