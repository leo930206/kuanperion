# Pest crop/event target planning (not an executable strategy service)

## Implemented read-only policy

PestStrategyPlan represents the requested crop/event strategy's selection step without importing the historical client's fetchers, global settings, worker threads, hidden service dependencies or actions. Mode is explicitly Off, Feast, Jacob, or Jacob-then-Feast. Configured exact crop priorities map only to explicitly declared native spray/record IDs and optional day/night preference; these are target data, not a menu click, cost proof, item allowance or game command.

Defaults are Off with no profiles/priorities/fallback, and no configuration or runtime caller is added to the existing Pest notification/automation flags. No unfinished strategy toggle, network request, startup polling, persistent pending decision, Farming-state rewrite or native menu action is exposed.

- Feast and Jacob use independent explicit ordered crop priorities, not the first arbitrary active crop. A crop must occur in both the current event facts and its configured priority list with a matching profile. Unknown/unlisted active crops do not become automatic authorization.
- Jacob-then-Feast considers Feast only after fresh same-world Jacob facts positively say inactive. Known active but unconfigured Jacob crops do not silently fall through to another action. A selected Jacob crop does not require irrelevant Feast facts.
- Facts distinguish Unknown, Inactive and Active; missing is never inactive. They are immutable, bounded, duplicate-free and tied to the current owner world plus local monotonic receipt time. The inclusive freshness window is30s. Future, stale, missing, contradictory or other-world facts cannot cause selection or fallback. A future native/approved-data adapter must prove these facts and capture receipt time itself, not reinterpret an external epoch timestamp as nanoTime.
- Explicit fallback can apply only when all events relevant to the chosen mode are positively known inactive. No configured fallback means leave the user's current state alone; never reset to an implicit default spray, record or garden time. Profiles with no declared change are not fabricated jobs.
- Every effect still needs its own real current root/thread/world/menu/native target/item/quantity/cost/cursor/server proof and explicit permission. A target profile cannot bypass purchasing budgets or infer that changing garden time/records is free. This policy acquires/releases no gameplay ownership and cannot resume Farming.

## Provenance and exclusions

Static, non-executing inspection of the user-supplied GPL client identified the original three-mode policy and event-driven spray/record/time intent. The old implicit first-active-crop fallback and remote event-feed implementation are **not** transplanted. No historical13-crop mapping is claimed current; test IDs beginning TEST_ are symbolic fixtures, not live product IDs or server labels. Current item/event mappings must be established separately before a native adapter uses them.

The historical strategy's third-party event service is not contacted or bundled. The runtime's existing fixed anonymous official-resource GET allowlist is unchanged. No author/Aether/OAuth/IRC/telemetry/remote Discord/update integration is introduced, and the original client JAR is never executed or distributed.

## Official data research boundary

On2026-10-28 the [official Public API reference](https://api.hypixel.net/) was inspected read-only. Its documented public resource routes did not identify a global Harvest Feast/Jacob event feed. That is not proof that no other endpoint exists, and it is not permission to guess one. The documented [Garden-by-profile route](https://api.hypixel.net/#tag/SkyBlock/paths/~1v2~1skyblock~1garden/get) is account/profile data, not a replacement for current global event evidence; no key/profile lookup or new runtime endpoint was added. Prefer separately validated current game facts; unknown facts keep the strategy inert.

## Verification and remaining work

PestStrategyPlanTest exercises the real pure implementation: default Off, independent priorities, Jacob precedence, known-inactive-only fallback, unlisted active crop refusal, unknown/stale/future/wrong-world facts, inclusive freshness, contradictions/duplicates/invalid profiles and immutable configuration copies. These tests prove target planning only—not a current event parser, spray/record/time menu, task integration or successful server change.

Still required: current server/approved data facts without forbidden integrations, validated native profile mappings and actual setters with cost/item/receipt guards, parent lifecycle/manual/Jerry interruption and explicit settings/UI. Pest event/crop strategy as a complete feature remains unchecked in the release checklist. Existing core Pest capture/traps/return and all other features are unchanged by this leaf policy.
