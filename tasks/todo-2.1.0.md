# 2.1.0 progress (truthful checkpoints)

## 0 — rollback and scope
- [x] Existing clean 2.0.16 and origin confirmed; user-approved scope recorded.
- [x] Source Git bundle + installed JAR backed up; backup/pre-2.1.0 pushed to GitHub.
- [x] feature/2.1.0 created; installed game version/settings untouched.

## 1 — safe runtime
- [x] Pure feature IDs, capability/risk/resource ownership contract + tests.
- [x] World/session/focus/Mining/recovery/menu guards; bounded lease and stale-token rejection.
- [x] Atomic schema-aware preferences: defaults off, unknown keys retained, corrupt/future files not overwritten.
- [ ] Stash vertical slice: system signals, bounded attempts/deadline, cancellation, guarded actual command and local feedback.
- [ ] Trade navigation/Stash UI and real settings persistence, no no-op toggles for unfinished workflows.
- [ ] Guarded menu observations/actions and receipt contracts (no blind slot clicking).
- [ ] Existing Farming hold/resume token and every stop/manual override invalidate path.

## 2 — Trade and acquisition
- [ ] Protected inventory identities + exact allowlists + budgets/reservations/receipts.
- [ ] Bazaar acquisition with title/identity/cost/amount contracts and safe failure.
- [ ] Auto Sell actual NPC/Bazaar paths, limits and exclusions.
- [ ] Book Combine pairing/levels/metadata/protection and menu flow.
- [ ] Pet Sell eligibility/confirmation/protection and menu flow.
- [ ] Junk exact allowlist/protection/location/drop limits and cancellation.

## 3/4 — Pest
- [ ] Observation/threshold/tool/target/movement/vacuum core -> return.
- [ ] Pest UI rename/merge, independent automation and original notifications.
- [ ] Trap cleanup/bait replenishment.
- [ ] Exchange/spray.
- [ ] Event/crop strategies and due-task scheduling.

## 5 — farm maintenance
- [ ] Greenhouse configured plots/tools/targets/harvest -> return.
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
