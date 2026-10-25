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

Legal navigation, lease/service/UI, acquisition, return/orchestration and current live server formats remain unimplemented/unverified. Synthetic offer/receipt fixtures and real native item projections are not evidence of server interaction. No original author account, bootstrap, backend, telemetry, remote control or updater is imported.

## Queue and local identity observation

Native Tab parsing requires one bounded exact Visitors header/count and a complete unique list of up to5 names. Missing/extra/conflicting entries, duplicate headers/names, unknown rarity suffixes, controls or excessive data produce UNKNOWN, never an empty success. Known rarity and explicit NEW! metadata are read only; no fuzzy name/substring acceptance is added.

Local discovery scans at most4096 loaded entities and projects at most256 relevant facts in64 blocks. It needs the exact uniquely named invisible queued hologram plus exactly one nearby living non-listed character below it, retaining both entity IDs/UUIDs/positions. Duplicate identities, multiple markers/characters or one character matched to two queued visitors deny selection, not choose nearest/first. Listed players cannot impersonate an NPC. No chunks are forced and no entity is interacted with. All facts still need fresh world/session/position revalidation before any later native driver can act. Current live Tab/entity formats remain unverified.
