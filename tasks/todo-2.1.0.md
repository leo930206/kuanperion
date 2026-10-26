# 2.1.0 progress (truthful checkpoints)

## 0 — rollback and scope
- [x] Existing clean 2.0.16 and origin confirmed; user-approved scope recorded.
- [x] Source Git bundle + installed JAR backed up; backup/pre-2.1.0 pushed to GitHub.
- [x] feature/2.1.0 created; installed game version/settings untouched.

- [x] Separate 2.0.17 hotfix released/installed on main: Garden Shift+G and Farming hotbar startup selection; synchronized into 2.1.0, no incomplete automation shipped.

## 1 — safe runtime
- [x] Pure feature IDs, capability/risk/resource ownership contract + tests.
- [x] World/session/focus/Mining/recovery/menu guards; bounded lease and stale-token rejection.
- [x] Atomic schema-aware preferences: defaults off, unknown keys retained, corrupt/future files not overwritten; strict UTF8/JSON, duplicate fields at every depth, trailing data and excessive nesting fail closed without changing original bytes.
- [x] Stash vertical slice: system signals, bounded attempts/deadline, cancellation, guarded actual command and local feedback (offline contracts/UI verified; live server results not yet verified).
- [x] Trade navigation/Stash UI and real settings persistence, no no-op toggles for unfinished workflows.
- [x] Server-applied cursor receipt path: exact active container/raw-applied state, bounded epoch/sequence/freshness, no local-prediction proof or cursor mutation; next item-transfer callers still pending.
- [x] Guarded menu observations/actions and server-only receipt contracts (pinned26.2 Mixin + fixture/unit tests; workflow integrations continue in phase2).
- [x] Existing Farming hold/resume token and stop/new-start/Mining/Jerry/world/death/disconnect/expected-menu invalidation paths; no task may arm Farming.

## 2 — Trade and acquisition
- [x] Protected inventory identities + exact allowlists + budgets/reservations/receipts (shared contracts; budget integration still required by buying workflows).
- [x] Bazaar shared primitives: on-demand anonymous public book estimates, bounded exact menu transitions and dual-receipt spend ledger; not yet wired to a buying feature.
- [ ] Bazaar acquisition actual GUI driver with title/identity/cost/amount proofs and caller wiring; confirm current server menu formats.
- [x] Auto Sell NPC vertical slice: real typed command/menu driver, exact allowlist/protection, bounds, fresh server removal receipts, independent preferences/UI; offline tests only.
- [ ] Auto Sell Bazaar path; validate current live NPC/Bazaar formats before release.
- [x] Book Combine exact preflight: authorized equal-level/single-enchantment pairs, protected identity and preserved result metadata; native26.2 NBT fixtures (no anvil actions yet).
- [x] Book Combine declared-layout protocol: ordered server-acknowledged inputs, fresh exact preview + explicit zero cost, authoritative result/recovery deltas and owned-menu cleanup; synthetic fixtures only, no runtime caller.
- [ ] Book Combine current native anvil layout/adapter, settings/service/UI, cancellation recovery and live format validation.
- [x] Pet Sell read-only preflight/native26.2 metadata projection: exact explicit type+rarity/XP/UUID caps, nested soulbound/active/transfer/attachment/candy/unknown-data protection, complete43 immutable inventory and duplicate identity veto. Anonymous public-item schema sampled with only redacted field facts retained; no account/profile query or runtime auction endpoint.
- [ ] Pet Sell current George offer/price/transfer/confirmation/recovery/server receipts, settings/service/UI and live validation; eligibility alone grants no disposal authority.
- [x] Junk native inventory disposal at an explicitly authorized Garden point: exact allowlist/protection, server receipts, limits/cancellation, per-world reconfirmation, independent settings/UI; offline tests/full38-state runtime verified, live drop not yet validated.

