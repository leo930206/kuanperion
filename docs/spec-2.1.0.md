# Kuanperion 2.1.0 — approved automation migration

Approved in conversation 2026-10-24. Target release starts at 2.1.0. Work on feature/2.1.0, not main; Stable 2.0.17 (the separately released hotfix) remains installed until the 2.1.0 release gate. An incomplete development build is not a completed migration.

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

All seven phases in tasks/todo-2.1.0.md are functional, wired to the requested pages, tested with failure/cancel/timeout paths and existing regressions green. Notifications and automation independent; no forbidden runtime integration; retained settings; protected inventory/spend limits; version/README/changelog truthfully match. Only then tag/install 2.1.0 and advance main. Until then publish honest checkpoints on feature/2.1.0, retaining stable 2.0.17 as the playable version.

## User amendment — 2026-10-24

The former /home shortcut must display /warp garden, send the exact command `warp garden`, and report `已發送 /warp garden。` in local chat. Keep key.kuanperion.home and hotkey.home.modifiers as persistence IDs so existing custom/cleared bindings survive. Shift+G is the default (user amendment 2026-10-24); keep saved custom/cleared bindings unchanged. Do not rewrite historical changelogs.

## Two release lines — 2026-10-24

Stable 2.0.17 was released from main/2.0.16 with only the Garden shortcut and Farming startup hotbar selection. These same changes are synchronized to feature/2.1.0. Do not merge incomplete automation into main or replace the stable installed JAR while continuing the upgrade. Shift+G applies to new/reset bindings; saved custom, cleared and former defaults remain unchanged. Startup selection respects the existing tool-detection toggle; runtime wrong-tool pauses remain manual.

## Visitor inventory-backed increment — 2026-10-25

The Farming card is Visitor, with automation and original notifications independently toggled. Explicit exact visitor/material allowlists and a nonzero whole-offer value ceiling are mandatory; defaults never authorize a material submission. Present scope uses main-inventory materials only, no acquisition/refusal. Both owned and missing materials have full opportunity-cost estimates from fresh anonymous official Bazaar depth; the item catalog is queried on task demand only, no startup polling/account fields. Missing/unknown/stale or over-limit offers are skipped and reported as partial, not silently purchased.

A single root acquires the original Farming handoff, takes typed Barn travel with server position receipts, revalidates dual hologram/NPC identities, opens an exact selected-title menu and clicks a uniquely identified semantic Accept Offer role once. Protected/UUID/hotbar/equipment same-ID stacks veto the whole submission. Confirmation requires exact server GAME acceptance, all fresh applied inventory losses and fresh physical/server empty cursor evidence. A Visitor-specific opaque one-use server HEAD/TAIL close token preserves only that pending root claim, invalidating old cursor scope and requiring a newer scope0 receipt; a local close remains cancellation. No preview/disappearance equals success, no retry or cursor repair. Completion or honest partial return requires original plot/anchor/tool/look/flight restoration before the valid handoff is released. Native/world/manual/Jerry lifecycle guards include Visitor; future buying/Supercraft and live validation remain unchecked release work.

## Supercraft preflight/receipt boundary — 2026-10-25

A declared Recipe is data only, never proof of an actual server menu or click authority. The read-only planner requires explicit output/ingredient allowlists and output/total-consumption/craft-count limits, an exact whole number of requested recipe batches, complete43-slot inventory, protected same-ID input veto and conservative output capacity; it does not buy, maximize or infer missing ingredients. A future native adapter must independently verify the current actual recipe, exact quantity and cost, revalidate the full immutable baseline and record its guarded result atomically on the client thread before server packets apply. The read-only proof window then requires every changed main slot's fresh applied receipt, exact ingredient losses/output gains and a fresh physical/server empty cursor. Existing hotbar/equipment/unrelated items may not be changed; ambiguous/late/manual/world/clock changes never succeed or retry. No native caller, action, command, UI toggle or automatic sequence is exposed by these contracts.

The historical official Supercraft source/quantity screenshots inform a separate read-only preview parser (see docs/supercraft-protocol-notes.md). It requires exact recipe/output IDs, selected batches, ingredient quantities and Inventory-only source allocation. Main inventory sufficiency alone is not source authority; other storage allocations, unknown costs/destinations and missing advanced-mode details deny the preview. This parser is not a current2026 live-format claim or native executor and never adds an unfinished toggle.
