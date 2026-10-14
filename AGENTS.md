# AGENTS

## Agent skills

### Issue tracker

Issues live as local Markdown files under `issues/`. See `docs/agents/issue-tracker.md`.

### Triage labels

Default vocabulary: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix` (1:1 mapping). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context layout (`CONTEXT.md` at root + `docs/adr/`). See `docs/agents/domain.md`.

### Review

Diff-time rules live in `docs/agents/review.md` — the reviewer enforces those, nothing else.

### Build and test

System Gradle: `gradle :common:test` (full suite), `gradle build` (all jars). The `gradlew` wrapper is a fallback for machines without Gradle. CI (`build.yml`) runs the same plus `bash scripts/check-docs.sh`.

Deploy for manual testing: copy `26.x/build/libs/infusedmobs-*+26.x.jar` (non-sources) to the Prism `26.3` instance `minecraft/mods`, then verify via `minecraft/logs/latest.log`.

### Shell

Commands run on Windows PowerShell: page output with `Select-Object -First N` instead of `head`, single-quote multi-word git args, no `python3`, `grep` or `bash` on PATH.
