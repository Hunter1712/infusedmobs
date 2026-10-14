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

# 3. Javadoc link drift: every {@link Name} whose target starts with an
#    uppercase simple name must resolve to a declared repo type, an import
#    of the linking file, or java.lang — catches seams deleted without
#    updating doc references (e.g. RollStore, TierRoll). Lowercase targets
#    are package-qualified externals and {@code #member} refs are skipped.
linttmp="$(mktemp -d)"
trap 'rm -rf "$linttmp"' EXIT
find "$ROOT" -path '*/src/*/java/*' -name '*.java' -print \
  | grep -v '/build/' > "$linttmp/files"
grep -rhoE '(class|interface|enum|record) [A-Za-z0-9_]+' \
  $(cat "$linttmp/files") 2>/dev/null \
  | sed -E 's/.*(class|interface|enum|record) //' \
  | sort -u > "$linttmp/declared"
printf '%s\n' String Object Class Override > "$linttmp/javalang"
while IFS= read -r javafile; do
  grep -oE '^import [^;]+;' "$javafile" \
    | sed -E 's/.*\.//; s/;//' | sort -u > "$linttmp/imports"
  grep -o '{@link [^}]*}' "$javafile" \
    | sed -E 's/^\{@link //; s/\}$//' > "$linttmp/links" 2>/dev/null || continue
  while IFS= read -r target; do
    case "$target" in \#* | [a-z]*) continue;; esac
    head="$(printf '%s' "$target" | sed -E 's/[ #(</].*$//; s/\..*$//')"
    [ -z "$head" ] && continue
    if ! grep -qx "$head" "$linttmp/declared" \
      && ! grep -qx "$head" "$linttmp/imports" \
      && ! grep -qx "$head" "$linttmp/javalang"; then
      echo "check-docs: $javafile links unresolvable type '$target'" >&2
      fail=1
    fi
  done < "$linttmp/links"
done < "$linttmp/files"

exit $fail
