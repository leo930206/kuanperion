# Pest Trap Bait button — guarded refill and optional acquisition

This implements a separate source-style **nonempty Trap Bait button** route. It does not make the older declared direct-item/capacity protocol a native feature. Supplied PestTrapManager.findBaitSlot/runRefillSequence identifies a nonempty role and transfers an inventory stack; the local exact role title/lore, no-fee grammar and receipt combination are **provisional adapter assumptions**, not verified current-server facts.

## Actual wiring

PestPolicy.bait is independent, default-off consent with an empty exact bait ID and zero per-trap/run withdrawal ceilings. Old Pest notification/trap/travel flags grant no material consumption. PestService receives the real shared InventoryReceipts/CursorReceipts; PestTask uses the original Pest root and Farming hold to approach a unique known No Bait marker, run PestBaitButtonTask, finish any own remainder return, close the frozen owned menu, and use the existing original-pose return before releasing Farming. Bait-processed trap IDs are separate from released-full-trap IDs.

Only main-inventory9..35, a protected-item policy and a whole existing source stack are eligible. The whole withdrawn stack is charged against the original run even if some returns; no net-consumption refund can turn concurrent gifts into extra authority. Oversized stacks are not split or partially authorized by a wished-for count. Optional material acquisition is separately authorized by explicit native ID, stack limit, stock target and original-run Coins cap. Zero/empty defaults never buy. The configured stock target must fit both the whole-source per-trap cap and remaining run withdrawal ceiling.

The private one-use transfer action binds root, native level/menu identities, full43 items, exact menu, cursor receipts and typed step. AutomationMenuActions.sendPestBaitNative is package-private with only PestFlightController as caller; generic sendNative still refuses all carried items. The narrow path rechecks physical source aliases, slot activity/placement/capacity, actual cursor, complete native inventory and private action before one PICKUP. It cannot become a general cursor repair/THROW/QUICK_MOVE API.

Pickup needs both canonical and active-container source removal plus own cursor. Deposit derives consumption from actual own-item deltas and fresh No Bait removal, not inferred server capacity. An exact local GUI-marker swap prediction is wait-only; a server-confirmed GUI marker is not owned bait. Return needs paired own-source restoration and empty cursor, without requiring an unrelated extra GUI-role packet. Closing revalidates settled full state and the frozen physical menu identity. Uncertain/manual/world/menu outcomes do not retry, repair, close a taken-over GUI or rearm Farming.

## Regression coverage

Recovered updatedRole/reentrantArm/reentrantSent/reentrantPoll failures were reproduced before fixing. Revision checks after fallible context callbacks prevent old readers/results from overwriting or cancelling newer accepted proof state. Accepted deposit-role evidence survives the remainder-return checkpoint.

Tests cover the source role, protected whole-stack plan, transfer/settled-state proof, task cancellation/timeout/menu replacement, strict settings persistence and full Pest-parent pickup/deposit/return/original-Farming composition. The native sender test executes its actual production body with real private proof objects and controlled physical fixtures; it is not a game/server run. Pinned26.2 build and archive checks are separate gates. Preserve tests' original failure evidence; later GREEN does not establish current Hypixel compatibility.

## Still open

Exchange/spray/strategy integration, expanded upstream compatibility and the complete native/live lifecycle matrix remain separate work. In particular, no current trap title/lore/Tab/receipt format is declared accepted merely because these offline fixtures pass. Installed2.0.17 and user settings are unchanged; no release tag/install/main advance.


## Optional original-root acquisition

PestBaitBuying is strict default-zero/empty consent, persisted under pest.bait.buying. A shortage at a uniquely selected No Bait trap may request one bounded acquisition for that trap. Every later trap uses the same original SpendingBudget object; a retry/uncertain result cannot create a new budget or re-open the old order. Existing oversized/protected stacks are not converted into implicit splitting or broader purchase permission.

The native flight port constructs only the already-audited anonymous official item/price clients, on the client thread after actual original-root shortage authorization, never on startup/settings changes. Native BazaarAcquisitionController supplies the existing exact custom-amount/sign/one-extra-confirmation/item/cost/purse/receipt guards. Successful delivered bait is checked again before approaching/opening the trap. Original root/world/Farming hold, native GAME message routing, manual/Jerry/world/timeout cancellation and release/disposal cover this branch. Cached transports remain confined to this one Pest root and close at disposal, even when physical cleanup fails. Unknown/uncertain transactions stop without retry or Farming restart.

The recovered cleanup fixture was extended rather than dropping assertions: a failing bait-child cleanup originally skipped physical release; AutomationStops now attempts all child cancellation and physical release before exposing errors and stopping the original Farm. Bait acquisition cancellation is part of the same actual finish-body regression.

Production UI smoke now finds relocated Pest action controls by name, exercises the real new bait/buy settings callbacks and strict external persistence without offline network/gameplay. Its existing Trade membership fixture was updated to include the previously implemented Pet Sell; the real Pet page/defaults/minimum payout/offline/Jerry checks and native shutdown inspection also include Pet. These fixture updates are not production navigation fixes or server acceptance.


A further native adversarial regression changes physical slot placement/alias rules inside the proof's context callback. The original ordering incorrectly sampled physical permissions before that callback; the reproduced late-placement RED is fixed by claiming/rechecking context first, then sampling the physical slot rules and complete native cursor/inventory immediately before the one-use effect. Late alias/inventory/cursor cases are covered too. This is a real callback-order correction, not removal of a rejection guard.

The final isolated client run passed40 states/screenshots and real native shutdown, including bait/buy settings and the existing Pet/Composter/Visitor pages. Responsive smoke fixtures no longer require truncation/wrapping at every HiDPI width: a fixed narrow palette case and supported large-font Book header exercise these behaviors explicitly. See verification for retained intermediate failures and the still-unresolved historical navigation incidents; no production navigation fix or current-server acceptance is inferred.
