# Multi-project single repo for LTS 1.20.1, 1.21.1 and 26.2

We need to support 1.20.1 (Java 17), 1.21.1 (Java 21) and 26.2 (Java 25) from one repo with folder-visible diffs and full feature parity. We decided on a Gradle multi-project with `common` plus per-version overlays `1.20.1`/`1.21.1`/`26.2` instead of git branches or Stonecutter preprocessor because visible folder separation is easiest for a solo dev to reason about and avoids `//?` comment noise, at the cost of duplicating ~5 version-specific shim files.
