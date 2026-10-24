# Kuanperion 2.0.15 verification

2026-10-24 / Minecraft 26.2 / Java 25 / Apple M1.

## Scope and acceptance
- Append-only Mining selection, every new unique type prepends; original relative order and partial sessions survive a restart of selection. Maximum 10 entries across UI, selection handler, persistence and legacy entry. Session progress counts only new types. Invalid/duplicate types do not consume capacity.
- Remaining capacity is live, not cached: three saved types allow at most seven, full list disables editor/action, deletion frees a slot. Empty list accepts ten; no forced eviction. Only related selection help text changed.
- With successful Jerry auto sethome and Jerry chat enabled, exactly `發現 color Jerry，且已發送 /sethome`; with Jerry chat disabled, exactly the manual shortcut feedback `已發送 /sethome。`. Both use the same prefix/style; no extra Jerry label/line. Send failure remains honest. Discord/screen payloads and auto-sethome-off behavior stay unchanged.

## Test strategy
Test-first failures for missing remaining-slot methods and previous exact Jerry chat format. Actual production start/right-click methods run against fake mouse/world boundaries: three-plus-seven, prepend order, unchanged old order, duplicate/air/bedrock rejection, no GUI/held-click penetration, full-cap guard, per-click persistence, deletion and partial-session restart. Jerry integration asserts exact strings for all four colors and both chat settings, plus failure paths. UI model checks live maximum, zero-capacity disabled action and finite input. Runtime is isolated, fake account, no server/real commands/Webhook/credentials. No real Hypixel acceptance is claimed.

## Result
PASS: full test.sh (including actual selection/Jerry integration), production compile, UI model, and byte-preserving legacy migration. Full 34-state isolated client passed at `/private/tmp/kuanperion-ui-smoke-2.0.15`; real config round-trip retained all ten ordered IDs and zero remaining slots. A focused visible-target-category run at `/private/tmp/kuanperion-ui-smoke-2.0.15-capacity` also passed: input verified on-screen before clicking, full list cannot focus the editor, three saved types allow editing and committing 99 clamps to 7. Screenshots 33/34 from the focused run visually inspected: no overlap, clear full-cap state and seven-slot range. Actual Hypixel right-click/server command execution was not tested; fake boundaries exercise production selection handlers and exact chat success/failure strings.

Release backs up tracked source, previous own JAR and generated build output (avoids Finder .DS_Store cleanup races), rebuilds in the real repository and compares all ZIP entry payload hashes to the runtime-tested JAR. Existing live-game settings are never copied/overwritten; ongoing saves during build are preserved by capturing the latest hashes immediately before install. Other mods are checked unchanged.
