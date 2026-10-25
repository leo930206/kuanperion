# Supercraft protocol evidence and limits

## Sources checked 2026-10-25

- [Official 2024-02-22 announcement](https://hypixel.net/threads/february-22-supercraft-update.5603786/), including the [source breakdown screenshot](https://hypixel.net/attachments/1708615393856-png.3293372/) and [selected quantity screenshot](https://hypixel.net/attachments/1708615361838-png.3293371/).
- The former official Wiki URL now redirects to the [staff's 2026-07-21 closure announcement](https://hypixel.net/threads/end-of-the-official-hypixel-wiki-july-2026.6112020/). Search-engine copies of old Wiki text are not evidence of the present server layout.

The historical Supercraft design can consume across inventory, Ender Chests, Backpacks and Sacks. Advanced mode displays source allocation; quantities and output destination are visible. Consequently, possessing enough main-inventory materials alone does **not** authorize a craft or guarantee where the server consumes them. This is a safety inference from the official design, not a claim that the current2026 format was live-tested.

## Implemented read-only preview contract

Synthetic fixtures follow the historical advanced-preview grammar, with different materials and quantities. They do not reproduce an actual2026 menu capture. The parser requires a complete exact recipe-title snapshot, a uniquely identified semantic craft role and raw result ID/name from the official catalog, an exact selected craft/output count, every explicitly authorized ingredient quantity, and allocation entirely from Inventory. It rejects basic-mode/missing sources, Sacks/Backpacks/Ender Chest allocations, unknown costs or instructions, duplicate roles/material aliases, mismatched result IDs, inadequate stock and unexpected destinations. Button numbers and gray ratio annotations do not determine authority; the data ticket is PICKUP only, never shift-maximize.

The parser alone is **not an executable native driver**. A dormant fixed-owner native adapter now independently rechecks the current43-slot baseline, all54 fresh post-command applied server slot receipts (including empty slots), the exact per-craft ingredient grid and real output stack limit, all costs/unknown controls, empty cursor/manual/world/resource guards and one guarded native attempt. It closes only its exact successfully issued container; uncertain sends never replay or close. Catalog lookup starts only under the current Supercraft root and cancels pending requests on disposal. The typed recipe callback is not connected to command dispatch. Current-format validation, the quantity-setting protocol where needed, lifecycle/service/UI and live acceptance remain pending. Unknown source formats or storage outputs stop, never fall back to an old slot or assume success. No automatic lookup, command, click, source-wide access or UI toggle is introduced here.

## Native adapter verification (2026-10-26)

`test-supercraft-native.py` executes the actual native method bodies with explicit fake clients and real leases/menu guards/receipt ledgers, without any network/account/game actions. Pinned26.2 class-linkage/no-client and neutral-grid/currency/raw-ID/stack-limit tests run in `test-native-observations.py`. Synthetic menu fixtures do not prove the current server schema. The adapter has no runtime constructor caller and Supercraft remains unavailable in the UI.
