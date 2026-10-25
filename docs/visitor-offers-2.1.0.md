# Visitor offer and submission boundaries — 2.1.0 development

This is not yet an executable Visitor automation module. The existing Visitor notification remains independent and unchanged. No offer is opened, clicked, accepted, rejected or bought by these additions.

## Offer observation

- Require the exact already-selected visitor title and unique external `Accept Offer` / `Refuse Offer` roles in a bounded chest shape. No fixed29/33 fallback, substring match or player-item impersonation.
- Bounded requirement/reward grammar, up to8 unique material IDs, strict integer quantities and one bounded Copper amount. Retain other reward text (including Copper Dye), without inventing its value or using substring-based rare-reward overrides.
- Resolve each exact display name only through an explicitly supplied name-to-ID map. The GUI scanner/button ID is never a material ID. Unknown or ambiguous requirements deny the whole offer; this slice adds no remote name lookup/catalog permission.

## Valuation

Explicit visitor/material allowlists, maximum full material value, maximum missing-material cost and minimum Copper default to no authority. Full material value includes already-owned inventory: it is not free and no reward-profit claim is made. Main slots9..35 only; hotbar, UUID/protected items never count as available. Every required item needs fresh, sufficient public Bazaar ask depth with conservative whole-coin rounding. Unknown, stale, shallow or oversized quotes deny the estimate. An estimate is never a GUI cost proof, spending reservation or purchase authorization.

## Inventory-only submission protocol

The caller must first hold the exact Visitor lease, own/revalidate its menu and pass a typed guarded click. The protocol itself acquires no lease and has no native action caller. It re-evaluates explicit rules, requires no missing material and exact `Click to give!` text, freezes all43 native26.2 inventory slots (36main + armor/offhand/body/saddle) together and verifies every menu inventory item against that snapshot. Any same-ID protected/hotbar/equipment/UUID stack vetoes submission entirely because the server, not the client, chooses consumption order.

After at most one guarded attempt, success requires an exact same-world server GAME `OFFER ACCEPTED with <selected visitor> (<known rarity>)`, all required item losses backed by fresh server receipts equal to the current applied inventory, and both physical and fresh server-proved empty cursor. Prediction, disappearance, old receipts, player CHAT, unrelated names and partial loss are not success. Refused/possibly-sent/exception/timeout/manual/world-change outcomes are terminal, never blind retry, inventory repair, cursor repair or automatic GUI close.

## Pending

Queue/entity identity, legal navigation, lease/service/UI, acquisition, return/orchestration and current live server formats remain unimplemented/unverified. Synthetic offer/receipt fixtures and real native item projections are not evidence of server interaction. No original author account, bootstrap, backend, telemetry, remote control or updater is imported.
