# Pest Sprayonator — execution foundation, NOT a runtime feature yet

## Status and remaining vertical slice

This checkpoint implements the selected-material tool/observation/consent/plan/task models and a narrow 26.2 native controller. It does **not** instantiate that controller in AutomationClientBridge, persist a spray policy, add a service/toggle, select a material in a GUI, buy stock, or implement continuous refresh. PEST_SPRAY has a stable ID and only HOTBAR/USE resources but implemented() remains false. Nothing here runs in the installed game or starts HTTP at startup. Do not count a model/native-fixture PASS as completed AutoSprayonator functionality or server acceptance.

Next: source-style GUI material selection, original-root stock buying/budget, independent default-off persisted consent + service/UI, proven plot/Tab refresh semantics, all dispatch/busy-root/manual/Jerry/world/shutdown matrices, then batch current-server evidence. No leaf prototype is substituted for those remaining requirements. Main/tag/install/release stay gated.

## Primary source facts, not live-server proof

The supplied Aether AutoSprayonatorManager independently checks its own flag and FARMING, polls Tab after750ms and applies a10s run cooldown. Its spray:/none test is broad; local NONE is a unique scoped Spray field inside Pests, never a missing/unknown value. The source recognizes SPRAYONATOR, JUICY_SPRAYONATOR and SALTY_SPRAYONATOR and Selected Material lore. Static bootstrap12 targets ClientUtils.performAttackClickDirect; inspecting its body shows it queues a key click, not a guessed sneak/use cycle. Bootstrap6 uses performUseClick. Source blind sleeps, fallback substring/any-slot selection, speculative retries and unconditional Farming resume were not copied. The original supplied JAR was statically inspected, never executed or bundled.

Official references checked2026-10-30:
- [Hypixel0.26.1 release](https://hypixel.net/threads/hypixel-skyblock-0-26-1-new-player-improvements-harvest-feast-changes-healing-revamp-and-more.6127383/) describes x3 and x5 upgraded-nozzle material modes. A five-item worst-case reservation is a **provisional local assumption**, not proof of the currently selected nozzle or exact per-plot cost. Confirm the base quantity and every supported mode in the final evidence batch before enabling this path; revise the plan if any base quantity exceeds one. No actual consumption is inferred from a sleep or native return value.
- [Official Wiki closure announcement](https://hypixel.net/threads/end-of-the-official-hypixel-wiki-july-2026.6112020/): the former /Sprayonator URL now redirects to this announcement. It yielded no item-cost or Sacks contract; do not cite it as one.

Sacks fallback/order is not proved by the supplied source. Policy therefore requires separate acknowledgement of possible server-side fallback, as well as at least five visible authorized inventory items. Sacks-only or zero-inventory-delta execution cannot complete; it times out/stops without retry. No Sacks amount is fabricated. These limitations and provisional bounds are another reason this foundation is not exposed yet.

## Implemented safety model

- Exact explicitly configured material ID/native ID/name, and a unique catalog name-to-ID match. Every copy of that ID is checked across all43 slots; hotbar/equipment/protected/UUID/component-variant copies block the action because the server chooses the consumption source.
- Exactly one matching source-ID tool in the hotbar with known matching selection. No implicit material change, inventory movement, buying or Coins authorization.
- One independent PEST_SPRAY root requires the existing manually armed Farming session. Pause its original inputs and allow bounded natural settling; freeze original tool/look, settled position, level/player identity and position-receipt epoch/sequence. Any correction, handover, foreign screen, identity/pose change or expiry stops. No movement/velocity/flight/look writes.
- Private one-use kind-scoped EQUIP/USE/RESTORE actions. The native edge rechecks actual all43/cursor/pose/Tab/thread/identity/current root after the fallible claim and before any effect. It calls ordinary gameMode.useItem(MAIN_HAND), not Minecraft.startUseItem or a blanket manual-world-input exemption. Only the hotbar selection is written/restored.
- Reserve the full provisional five-item ceiling before use, never refund it on an uncertain outcome. Exactly one native use; possible-send exceptions never retry or repair.
- Completion needs post-claim GAME acknowledgement naming the same plot, a new matching ACTIVE Tab observation, and new applied window-0 receipts for every changed authorized inventory stack. A predicted decrement, native return, chat alone or unknown field cannot substitute. Full43 settled verification also applies when restoring the already-selected original slot would otherwise be a no-op.
- Reentrant failed preflight/post-acquire readers cannot cancel a newer accepted generation; revocation during data demand cannot strand ACQUIRING. Owned lookup disposal releases only its exact root and does not leak transport ownership on a close error. No method writes Farming=true.

## Evidence boundaries

TDD logs include missing model/task/native APIs; a zero-length ACTIVE timer regression; old failed post-acquire observation cancelling a new root; preflight/post-acquire armed flags without an actually acquired original Farming hold; offthread catalog demand observing native context; and late game-mode replacement. The latter three were reproduced then fixed. Initial native-fixture cursor mutation targeted an unused alias rather than containerMenu.carried; this was a harness setup error, corrected without weakening the production cursor check. Frozen final gate results are appended to verification-2.1.0.md only after process exit0.

No new real GUI run is claimed: no spray UI exists. Existing UI and migration regression gates must still pass. Current server title/lore/message grammar, nozzle/consumption semantics, Sacks behavior, refresh cadence and live evidence remain unaccepted. Overall upgrade estimate stays roughly70%, not a release/test pass percentage.
