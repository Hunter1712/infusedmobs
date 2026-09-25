# Local Issue Tracker

Issues are stored as individual Markdown files in this directory.

## File Format

Each issue is a `.md` file named `NNN-title-slug.md` where `NNN` is a zero-padded issue number.

### Front Matter (required)

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

### Body

Markdown content follows the front matter.

## Conventions

- **Create an issue**: Add a new file with the next sequential number
- **Read an issue**: `cat issues/NNN-*.md`
- **List issues**: `ls issues/*.md` (filter by state in front matter)
- **Comment on an issue**: Append to the file under a `## Comments` section
- **Apply/remove labels**: Edit the `labels` array in front matter
- **Close**: Change `state: closed` in front matter and add closure comment

## Triage Labels

See `docs/agents/triage-labels.md` for the canonical label vocabulary:
- `needs-triage`
- `needs-info`
- `ready-for-agent`
- `ready-for-human`
- `wontfix`

## Example

See `issues/001-example.md` for a template.