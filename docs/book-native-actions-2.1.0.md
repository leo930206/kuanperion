# Book native operation boundary (not a public feature)

`BookMenuController` implements the existing real task Port against pinned Minecraft26.2. It requires an immutable explicit policy, a declared anvil layout, the shared inventory/cursor ledgers, the original session/coordinator and a typed `AnvilSender`. There is no default layout, `/av` assumption, arbitrary command string, runtime service/settings/UI or startup action. Actual current menus, opening permission and live success remain unverified.

## Native effects

- Bind the exact Book root, level object, original pose/look/selected tool/flying state and concrete native container object, not only its numeric ID. Client thread, safe player, empty physical cursor and physical manual-input guards precede effects. A native object replacement with identical title/ID is not owned.
- Before the first move, require all90 opening slots, including empty external roles and player aliases, to match post-request applied server receipts. A separate opening cursor stamp crosses the newly assigned container scope but retains world/clear generation, global receipt revision, timestamp and inclusive10-second age. Pre-request and same-timestamp old packets cannot satisfy it. Subsequent actions retain the task's stricter per-attempt cursor sequence stamps.
- Ready/native-send gates repeat full43 inventory, complete exact menu, item/cost/role/protection, current root, cursor and unchanged pose checks. Hotbar-only slot-derived protection is added to packet snapshots for equality; no metadata/count/identity is ignored and no main/external protection is normalized away.
- Move only the original selected first/second books to empty declared inputs, combine only the exact expected preview with explicit zero cost, collect only the proved expected result, reclaim only known originals before any combine. Unknown/priced external controls refuse input. Pending local predictions remain observable to the task but cannot authorize the next click without matching server evidence.
- Every possibly-sent native exception freezes further actions; no replay, cursor repair, arbitrary destination, forced close or guessed recovery. Revalidate ownership/server evidence at owned close; a terminal failure never arms Farming.

## Deliberately remaining destination limitation

The current task contract uses `QUICK_MOVE` for collection/recovery. Vanilla/server quick-move may prefer a hotbar slot. Therefore this adapter currently refuses **before acquiring a task/Farming hold** if any hotbar slot is empty or contains an enchanted book (a possible merge target). All nine hotbar slots must remain occupied by nonbooks. This is a conservative unpublished limitation, not a new user preference or a claim that arbitrary quick-move destinations are safe. General controlled-destination handling/current native menu validation must be completed before exposing Book Combine; do not silently weaken the protected-inventory contract to ship it.

## Verification scope

The real production controller is composed with the real task/session/planner and simulated packet ledgers in a headless fixture. It covers the entire opening/input/zero-cost combine/result/close and original recovery paths, missing receipts, cursor evidence, unknown costs/roles, predicted settling, exact level/container identity, pose/equipment drift, manual/ownership takeover and send uncertainty. Real26.2 ItemStack/Slot fixtures separately verify native projections and packet-vs-hotbar protection. A compiled/linkable native implementation is not a live GUI/server interaction claim. Existing stable2.0.17/user preferences remain unchanged.
