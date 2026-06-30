#!/usr/bin/env bash
# PreToolUse guard for Edit|Write. Reads the tool-call JSON on stdin and denies
# edits to build output, generated sources, and likely-secret files.
# Emits a permissionDecision JSON on deny; stays silent (exit 0) to allow.
set -euo pipefail

input="$(cat)"

# Extract the target path from the tool input (Edit/Write use file_path).
path="$(printf '%s' "$input" | sed -n 's/.*"file_path"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')"

# Nothing to check — allow.
[ -z "$path" ] && exit 0

deny() {
  printf '{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"deny","permissionDecisionReason":"%s"}}\n' "$1"
  exit 0
}

case "$path" in
  */target/*|target/*)                deny "Refusing to edit build output under target/." ;;
  *.flattened-pom.xml|*dependency-reduced-pom.xml) deny "Refusing to edit generated POMs." ;;
  */generated-sources/*|*/generated/*) deny "Refusing to edit generated sources." ;;
  */apigenerator/*)                   deny "Refusing to hand-edit API-generator output; change the generator instead." ;;
esac

# Secret-ish filenames.
case "$(basename "$path")" in
  .env|.env.*|*.pem|*.key|*.p12|*.jks|*.keystore|id_rsa|*.crt) \
    deny "Refusing to edit a credential/secret file." ;;
esac

exit 0
