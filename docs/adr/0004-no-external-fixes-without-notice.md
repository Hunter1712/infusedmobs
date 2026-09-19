# No agent work on external issues without maintainer notice

Issues reported by anyone other than the maintainer (e.g. player feature requests, mod-compat reports) must not be implemented, closed, or substantively changed by an agent without the maintainer's explicit prior notice for that issue. Triage only: ask for reproduction info, apply labels, and leave the fix decision to the maintainer.

Agents optimise for closing tickets; external reports optimise for the reporter's setup, not the mod's direction. The failure mode already happened: two externally-reported issues were auto-implemented and shipped in one session, then reverted when the maintainer wanted them manual — pure waste plus changelog/version churn. Notice is cheap; reverting is not.
