#!/usr/bin/env bash
# Regression guards for past docs-review findings.
# Pass: exit 0. Any violation: message to stderr, exit 1.
# DOCS_ROOT overrides the tree under check (used to exercise failure cases).
set -u

ROOT="${DOCS_ROOT:-$(cd "$(dirname "$0")/.." && pwd)}"
fail=0

# 1. Glossary drift: abilities.md table headers must use TriggerType (CONTEXT.md
#    defines TriggerType, _Avoid_: Trigger) and must not label an Ability
#    column Effect (CONTEXT.md _Avoid_: Effect for Ability).
if grep -q '^| Trigger |' "$ROOT/docs/guide/abilities.md" \
  || grep -q '^| ID | Ability | Effect |' "$ROOT/docs/guide/abilities.md"; then
  echo "check-docs: docs/guide/abilities.md uses a glossary-avoided header (Trigger/Effect); see CONTEXT.md" >&2
  fail=1
fi

# 2. ADR consistency: if no CI workflow exists, no ADR may cite build.yml.
if ! ls "$ROOT/.github/workflows/"*.yml >/dev/null 2>&1 \
  && grep -rq 'build\.yml' "$ROOT/docs/adr/"; then
  echo "check-docs: docs/adr cites build.yml but .github/workflows has no workflow file" >&2
  fail=1
fi

exit $fail
