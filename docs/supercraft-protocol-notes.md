# Supercraft protocol evidence and limits

## Sources checked 2026-10-25

- [Official 2024-02-22 announcement](https://hypixel.net/threads/february-22-supercraft-update.5603786/), including the [source breakdown screenshot](https://hypixel.net/attachments/1708615393856-png.3293372/) and [selected quantity screenshot](https://hypixel.net/attachments/1708615361838-png.3293371/).
- The former official Wiki URL now redirects to the [staff's 2026-07-21 closure announcement](https://hypixel.net/threads/end-of-the-official-hypixel-wiki-july-2026.6112020/). Search-engine copies of old Wiki text are not evidence of the present server layout.

The historical Supercraft design can consume across inventory, Ender Chests, Backpacks and Sacks. Advanced mode displays source allocation; quantities and output destination are visible. Consequently, possessing enough main-inventory materials alone does **not** authorize a craft or guarantee where the server consumes them. This is a safety inference from the official design, not a claim that the current2026 format was live-tested.

## Implemented read-only preview contract

Synthetic fixtures follow the historical advanced-preview grammar, with different materials and quantities. They do not reproduce an actual2026 menu capture. The parser requires a complete exact recipe-title snapshot, a uniquely identified semantic craft role and raw result ID/name from the official catalog, an exact selected craft/output count, every explicitly authorized ingredient quantity, and allocation entirely from Inventory. It rejects basic-mode/missing sources, Sacks/Backpacks/Ender Chest allocations, unknown costs or instructions, duplicate roles/material aliases, mismatched result IDs, inadequate stock and unexpected destinations. Button numbers and gray ratio annotations do not determine authority; the data ticket is PICKUP only, never shift-maximize.

This is **not an executable native driver**. Any future caller still needs current-format validation, fresh applied server menu observations, complete43-slot inventory/ownership/cursor revalidation, actual quantity-setting protocol where needed, a guarded native send and the existing single-attempt ingredient/output/cursor receipt proof. Unknown source formats or storage outputs stop, never fall back to an old slot or assume success. No automatic lookup, command, click, source-wide access or UI toggle is introduced here.
