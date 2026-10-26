# Supercraft protocol evidence and limits

## Sources checked 2026-10-25

- [Official 2024-02-22 announcement](https://hypixel.net/threads/february-22-supercraft-update.5603786/), including the [source breakdown screenshot](https://hypixel.net/attachments/1708615393856-png.3293372/) and [selected quantity screenshot](https://hypixel.net/attachments/1708615361838-png.3293371/).
- The former official Wiki URL now redirects to the [staff's 2026-07-21 closure announcement](https://hypixel.net/threads/end-of-the-official-hypixel-wiki-july-2026.6112020/). Search-engine copies of old Wiki text are not evidence of the present server layout.

The historical Supercraft design can consume across inventory, Ender Chests, Backpacks and Sacks. Advanced mode displays source allocation; quantities and output destination are visible. Consequently, possessing enough main-inventory materials alone does **not** authorize a craft or guarantee where the server consumes them. This is a safety inference from the official design, not a claim that the current2026 format was live-tested.

## Implemented read-only preview contract

Synthetic fixtures follow the historical advanced-preview grammar, with different materials and quantities. They do not reproduce an actual2026 menu capture. The parser requires a complete exact recipe-title snapshot, a uniquely identified semantic craft role and raw result ID/name from the official catalog, an exact selected craft/output count, every explicitly authorized ingredient quantity, and allocation entirely from Inventory. It rejects basic-mode/missing sources, Sacks/Backpacks/Ender Chest allocations, unknown costs or instructions, duplicate roles/material aliases, mismatched result IDs, inadequate stock and unexpected destinations. Button numbers and gray ratio annotations do not determine authority; the data ticket is PICKUP only, never shift-maximize.

The parser alone is **not an executable native driver**. A fixed-owner native adapter independently rechecks the current43-slot baseline, all54 fresh post-command applied server slot receipts (including empty slots), the exact per-craft ingredient grid and real output stack limit, all costs/unknown controls, empty cursor/manual/world/resource guards and one guarded native attempt. It closes only its exact successfully issued container; uncertain sends never replay or close. Catalog lookup starts only under the current Supercraft root and cancels pending requests on disposal. The typed recipe callback is now connected only through the current Supercraft lease and a fixed `/recipe <exact output ID>` sender. The sender requires client thread, native world/player, no open screen, the current COMMAND capability, and an observed own-command event before local feedback. It cannot accept arbitrary command strings.

The Other page now exposes explicit recipe/consumption caps and a bounded inventory-only one-batch service, off and unconfigured by default. A manual UI request waits for the user to close the screen and expires after30 seconds; opening an unrelated screen during execution cancels instead of replacing it. Automatic execution is opt-in with an interval and Farming-only default. Unknown preview/source/cost/quantity remains refusal. The service does **not** set a quantity, maximize, buy materials, consume storage contents or bypass an unrecognized menu.

Current2026 format validation, the quantity-setting protocol where needed and live acceptance remain pending. A one-batch driver existing is not full Supercraft completion.

## Native adapter verification (2026-10-26)

`test-supercraft-native.py` executes the actual native method bodies with explicit fake clients and real leases/menu guards/receipt ledgers, without any network/account/game actions. Pinned26.2 class-linkage/no-client and neutral-grid/currency/raw-ID/stack-limit tests run in `test-native-observations.py`. Synthetic menu fixtures do not prove the current server schema. The adapter has exactly one fixed-owner constructor route via `AutomationClientBridge`; the pure service and Other UI use it. Actual method-body tests cover the typed sender and native gates, service tests cover success receipts/original-hold release as well as cancellation, and isolated offline UI tests cover real callbacks/external persistence without starting data or gameplay. These are not a successful live craft claim.

## Settings and lifecycle

All recipe, material, protected-ID and quantity limits persist in `config/kuanperion/automation.json`, independent of a JAR update. Missing/malformed policy fields never grant authority; zero defaults permit no output or consumption. Unknown preference fields are retained. An unsuccessful task suspends and disables automatic crafting with no uncertain resend; even a conflicting-file failure while disabling stops dispatch in memory. Pending cancellation also suspends timers, avoiding an immediate automatic rerun of a cancelled click.

Manual look/world input/inventory input/commands, original Farming invalidation, world changes, client shutdown and Jerry stop include the service; world/shutdown dispose pending public data. Jerry also clears queued manual requests and idle automatic timers without rewriting idle preferences. The global-stop regression matrix now covers8 executable services.
