---
name: pr-helper
description: Create a GitHub PR for the current branch — summarize the diff, fill the
  repo PR template, and open it. Use when the user wants to open or draft a pull request.
allowed-tools: Bash, Read
---

# PR Helper

Open a PR that satisfies Lettuce's contribution rules on the first try.

1. Inspect the change: `git diff main...HEAD` and `git log main..HEAD --oneline`.
2. Read `.github/PULL_REQUEST_TEMPLATE.md`; fill EVERY checklist item, grounded in the diff:
   - contribution guidelines read,
   - a referenced feature-request issue number,
   - `mvn formatter:format` applied (verify it produces no diff),
   - unit and/or integration tests included.
3. Confirm the build is clean: `mvn clean test` for unit changes; for changes touching
   integration paths, note that `make start && make test && make stop` is the full gate.
4. Ensure the branch is pushed: `git push -u origin HEAD` if it has no upstream.
5. Open the PR: `gh pr create --title "<imperative summary> #<issue>" --body-file <tmp>`.
   - Title follows the commit convention `<text> #<ticketnumber>`.
   - Body notes any new `@since` additions and any files you added yourself to as `@author`.

## Boundaries
- Never force-push, never auto-merge, never `--admin`; ask before requesting reviewers.
- Do NOT open a PR if there is no linked feature-request issue — prompt the user to create one first.
- Do NOT include formatting-only changes in the diff; they get PRs rejected.
- Split feature/bugfix commits from polishing commits.
