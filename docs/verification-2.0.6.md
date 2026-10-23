# Kuanperion 2.0.6 verification

2026-10-23 / Minecraft26.2 / Java25 / Apple M1.

## Cause and scope

MainGUI already inherits a non-pausing NVGScreen, but both smart-pause wrappers treated any screen as blocking. Merely excluding MainGUI is insufficient for Farming: official installed26.2 bytecode (javap -p -c) shows Minecraft.tick sets missTime=10000 with an open screen and skips handleKeybinds, while Farming only held keyAttack. Installed Minecraft.tick/render paths still refresh pick with a screen open; no custom targeting/range extension is added. A narrow Accessor/Invoker now clears this blocker and invokes vanilla continueAttack once per active Farming tick in MainGUI, preserving vanilla held-block and item-use checks. On own-menu→world transition it clears the leftover blocker once without extra harvesting or ongoing cooldown bypass.

Other GUIs and their500ms closure debounce remain; entering MainGUI from another GUI also honors its closure debounce. The menu does not forward keyboard/mouse events into the world. Right-click selection ignores GUI clicks and carries held-button state so closing a GUI while holding right-click does not select behind it. Mining direct game-mode calls, recovery queue, Jerry safety and ordinary gameplay input routing remain unchanged. Own-menu stall monitoring stays active; other GUI exclusion retained.

## Tests

- TDD red: own-settings guard overload assertions failed before implementation; production settings-attack regression initially rejected missing continuation method.
- GuiPauseGuardTest retains existing boundaries/Bazaar/repeated queries/independence and adds own-menu open/long-open/close and other→own transitions.
- SettingsAutomationTest compiles extracted production continuation against instrumented vanilla-like stubs: GUI10000 miss blocker, one held/release call per tick, no extra closing attack, one-time close cleanup, no normal cooldown override, other GUI/inactive/Mining/recovery/dead/paused/overlay/missing-world exclusions. Added a failing regression for aborting the previously-owned harvest on manual stop/other GUI, then fixed cleanup; a new Mining owner is never aborted. Source contracts keep call at tick end and world-only selection guard.
- FarmingStallIntegrationTest covers continued monitoring in own settings plus prior exclusions/timing/Discord behavior. Full19 regression groups, build, UI model and legacy config tests.
- Actual isolated client verifies non-pausing MainGUI, applied26.2 Mixin/accessor, both real pause wrappers and other-GUI protection; prior13-state UI tests remain.
- Release gate rebuilds real repo, compares every JAR payload with runtime-tested artifact, verifies installed SHA and keeps only latest own JAR. Manual sandbox uses fresh settings/offline dummy account, latest release and helper that opens UI without auto shutdown.

## Boundaries

No connected Hypixel session or real crop/movement physics/harvesting progression was exercised in-world. Continuation behavior is covered by extracted-production tests, checked installed vanilla bytecode and successful runtime Mixin application; not presented as a live farming-session verification. No real Webhook delivery/full80-mod interoperability or Windows/Linux rendering claim. Existing older manual windows are not closed automatically. Navigation reorganization remains discussion-only.
