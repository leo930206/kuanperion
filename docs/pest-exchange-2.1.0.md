# Pest exchange continuation — source-marked Abiphone route

> Current development extension: [separately opted-in Barn physical route](pest-barn-exchange-2.1.0.md). Earlier "Barn unfinished" paragraphs below describe the original Abiphone checkpoint; live exchange acceptance remains open.

## Increment scope / assumptions

The already approved2.1.0 Pest exchange is implemented first via the supplied PestExchangeManager alternative `/call phillip`. Default-off explicit consent means exchanging the captured bag, at most once after the original Pest cleanup/bait pass, not creating a new Farming session. Barn/physical-NPC routing, spray and event strategy are separate unfinished increments. No historical3-call retry or timed-success inference is transplanted.

Static supplied-source facts: menu title contains pesthunter/phillip; role name contains empty vacuum/vacuum bag; `Click to empty` selects a single PICKUP; `exchanged enough Pests` is the source's already-exchanged condition. Local exact title/role/lore grammar, absence-of-fee checks and the fresh server-role receipt combination are provisional current-adapter assumptions pending final batch acceptance. They are not claimed current Hypixel facts. No invented bag NBT/count, fee or new backend is introduced.

## Required vertical path

1. Same PEST lease/coordinator/Farming hold and currently prepared actual Vacuum; independent default-false policy persisted atomically and bound to real Pest UI. No idle/startup command/data client.
2. One fixed typed command opener on the main thread with original root/world/Garden/manual/screen/cursor guards; expected menu registered before send. Unknown opening result never retries.
3. Freeze complete43 native items and exact level/menu identity. Only the original selected nonempty-UUID Vacuum may update metadata while retaining its ID/native/UUID/range; no other inventory mutation is authorized. No cursor use or generic carried-item bypass.
4. A unique nonempty source-role button plus fresh opening applied item/cursor receipts is mandatory. Already-exchanged opening role skips the effect rather than claiming a new exchange. Cost/quantity/unknown role never guesses authority.
5. Private one-use click binds current frame/root/menu/role/full43/cursor and physical external slot permissions immediately before one PICKUP. It may not become a broad command or inventory click API.
6. Completion needs fresh applied same-role transition to source's already-exchanged status and empty cursor, unchanged full43 except the exact owned Vacuum with paired canonical+active receipts if changed. No local prediction/time/menu-close counts as success. Exact owned menu is closed only after settled-state revalidation.
7. Manual/Jerry/world/Farming stop/menu replacement/timeout/callback fault cancels without repair/retry/rearm. Return under original parent only after acknowledged success/known skip, otherwise stop. All cleanup children run even on one failure.

## Layout and commands

Production: automation/pest policy/view/task, PestFlightController native port, AutomationClientBridge fixed opener, AutomationMenuActions private native action, AutomationSettingsStore and PestModuleSettings. Pure/composition tests follow src/test/java/.../automation/pest; actual native body/wiring checks follow scripts. Existing compact Java state/policy style and GPL provenance apply; do not refactor unrelated modules.

Tests: `./scripts/test.sh`; native compile/audit `./scripts/build.sh`; `python3 scripts/test-native-observations.py`; `./scripts/test-ui.sh`; `./scripts/test-ui-migration.sh`; isolated offline UI only via scripts/ui-smoke with a separate game directory. Start with missing API RED, then focused role/task/parent/native/settings negatives, then full gates. Always preserve failure evidence, strict defaults and original stable artifact/settings. Never install/tag/move main or claim current-server success from fixtures.

## Current implementation and proof scope

The task/view, real Pest parent, default-false strict external policy/UI, fixed typed bridge opener and private native click/close gates are now wired. Exchange cannot start an idle root by itself: it is a once-only follow-up to a successfully completed existing cleanup/bait pass. It runs before the original return/restoration, under the exact same lease and Farming hold. Existing failure/manual/Jerry/world/shutdown paths cancel the child; all cleanup children and physical release are attempted even if exchange cancellation fails. No new service/owner or HTTP client is added.

A private action has distinct CLICK/CLOSE kinds. Both must be claimed once by the correct root against a frozen frame; after fallible/reentrant context reads the native edge samples actual level/player/screen/menu, selected slot, bodily/flight state, empty cursor and all43 items again. The click additionally checks the actual non-player slot/role/permissions. Closing requires its own settled-state action, not a bare terminal bit or numeric container ID. An exact local GUI-marker cursor prediction is wait-only; a server-confirmed marker cursor is uncertain, never owned material. Unknown menus, role/cursor receipts or closure results do not retry or repair anything.

The original selected Vacuum may receive server metadata updates only while keeping its exact nonempty UUID/IDs/range; a changed snapshot additionally needs both canonical and active-container item receipts. Non-UUID tools cannot receive such identity relaxation. All other43-slot entries remain unchanged. The source's `exchanged enough Pests` role is treated only as an acknowledgement/known skip: no captured-pest NBT/count or completely empty bag is invented from that string. COMPLETE counts one acknowledged attempt; an already-exchanged opening produces SKIPPED with zero new exchange count.

Focused RED -> GREEN covers view/malformed/cost/duplicate roles, task opening/one-use click/acknowledgement/settled close, parent capture-to-exchange-to-original-return, settings preservation and cleanup failure. Reentrant begin regression first exposed an old failing reader cancelling a newer accepted begin; begin now retains only its own committed revision. The actual typed command-body test also reproduced level/player replacement during the context callback; native identity is frozen before that read and rechecked before dispatch. Intermediate harness missing-sourcepath/Gson/player-method issues and one source-compilation timeout are test-fixture/setup failures, not production behavior REDs; the native fixture no longer drags in an unrelated full PestTask test merely to obtain a tool constant.

Actual private native method-body fixtures use the real task capabilities with controlled physical edges; typed command/outgoing-event fixtures use the actual bridge bodies with a controlled connection. They are not live client/server exchanges. Final full/build/native/UI/migration results belong in verification after completion, not inferred from these focused passes.


## 2026-10-30 canonical hotbar receipt correction

A post-checkpoint audit reproduced a real false-negative against713ac98 production bytecode: a Vacuum metadata update in hotbar inventory index3 is recorded by the actual KuanperionClient callback under window0 raw39 and external-menu raw57, but inventoryProof queried window0 raw3. The previous model also minted that wrong alias and therefore passed without representing the native projection. This is a missing/misbound receipt causing possible timeout, not proof that an ignored packet could execute an extra action.

The query now uses36+tool.slot(); external+27+slot and all other ownership/receipt/cursor/full43/identity/cost/once-only guards are unchanged. All9 hotbar slots cover real pairs, either missing half, and wrong inventory-index aliases. Another test executes the actual observeAutomationSlot / observeAutomationInventorySlot bodies for all9 slots through modern inventory, legacy -2 inventory and active external routes, into the real task/ledger. Pinned26.2 InventoryMenu.isHotbarSlot independently confirms raw36..44 and rejects indices0..8. Controlled callback wrappers are not a claim of applied Mixins or live Hypixel compatibility.

Source/grammar compatibility and live exchange outcome remain open. No original-role/cost guard was weakened, no new command/retry/close path was added, and no formal game/settings were updated.
