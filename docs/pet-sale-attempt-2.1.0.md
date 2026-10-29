# Pet Sell single-attempt outcome boundary

2026-10-29, development only. `PetSaleAttempt` is a Minecraft-independent parent-owned guarded-result/proof component, **not** a George menu parser, inventory transfer/return driver, automatic seller or exposed feature. Current native menu/price/payout evidence is still required; no slot/title/command or acknowledgement string was guessed.

## What it does

- Accepts only an exact pet already selected by the private immutable `PetSalePreflight.Plan`, with the complete43-slot baseline still unchanged. It additionally rejects duplicate/malformed UUID observations and malformed empty stacks. Other pets, equipment, hotbar and unrelated items cannot be lost as part of this one sale.
- Requires an explicit positive minimum payout and bounded exact declared quote, the original PET_SELL session/lease/world/Farming generation, a fresh complete player/menu-alias frame and a physically empty cursor. These facts are data, not native button authorization. A future adapter must independently establish current semantic quote/role/custody facts and atomically guard the irreversible operation.
- Invokes the caller's typed guarded native-result boundary at most once after preflight. A rejected result remains rejected; null/throwing/uncertain results and cancellation during send cannot be retried or relabeled success. Pre-send/reentrant notices and receipts are excluded by sampling item/cursor stamps only after an accepted return.
- Completion requires the selected pet alone to disappear, every other baseline slot unchanged, matching fresh applied server receipts for both the main-inventory slot and its visible menu alias, a fresh server empty-cursor receipt plus current physically empty cursor, absence of the selected UUID from all menu slots, and an exact declared server-sale acknowledgement bound to this world/UUID/quoted payout. There is no actual native acknowledgement parser or purse/balance projection yet; `onServerSale` is only the future adapter's typed boundary, **not** evidence that a live server actually provides that tuple or that a display name can supply UUID authority.
- The proof refuses changed menu scope, incomplete/stale/future observations, world/owner/hold/blocker/clock/timeout failures and any other inventory mutation. It preserves a later replacement owner. Context-read reentrancy cannot cause an old preflight to send twice or turn an already cancelled proof into confirmed.
- It has no direct root acquire/release, Farming end/resume, menu/cursor close/repair, item movement, command, settings write, profile lookup or networking implementation. Its explicit guarded-result callback is the only delegated native-effect boundary; that callback remains the future adapter’s responsibility. The future parent remains responsible for safe cancellation, custody recovery, owned close, return and final original-hold release. Server-close/changed-menu recovery is deliberately not supported by this proof yet; ambiguity remains UNCERTAIN.

The coordinator adds a passive exact-lease identity query, not a new permission path: it does not validate/expire a lease, inspect a potentially stale context or mutate a replacement owner. This proof combines it with its own existing world/clock/blocker/hold checks and the unchanged native guard requirement. Existing validated isOwner/permits behavior is unchanged.

## Evidence and limits

A missing-API RED preceded the implementation. Initial real-model tests then passed; adversarial tests exposed three genuine behavioral REDs in the initial implementation: cancellation during a preflight still sent, nested accepted preflight was overwritten/re-sent, and cancellation during a completion observation could be overwritten as confirmed. Post-read state checks fixed those cases without suppressing parent/root errors.

Tests execute the actual proof, preflight, session/coordinator and receipt ledgers with declared GUI/notices and controlled native-result callbacks: exact success, every missing evidence component, other-pet/hotbar/equipment changes, stale/mismatched notices, invalid original owner, changed immutable baseline, nonpositive/under-minimum/unbounded quotes, all native-result outcomes/throw, pre-send evidence, world/focus/time/root loss, replacement owner and read/send reentrancy. No real George interaction, coins or inventory are used. Source guards keep this component without a runtime caller, keep PET_SELL unavailable and retain the metadata/native projection restrictions.

Current GUI semantics, transfer/cursor custody, server payout format/source, actual sale/return receipts, service/settings/Trade UI, lifecycle matrix integration and live acceptance remain substantive unfinished work. A synthetic declared notice cannot be promoted into a current native parser or a successful-sale claim.

A further adversarial review exposed three behavioral REDs when the coordinator lease was cleared/replaced while the shared session handle had not yet been polled: stale preflight send, replacement during a read and stale completion. Exact passive identity checks before/after observations and after send now deny all three without invalidating a foreign new-world Stash lease. This is why merely checking session.isCurrent was insufficient for this new component.


## 後續原生接線 — 2026-10-29

以上保留原元件檢查點語意。後續已加入 PetSaleCustody 私有入庫 token、PetGeorgeTask／PetGeorgeController、設定／service／Trade UI、同報價一次確認與真正伺服器 Close HEAD/TAIL 路徑；見 george-sale-2.1.0.md。當前完整回歸仍進行中；原 typed notice 並沒有因此變成已證明的現行伺服器格式。
