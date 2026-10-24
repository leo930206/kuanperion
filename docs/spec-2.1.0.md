# Kuanperion 2.1.0 — approved automation migration

Approved in conversation 2026-10-24. Target release starts at 2.1.0. Work on feature/2.1.0, not main; 2.0.16 remains installed until the release gate. An incomplete development build is not a completed migration.

## Objective and approved surface

| Sidebar (order) | Modules |
|---|---|
| Farming | Farming, Pest, Greenhouse, Visitor, Composter |
| Mining | existing Mining |
| Trade | Auto Sell, Book Combine, Pet Sell, Junk, Stash |
| Other | existing Jerry detection, Discord Webhook, Supercraft |
| Setting | existing UI/theme/key configuration; necessary global automation limits |

Pest replaces/merges Pest notification; Visitor replaces/merges Visitor notification. Automation enable and notification enable are independent for both: notifications work with automation off and automation works with notifications off. Preserve existing notification flags/channels/durations/thresholds; never migrate a notification-enabled flag into automation-enabled.

Pest includes cleaning/vacuum capture, trap cleanup, bait replenishment, exchange, spray and crop/event strategy. Greenhouse includes configured plot/tool/target harvest. Composter includes crop/fuel inspection and replenishment from configured sources. Visitor includes offer parsing, valuation, explicit acceptance/rejection rules, material acquisition and submission. Trade includes allowlisted NPC/Bazaar sales, book pairing/combination, configured pet sale, protected-item-aware junk disposal and bounded Stash pickup. Supercraft includes configured recipes/quantities/intervals and a bounded menu sequence.

## Dependencies and orchestration

Build local task ownership, observation snapshots, state transitions, guarded commands/menu actions, inventory/budget protection, persistence, then individual workflows. No import of upstream global bootstraps, MacroStateManager, MacroWorkerThread, provider ServiceLoader or their hidden dependencies.

One active gameplay owner, stable session/world generation, cancel and timeout on every task. New movement/GUI/hotbar tasks must hold existing Farming without changing its user's enable preference, own only their input resources, exclude intentional pauses from stall detection, and resume only when a valid resume token still matches the same manually armed Farming session. Manual stop, Jerry stop, Mining, recovery, death, disconnect/world change or unexpected menu invalidate that token. Never restart Farming just because an old task finished. Stash is command-only and must not claim movement, rotate or close the user's menus.

Automatic post-Pest handoff follows due/configured tasks: traps/bait -> Greenhouse -> Composter -> Supercraft -> Visitor -> existing Farming. This is not permission for every timer to preempt other tasks. Capacity/money problems may request configured Trade helpers, not run arbitrary extra features. Shared Bazaar acquisition is infrastructure, with settings in the owning module, not an extra unapproved page.

## Persistence and safety

New automation preferences live in game config/kuanperion/automation.json, schema-versioned and atomic; unknown keys preserved, future schema read-only, malformed data never silently overwritten. Existing properties/theme/options/color presets remain in place. New automation defaults false. Runtime ownership, requests, resume tokens and pending purchases never persist/replay across launch or server change.

Spending defaults to zero budget and destructive operations to empty allowlists. Missing configuration means no purchase/sale/drop. Match explicit item identity/metadata, protect held tools, armor, locked/favorite items and configured exclusions. No broad lore substring may authorize disposal. Require expected menu title + container identity + slot identity/lore + quantity/cost checks before any irreversible click. Unexpected/changed/ambiguous menus stop without guessing.

Legacy Mining's 10 saved target types, prepend selection, shortcuts, Jerry behavior, mute/stall controls, UI resizing/hover and external settings persistence remain regression requirements. Chat prefix unchanged; body #00D8F5 / semantic highlights #0088D1, all fragments nonbold.

## Allowed / prohibited integrations

Only necessary game activity/price queries may be considered, through explicitly audited official/public data endpoints. No Aether service contact, author accounts, OAuth, IRC, telemetry, remote Discord control, screenshot upload or updater. The user's existing outgoing Discord notifications remain allowed. Freecam, Freelook, window pinning, ungrab mouse and unrelated features are excluded. Do not execute or distribute the original Aether client JAR. Retain GPL/source notices and provenance for adapted code; removing runtime connectivity does not mean deleting attribution.

## Project structure and commands

Java 25 / Minecraft 26.2 / Fabric Loader 0.19.5 + existing Fabric API, no new runtime dependency without approval.

- src/main/java/dev/kuan/kuanperion/automation: independent policies/task runtime/persistence, then workflow subpackages.
- KuanperionClient: thin tick/message/legacy-state bridge only; don't transplant the upstream monolithic client.
- src/test, scripts/test-*.sh/.py: pure state/contract tests and actual-production-method boundary harnesses.
- src/smoke: isolated runtime UI verification; never package smoke classes in the release.

Commands: ./scripts/test.sh; ./scripts/build.sh; ./scripts/test-ui.sh; ./scripts/test-ui-migration.sh. New pure automation tests run via ./scripts/test-automation-runtime.sh. git diff --check. Source/JAR/network allowlist inspection, rebuilt payload hash equality and isolated client checks before installation. Live server outcomes are not implied by mocked or offline verification.

Code style: small deterministic state machines, client-thread effects through typed boundaries, no sleeps or busy loops in a game tick. Example: `if (!context.canIssueCommand()) return;` before a typed allowlisted command. Unknown observations stop, rather than fabricating success.

## Definition of complete

All seven phases in tasks/todo-2.1.0.md are functional, wired to the requested pages, tested with failure/cancel/timeout paths and existing regressions green. Notifications and automation independent; no forbidden runtime integration; retained settings; protected inventory/spend limits; version/README/changelog truthfully match. Only then tag/install 2.1.0 and advance main. Until then publish honest checkpoints on feature/2.1.0, retaining 2.0.16 as the playable version.
