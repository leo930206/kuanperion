# Kuanperion 2.0.1 verification

2026-10-23 (Asia/Taipei). Minecraft 26.2 / Java 25 / Fabric Loader 0.19.5 / Fabric API 0.161.0+26.2 / Apple M1 OpenGL 4.1.

## Scope

User-approved white/sky-blue/blue-pink theme, flexible key chords, paired screen-notification duration/toggle, explanatory setting rows, and real-selected target rows with material icons plus count/start in one row. No new gameplay macro or algorithm, accounts, remote services, telemetry or Webhook destinations.

## Automated verification

- Existing 15 regression groups still pass. New `test-theme.sh` and `test-hotkeys.sh` run from `test.sh` (17 groups total).
- Palette policy: white surface, blue/pink colors, normal-text contrast on white/pale-blue, exact legacy-default detection, one custom color/rainbow/partial file preservation, independent scaling preference.
- Chord policy: all 16 Ctrl/Alt/Shift/Command combinations; exact modifiers, Caps/Num flags ignored; modifier-only capture waits; explicit zero (single key), legacy defaults, invalid persisted masks; one-shot key edges, modifier-release and GUI/background/held-through-close suppression.
- UI model: paired controls have independent callbacks; blank placeholder; count clamped 1–5 without starting; selection action; hidden unselected targets; real explanatory text; full-em Chinese wrap reservation. Existing text/animation/geometry/migration tests retained.

## Actual 26.2 client checks

Isolated dummy-identity client, no server and no real account/Webhook. Existing smoke coverage retained; twelve screenshot states, captured after fresh frames.

- New palette rendered in overview, Farming/Mining/Jerry, colors/profiles/keybinds/settings/search and reopen.
- Exact old default upgrades with byte-preserving backup; custom theme stays custom; non-menu UI scale/animation/HUD preference survives; original dark preset disables new wash; new preset and gradient export/save/load work.
- Real mouse click edits preference while feature stopped.
- Capturing Ctrl alone keeps listening; Ctrl+Shift+F, plain F and Ctrl+Alt+Shift+F capture/display correctly. No forced Shift. Feature Profile excludes partial modifier-only settings.
- Actual duration-field click focuses editor without toggling; commit 2.5; clearing retains blank placeholder with effective 3s; actual checkbox click changes channel without changing duration.
- Zero targets shows no phantom rows. Two selected blocks show two rows; real resource-pack materials for stone/diamond ore load through NanoVG. Priority/delete change existing target list and row visibility.
- Existing GL restoration, profile secret exclusion and Esc/settings shortcut focus priority checks pass.

## Manual/source review

Painting and hit testing share paired-control geometry. Descriptions reserve CJK full-em lines rather than Latin half-width estimates. Icons are runtime **block-material thumbnails**, not bundled Minecraft textures or a full native 3-D item renderer; animated strips display first square frame. Cache bounded to 64 entries and invalidated when model set changes; NanoVG-owned resources cleaned up. No real-instance config edited during testing.

Conflicting identical mod chords are rejected. Defaults remain Shift+M/F but can be changed. Minecraft native control settings edit the base key only; modifier state is kept locally. Opening/selecting in the UI does not implicitly enable macros; selection start requires an actual world. Feature profiles do not apply just the modifier half of a key binding.

## Reproduce

```bash
./scripts/test.sh
./scripts/build.sh
./scripts/test-ui-migration.sh
./scripts/test-ui.sh
python3 scripts/ui-smoke/prepare.py
python3 scripts/ui-smoke/launch.py
```

Smoke is a separate temporary game directory with only the mod, Fabric API and test instrumentation. It does not modify the regular 80-mod instance or user manual-test window. Vanilla failed account/Realms requests with the fake token are expected. `src/smoke` is not shipped.

## Boundaries

No live Hypixel session, actual farming/mining actions or real Discord delivery; no Windows/Linux runtime or full installed-mod interoperability verification. Physical OS keyboard routing/other-mod conflicts are not exhaustively tested; policy and in-client capture are. Main visual verification uses default text scale 1.0; not an exhaustive all-fonts/all-scales guarantee. Gameplay regressions are automated tests, not a new live-server certification. Running old client windows must be restarted to load 2.0.1.
