# Bazaar acquisition — offline parent-owned boundaries

## Scope, not a released buying feature

`BazaarAcquisitionPlan` and `BazaarPurchaseAttempt` add the deficit/price/budget/delivery part of the shared purchasing infrastructure. They do **not** open Bazaar, enter an amount, parse a current native confirmation GUI, click a guessed slot, close a parent menu or expose an unfinished purchase toggle. A declared `Offer`/`MenuContract` is data supplied by the future adapter, not proof that an actual server button authorizes the transaction. The existing NPC Sell path is unchanged; this is not Bazaar selling.

The remaining native opening/navigation/quantity/current-price/cost parser and parent Visitor/Composter/Pest/Supercraft caller wiring are still required. Current-format/live release gates stay open. Existing installed2.0.17 and game settings are unchanged.

## Read-only acquisition planning

- An owning module supplies at most8 exact product requirements and explicit native material IDs, stack limits, per-product ceilings, whole-run money cap and total item cap. Defaults are empty/zero, never purchase authority.
- Complete canonical43-slot inventory is frozen. Unknown/missing/duplicate identities, protected/hotbar/equipment same-ID supplies and mismatched native products are refusals, not material credit. Only ordinary authorized main-inventory stacks count against the requested target totals.
- Buy only the missing quantity; duplicate needs are rejected. Require conservative capacity in explicitly empty main slots for every product—no assumed merging, hotbar, sacks or Stash destination. Already sufficient material produces no purchase requests.
- Resolve the exact unambiguous display name from the demand-loaded public catalog, not fuzzy item names. Fresh public order-book depth produces conservative whole-coin estimates, not a GUI total or an order.
- Estimates must fit each configured product and the shared original run budget. The passed budget's total limit cannot exceed the owning module's configured run cap; a larger unrelated/global budget cannot silently override that cap. A request's necessary `authorizes` check also reads the current available balance/holds; the final native GUI proof and atomic reservation are still mandatory. Reuse the same budget in the purchase attempt; never create a new budget for each product/retry.
- Planning does not reserve money, acquire roots, query the network, send commands or save/replay requests. Frozen inventory must be revalidated before native execution.

## One irreversible purchase attempt

- Reuse the exact current original Pest, Composter, Visitor or Supercraft root. No independent Bazaar feature/root is introduced. Do not lend this permission to a Book/Sell/Junk/Stash root, start Farming, or finish/release the parent from this subtask.
- Arming requires exact declared menu/ticket/native product/amount/cost facts, original world/root and full43/player-menu aliases, explicit capacity, physical empty cursor and fresh scoped server-applied empty cursor after the opening stamp. The future adapter must capture that stamp before the real owned opening and independently validate complete native menu/semantic button facts.
- Before a single guarded send, revalidate the original inventory and exact container/revision/button, current parent/context/lease and both physical/server cursor states within1s. The actual native sender must revalidate immediately on the client thread too; this boundary never substitutes a generic unguarded click.
- Reserve the GUI-validated worst total under the shared budget immediately before the supplied guarded callback. A definite REJECTED releases that reservation; exceptions, UNCERTAIN, cancellation or late/unknown receipt outcomes retain possible spending and never trigger a retry/refund.
- After SENT, freeze new inventory/cursor stamps. Success requires the exact server GAME purchase line/cost **and** an exact positive gain of the ordered native product in main slots only. Every changed slot needs both canonical and active-container applied receipts, unchanged unrelated/hotbar/equipment/protected inventory and fresh physical/server empty cursor. A stale local prediction, chat alone, another product, aggregate gain hiding a loss, wrong alias/world/root or unknown cursor is not success. Partial delivery can settle within the bounded15s window without another action.
- Success commits actual receipt cost, rounded conservatively to whole coins once. Deadline/world/Farm/manual/control loss is uncertain, does not clean up an unknown cursor or close a taken-over menu, and leaves the parent responsible for safe termination.
- Current supported proof requires the same owned container and explicitly main-inventory delivery. A server route which closes/replaces the GUI or delivers to Sacks/Stash needs its own validated transition/evidence; it cannot be guessed into this path.

## Evidence and verification

`BazaarAcquisitionPlanTest` exercises real immutable inventory/catalog/book/budget models: deficits, protected/duplicate/capacity refusal, exact names/IDs, quote age/depth/caps, larger-budget mismatch, live reservation changes and no-effect planning. `BazaarPurchaseAttemptTest` composes the real session, budget, item/cursor ledgers and a **declared test GUI**. It exercises all four allowed parents, standalone Supercraft, cross-world frame refusal, initial cursor authority, exact-once send, rejection/uncertainty, missing cost/slot/cursor, partial settlement, unrelated/equipment/hotbar/wrong native products, root/Farm/focus/deadline/manual cancellation and final same-parent cost commit. These are not current Bazaar GUI/native-click or server compatibility tests.

Read-only research on2026-10-27 found the official [Bazaar](https://wiki.hypixel.net/Bazaar) and [George](https://wiki.hypixel.net/George) URLs redirecting to the official Wiki closure announcement. Those returned pages supply no current role, price, amount or permission evidence. Do not infer live slots/costs from them or execute the historical upstream client to obtain authority. Current user-provided menu/tooltip evidence is still needed for the native transaction adapter; no account/profile/telemetry/author contact was added.
