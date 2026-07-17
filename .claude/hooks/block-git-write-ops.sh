#!/usr/bin/env bash
#
# PreToolUse guardrail: block write/publish git & GitHub operations for agents.
# Enforces the "Guardrails (agents)" section of AGENTS.md.
#
# Reads the Claude Code PreToolUse hook payload (JSON) from stdin, extracts the
# Bash command, and exits 2 (blocking the tool call) if the command commits,
# pushes, or creates/merges/comments on PRs or issues. The maintainer performs
# these actions manually; the agent must not.
#
# Fail-open: if the payload can't be parsed, the hook allows the call (exit 0) —
# the written rule in AGENTS.md still applies. This is a workflow guardrail, not a
# security control. Note: raw `gh api` write calls are not matched.

set -euo pipefail

input="$(cat)"

# Pull tool_input.command out of the JSON payload (Bash tool). Fail open if we
# can't (e.g. python3 missing or non-Bash tool with no command).
cmd="$(printf '%s' "$input" \
  | python3 -c 'import sys, json; print(json.load(sys.stdin).get("tool_input", {}).get("command", ""))' \
  2>/dev/null || true)"

[ -z "$cmd" ] && exit 0

# Collapse newlines so chained commands match on one line.
norm="$(printf '%s' "$cmd" | tr '\n' ' ')"

block() {
  echo "BLOCKED by .claude/hooks/block-git-write-ops.sh: $1" >&2
  echo "Per AGENTS.md 'Guardrails (agents)', agents must not commit/push or" >&2
  echo "create/merge/comment on PRs or issues. Leave this to the maintainer;" >&2
  echo "to draft a PR description, use the draft-pr-description skill." >&2
  exit 2
}

# git commit / git push (matches anywhere, incl. after && ; |)
if printf '%s' "$norm" | grep -Eq 'git[[:space:]]+([-][^[:space:]]+[[:space:]]+|-C[[:space:]]+[^[:space:]]+[[:space:]]+)*(commit|push)([[:space:]]|$)'; then
  block "git commit/push is not allowed"
fi

# gh pr create/merge/comment/review/edit/close/reopen/ready
if printf '%s' "$norm" | grep -Eq 'gh[[:space:]]+pr[[:space:]]+(create|merge|comment|review|edit|close|reopen|ready)([[:space:]]|$)'; then
  block "creating/merging/commenting on pull requests is not allowed"
fi

# gh issue create/comment/edit/close/reopen
if printf '%s' "$norm" | grep -Eq 'gh[[:space:]]+issue[[:space:]]+(create|comment|edit|close|reopen)([[:space:]]|$)'; then
  block "creating/commenting on issues is not allowed"
fi

exit 0
