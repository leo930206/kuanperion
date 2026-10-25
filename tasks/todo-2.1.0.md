# 2.1.0 progress (truthful checkpoints)

## 0 — rollback and scope
- [x] Existing clean 2.0.16 and origin confirmed; user-approved scope recorded.
- [x] Source Git bundle + installed JAR backed up; backup/pre-2.1.0 pushed to GitHub.
- [x] feature/2.1.0 created; installed game version/settings untouched.

- [x] Separate 2.0.17 hotfix released/installed on main: Garden Shift+G and Farming hotbar startup selection; synchronized into 2.1.0, no incomplete automation shipped.

## 1 — safe runtime
- [x] Pure feature IDs, capability/risk/resource ownership contract + tests.
- [x] World/session/focus/Mining/recovery/menu guards; bounded lease and stale-token rejection.
- [x] Atomic schema-aware preferences: defaults off, unknown keys retained, corrupt/future files not overwritten.
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
- [ ] Pet Sell eligibility/confirmation/protection and menu flow.
- [x] Junk native inventory disposal at an explicitly authorized Garden point: exact allowlist/protection, server receipts, limits/cancellation, per-world reconfirmation, independent settings/UI; offline tests/full38-state runtime verified, live drop not yet validated.

## 3/4 — Pest
- [x] Pest read-only observations: bounded scoped Tab, exact curated skin URL, official VACUUM IDs + explicit ability range, marker-associated entities, native26.2 immutable snapshots; read-only facts used by a separately guarded driver.
- [x] Loaded-target Pest core vertical slice: independent threshold/service, server-granted flight, bounded swept-body pathing/rotation/key/use, paired server Tab decrease + target departure, return/tool/look/support restoration before releasing Farming; pure/native boundaries/offline verification (not live capture validation).
- [x] Native-world context quorum and bounded server-applied absolute position receipt infrastructure (real packet fixtures/Mixin proof; now used by the guarded Pest travel caller).
- [x] Pest explicit opt-in strict-Tab plot travel, server absolute-position quorum before flight, per-run visit bound and original-plot/segmented exact-anchor return (offline verified).
- [ ] Complete Garden search/cleanup behavior and current server formats; validate actual server flight/capture/return behavior before release.
- [x] Pest UI rename/merge, independent automation and original notifications.
- [x] Explicit opt-in loaded full-trap release -> normal Pest capture -> exact return under one original Farm hold; strict Tab/marker/menu/proof and manual/cursor safety, offline verified.
- [ ] Unloaded trap destination/travel, bait replenishment and current live trap formats.
- [ ] Exchange/spray.
- [ ] Event/crop strategies and due-task scheduling.

## 5 — farm maintenance
- [x] Greenhouse read-only exact crop identities, bounded no-load native discovery, typed entity/block targets and watched server-applied harvest update receipts; no local prediction proof or unfinished toggle.
- [x] Shared fixed-owner legal-flight primitive extracted without widening Pest/Garden authority; original Pest vacuum/return facade behavior and method bodies verified.
- [ ] Greenhouse configured plots/tools/targets executable harvest -> return, native flight integration and live validation.
- [ ] Composter inspect/source/purchase/fill -> return.

## 6 — Visitor/Supercraft
- [ ] Visitor offers/rules/valuation/budgeted buy/submit -> return.
- [ ] Visitor UI rename/merge, independent automation and original notifications.
- [ ] Supercraft recipe/amount/material/menu/deadline -> return, Other UI.

## 7 — release gate
- [ ] All approved modules function, all old UI/gameplay/preferences regressions pass.
- [ ] Lifecycle/hand-off/manual stop/Mining/Jerry/disconnect/death/error test matrix.
- [ ] Audit no forbidden integration, no bundled original client, no secrets.
- [ ] Isolated runtime/UI checks and explicit server-test limitations.
- [ ] Build payload equality, source/JAR backup, settings and unrelated mods preserved.
- [ ] Only after all above: publish/tag/install 2.1.0, update main.

### Next Pest increment (in progress, not released)
- [x] Explicit opt-in local four-point sweep when global pests are known but no loaded target is in range; same native flight fence, discovery interrupts sweep, exhaustion returns INCOMPLETE and stops instead of claiming a cleared Garden.

- [x] Vanilla startAttack/startUseItem HEAD manual priority catches released fast clicks before native action, noncancellable; isolated 26.2 applied-Mixin/UI regression passes.