## 3/4 — Pest
- [x] Pest read-only observations: bounded scoped Tab, exact curated skin URL, official VACUUM IDs + explicit ability range, marker-associated entities, native26.2 immutable snapshots; read-only facts used by a separately guarded driver.
- [x] Loaded-target Pest core vertical slice: independent threshold/service, server-granted flight, bounded swept-body pathing/rotation/key/use, paired server Tab decrease + target departure, return/tool/look/support restoration before releasing Farming; pure/native boundaries/offline verification (not live capture validation).
- [x] Native-world context quorum and bounded server-applied absolute position receipt infrastructure (real packet fixtures/Mixin proof; now used by the guarded Pest travel caller).
- [x] Pest explicit opt-in strict-Tab plot travel, server absolute-position quorum before flight, per-run visit bound and original-plot/segmented exact-anchor return (offline verified).
- [ ] Complete Garden search/cleanup behavior and current server formats; validate actual server flight/capture/return behavior before release.
- [x] Pest UI rename/merge, independent automation and original notifications.
- [x] Explicit opt-in loaded full-trap release -> normal Pest capture -> exact return under one original Farm hold; strict Tab/marker/menu/proof and manual/cursor safety, offline verified.
- [x] Explicit independently configured trap plot, bounded shared travel budget/server arrival/loading observation, no trap-ID/plot-ID inference, release/capture/original return.
- [x] Delayed applied server trap-close observation retains the original hold; local/manual/ignored/stale close remains cancellation.
- [x] Bait declared direct-item transfer protocol/subtask: explicit protected43 material/count/capacity preflight, fresh pickup/deposit/owned remainder-return/cursor/NoBait receipts, same original Pest/Farm root and exact owned close; no native caller or live layout claim.
- [ ] Bait actual current native role/capacity/transfer gate, Pest parent navigation/dispatch, settings/UI/acquisition and live trap formats.
- [ ] Exchange/spray.
- [ ] Event/crop strategies and due-task scheduling.

## 5 — farm maintenance
- [x] Greenhouse read-only exact crop identities, bounded no-load native discovery, typed entity/block targets and watched server-applied harvest update receipts; no local prediction proof or unfinished toggle.
- [x] Shared fixed-owner legal-flight primitive extracted without widening Pest/Garden authority; original Pest vacuum/return facade behavior and method bodies verified.
- [x] Greenhouse configured plots/kinds/exact tool, bounded harvest -> server-confirmed original return, fixed-feature native flight/attack integration, independent persistent opt-in and actual Farming settings callbacks.
- [ ] Greenhouse live validation; offline native/gameplay fixtures are not a server success claim.
- [x] Composter inventory-source slice: exact configured Barn machine/IDs/unit values, guarded full-stack deposit, source/resource/cursor server proof, verified original return, independent external preferences and Farming UI.
- [ ] Composter Sacks source, budgeted material purchases and current live menu/machine/receipt validation.

## 6 — Visitor/Supercraft
- [x] Shared official item catalog: bounded strict JSON, unambiguous exact names/IDs, demand-only anonymous transport/receipt-time cache; no startup consumer or remote author lookup.
- [x] Visitor exact selected-title/semantic-role offer parsing, explicit material IDs, protected main-inventory deficits and full opportunity-cost estimates; native43-slot fixtures.
- [x] Visitor inventory-only submission protocol: explicit rules, protected same-ID veto, one guarded attempt, exact GAME accepted line + fresh applied removals + empty cursor; runtime inventory-backed caller wired.
- [x] Visitor bounded complete Tab queue and unique hologram/nearby non-listed-character identity snapshots; unknown/duplicate/missing/ambiguous facts cannot pick a target.
- [x] Visitor inventory-only executable navigation/menu service/UI: typed server-confirmed Barn travel, dual identity, explicit rules, demand-only official data, one guarded submission, scoped applied-close and fresh cursor proofs, exact original return.
- [ ] Visitor budgeted acquisition and current live offer/accepted-line/inventory/cursor/return validation.
- [x] Visitor UI rename/merge, independent off-by-default automation and retained original notifications.
- [x] Supercraft read-only exact-batch preflight: explicit output/ingredient/consumption/craft caps, complete protected43-slot inventory and capacity, immutable baseline; recipe data does not authorize a GUI action.
- [x] Supercraft single-attempt receipt contract: current complete baseline, guarded-result window, exact requested input/output deltas backed by every affected server slot and empty cursor, no replay/prediction/manual/world/clock/timeout success. No native caller or placeholder toggle.
- [x] Historical official advanced-preview read-only contract: complete exact recipe/output/selected amount/material/source/destination facts, inventory-only allocation, no fixed slots or unknown-cost skipping; current2026 live compatibility remains unverified.
- [x] Supercraft abstract exact-selected-batch task: one root/catalog/typed recipe/guarded-attempt, strict preview -> server proof -> unchanged original pose/tool and owned close before valid Farming release; standalone native adapter verified below, native service/UI exact-batch caller now wired below.
- [x] Native Supercraft adapter: fixed root/thread/world, lazy catalog, exact recipe callback, complete fresh server54 + inventory43/grid/stack-limit/source/cost/cursor proof, one guarded attempt/uncertainty, exact owned close and pending-data disposal; single fixed-owner typed recipe/service/Other UI caller now wired below; unknown current format remains refusal.
- [x] Supercraft exact-selected-batch runtime service/Other UI: strict external recipe/caps, off/unconfigured defaults, one deferred expiring manual request, opt-in interval, real typed recipe/native guarded attempt, complete server receipt/original-hold proof, active/idle/pending cancellation and independent settings. Offline verified only.
- [ ] Supercraft current live recipe/source/cost formats, actual quantity-setting UI where needed, broader recipe/amount behavior and live receipts/original-pose validation.

