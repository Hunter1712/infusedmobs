# Issue tracker: Local Markdown

Issues and specs for this repo live as Markdown files in `issues/`. Use standard shell tools for all operations.

## Conventions

- **Create an issue**: Add a new file `issues/NNN-title-slug.md` with the next sequential number. Use the front matter format documented in `issues/README.md`.
- **Read an issue**: `cat issues/NNN-*.md`
- **List issues**: `ls issues/*.md` — filter by `state` in front matter (e.g., `grep -l 'state: open' issues/*.md`)
- **Comment on an issue**: Append to the file under a `## Comments` section with timestamp and author
- **Apply / remove labels**: Edit the `labels` array in front matter
- **Close**: Change `state: closed` in front matter and add a closure comment

## Front Matter Schema

```yaml
---
number: 1
title: "Issue title"
state: open|closed
labels: [label1, label2]
created: "2025-01-15T10:30:00Z"
updated: "2025-01-15T10:30:00Z"
assignee: ""
---
```

## Triage Labels

See `docs/agents/triage-labels.md` for the canonical label vocabulary:
- `needs-triage`
- `needs-info`
- `ready-for-agent`
- `ready-for-human`
- `wontfix`

## Pull requests as a triage surface

**PRs as a request surface: no.** _(Set to `yes` if this repo treats external PRs as feature requests; `/triage` reads this flag.)_

When set to `yes`, PRs run through the same labels and states as issues, tracked in the same `issues/` directory with a `pr:` prefix in the title.

## When a skill says "publish to the issue tracker"

Create a Markdown file in `issues/`.

## When a skill says "fetch the relevant ticket"

Run `cat issues/NNN-*.md`.

## Wayfinding operations

Used by `/wayfinder`. The **map** is a single issue with **child** issues as tickets.

- **Map**: a single issue labelled `wayfinder:map`, holding the Notes / Decisions-so-far / Fog body. Create with the next issue number and `wayfinder:map` label.
- **Child ticket**: an issue linked to the map via a `Part of #<map>` line at the top of the child body and a task list in the map body. Labels: `wayfinder:<type>` (`research`/`prototype`/`grilling`/`task`). Once claimed, the ticket is assigned in the `assignee` front matter field.
- **Blocking**: Add a `Blocked by: #<n>, #<n>` line at the top of the child body. A ticket is unblocked when every blocker has `state: closed`.
- **Frontier query**: List the map's open children (grep for `Part of #<map>` and `state: open`), drop any with an open blocker or an assignee; first in map order wins.
- **Claim**: Edit the child issue's front matter `assignee: @me`.
- **Resolve**: Append answer to the child issue's `## Comments`, change `state: closed`, then append a context pointer to the map's Decisions-so-far.