# Composter inventory-source slice (2.1.0 development)

This is not the complete Composter feature or a live server validation. Sacks and budgeted purchases are not implemented by this slice. New automation remains off with no machine/material permission by default.

## Explicit authority

- Record the actual crosshair machine block in Garden Barn, with a currently loaded matching block ID and exactly one nearby Composter/COMPOSTER hologram. No upstream default coordinates or substring matching. Record only preferences; no click or movement from the capture action.
- Configure exact ordinary material IDs, resource kind and integer units per item. Do not infer conversions from display names or invent catalog values. Materials with UUIDs, protected flags, held/hotbar/equipment slots or oversized stacks are not consumed.
- Select only a full main-inventory stack that fits the remaining configured percentage target and item/stack limits; never split the cursor or overfill. Percentage targets round up rather than falsely treating a zero resource as a tiny nonzero target.

## Execution and evidence

One COMPOSTER lease holds the existing manually armed Farming token. A normal typed Barn trip requires a new same-world absolute server position and matching current pose before reanchoring. Use only server-granted flight, owned normal keys, loaded collision-safe paths and bounded rotation. Recheck configured machine, exact current tool, loaded reach and block clip before one normal native opening action. A menu-only expected contract is registered before opening; a known loading menu waits at most eight seconds without blindly clicking. No other blocker is masked by the owned menu.

A deposit is one protected whole-stack inventory click. Confirmation requires all of:

1. A new server source-slot decrease for the full expected stack, matching the currently applied menu state.
2. A new server resource-slot item matching the current applied role and exactly the configured resource increase, with the same capacity/menu/world/role identity.
3. A new server-applied empty-cursor receipt and an actually empty carried item.

Local source/resource prediction, a packet without the expected delta, or an empty cursor without a fresh acknowledgement cannot prove success. Do not retry a potentially sent action, guess a slot, repair a cursor or close the player's taken-over menu.

Completion or an honest PARTIAL result (insufficient inventory/cap/return reserve) first closes only the positively owned menu with an empty cursor, then confirms the original plot return and exact original position/tool/view/flight state before releasing the same Farming token. Failure, cancellation, timeout, focus loss, Mining, Jerry/manual stop or world change releases owned controls and disables only Composter; no old-view restoration or Farming restart.

## Persistence and validation boundary

External automation.json has strict machine coordinates, material kinds/units and bounded limits. Missing anchor coordinates are invalid, not an implicit position zero. Unknown root and retained material fields remain preserved. Enabling/cancelling Composter must never mutate Greenhouse's feature bit; a real isolated UI regression caught an initial copy error and the pure service regression now covers both enable and cancellation isolation.

The default interval is 7,200 seconds with no startup job replay. Native26.2 fixtures verify real component/lore projection and absent-world refusal; pure task tests verify dual inventory/resource evidence plus cursor acknowledgement and exact partial return. The original isolated UI failure is retained separately from the corrected run. Current server hologram, menu, resource display, click and physics behavior still need live validation before release. Installed stable2.0.17 and real user configuration are not altered by these tests.