## 7 — release gate
- [x] NPC/Junk service entry points also reject unavailable complete inventory scans; Junk anchor/status/enable reads fail closed, explicit preferences are preserved without inventing current-world authority, and a successful manual request uses its acquired world without another fallible native read. Idle observation failures recover only from a later fresh guarded dispatch.
- [x] Shared task acquisition rejects null/throwing or invalidated-generation observations before ownership; NPC/Junk complete-list preflight faults are inert and Junk running context/location/menu faults revoke only the original root, without disposal/retry or swallowed safety-stop failures. Other native/service lifecycle observations remain under the unchecked full matrix below.
- [x] Trusted Jerry automatic sethome does not preempt held Farming; original independent stop toggle/message/beam order and subsequent manual-command priority verified with actual composed method bodies and a negative old-route mutation.
- [x] Jerry stop applies to all eight existing new services including Supercraft pending requests, standalone/manual tasks and idle automatic timers with Farming off; explicit per-feature re-enable required, idle preferences retained.
- [ ] Extend global-stop/priority matrix to future Pet Sell/Book Combine and full due-task handoffs before release.
- [ ] All approved modules function, all old UI/gameplay/preferences regressions pass.
- [x] Shared session observation faults: null/throwing context/menu/transition reads revoke only original root/hold; callback owner/contract changes (including same-object rebinding) cannot cancel newer work or install old menu bindings. Pause callbacks cannot return or cancel a newer owner's handle; stop-callback failures remain visible.
- [ ] Remaining full lifecycle/hand-off/manual stop/Mining/Jerry/disconnect/death/error test matrix and live validation.
- [x] Pest/Greenhouse/Composter/Visitor terminal cleanup now precedes successful original Farming release; failed release/disposal/context/expiry stops first, then an exact one-use same-level/no-new-root physical-input token reconciles legacy key clearing. Actual finish/native bodies plus full Visitor failure path tested; other release gates remain open.
- [ ] Audit own-settings GUI coexistence with claimed server menus; never silently replace the user's settings screen or claim uninterrupted operation without proof.
- [x] Settings hotkey/local-command entry refuses while a shared task, server container, carried item or loading overlay is active; read-only guard leaves original work/screen intact, no queued replay, and legacy Farming/Mining/title/close behavior remains. Delayed native packets and all manual-feature UI paths remain part of the unchecked full audit above.
- [x] Pest manual UI request waits for settings to close before acquiring a root or touching flight/tools; one expiring world/Farming-context-bound intent, fresh dispatch validation, visible stop/status and complete cancellation without persistence/replay. Other manual task UI paths and delayed packets remain under audit.
- [x] Greenhouse/Composter/Visitor manual UI requests use the same bounded nonpersistent world/Farming-context-bound intent semantics before ownership, data or native effects; stop/status callbacks and fresh actual-task dispatch tested. Trade manual paths and delayed native menu packets remain under audit.
- [x] Auto Sell NPC/Junk manual UI requests defer before ownership/commands/inventory/disposal; explicit scope and full-inventory exception retained, pending stop/status and original context/expiry cancellation tested. Manual look/inventory/ordinary-command hooks clear pending Trade and revoke active ownership before cleanup so a taken-over menu is never closed; trusted Jerry sender retains original handoff. Delayed vanilla packets and live audit remain unchecked.
- [x] Current source/production archive static regression audit: fixed anonymous official GETs plus explicit user-configured outbound webhook; no upstream runtime class/client, agent/test/private-path payload or unapproved bundled dependency. Five rendering JAR hashes pinned, GPL/source notices preserved, in-memory negative mutations tested. This is not a network sandbox or a complete information-flow/security proof; reruns on every build.
- [ ] Isolated runtime/UI checks and explicit server-test limitations.
- [ ] Build payload equality, source/JAR backup, settings and unrelated mods preserved.
- [ ] Only after all above: publish/tag/install 2.1.0, update main.

### Next Pest increment (in progress, not released)
- [x] Explicit opt-in local four-point sweep when global pests are known but no loaded target is in range; same native flight fence, discovery interrupts sweep, exhaustion returns INCOMPLETE and stops instead of claiming a cleared Garden.

- [x] Vanilla startAttack/startUseItem HEAD manual priority catches released fast clicks before native action, noncancellable; isolated 26.2 applied-Mixin/UI regression passes.
