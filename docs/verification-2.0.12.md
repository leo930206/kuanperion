# Kuanperion 2.0.12 verification

2026-10-23 / Minecraft 26.2 / Java 25 / Apple M1.

## Scope
- Pointer ownership follows paint order: content < dropdown/color picker < animated sidebar. Covered content receives no hover; sidebar clicks/wheel cannot reach underlying cards. Passive transition views do not hover.
- Selected Mining targets display one vertically centered line `順位 N: block name`, retaining icons, ordering and removal controls with clipped text before the buttons.
- Remove only the redundant Interface-page Kuanperion/Minecraft InfoSetting. Sidebar version/About navigation is retained.
- Move built-in palettes from Profiles to Colors: collection, six built-ins, editable menu colors. English names, light white-base palettes, miniature swatch previews and wrapping buttons. No seventh default/reset shortcut. No automatic reapplication to existing user files.
- Introduction text remains pending user review; inventory is a vault note based on 2.0.11, including internal/legacy descriptions. Large feature migration is still paused.

## Test-first and automated checks
- New UI tests failed against original compiled classes: missing shared sidebar hit geometry and one-line target label/preset layout helper.
- UI model covers partial/full/collapsed sidebar bounds, edge ownership, six complete white-base resources, English labels, responsive columns and single-line target output.
- Full local logic suite, production build, UI models, legacy migration and color preset storage tests are required before installation.
- Runtime smoke uses an isolated offline/fake-account Minecraft instance. No live-server commands, real Webhook or personal account credentials are used. Production JAR must not contain the smoke helper.

## Release safeguards
- Back up previous tracked source/JAR; compare runtime-tested and rebuilt ZIP payloads.
- Hash own settings/options and unrelated mod JARs before/after installation; never overwrite config files or unrelated mods.
- Retain only the newest own JAR; preserve source notices and bundled licenses.

## Additional regression evidence
- Visual inspection caught renderer-level localization of Sky Blossom despite its English enum label; a failing localization regression was added before removing the legacy label alias.
- A runtime palette-fill regression exposed stale frame click areas after dynamic collection growth. Built-in palette buttons now use the setting interaction controller and current shared row geometry instead of frame-cached click areas; five-slot add/save/load/delete plus all six built-in clicks pass.
- Focused isolated sidebar test passes real collapse → expand → covered-card hover, click and wheel ownership, followed by normal uncovered-card hover. Evidence: `/private/tmp/kuanperion-ui-hover-2.0.12/client.log` and screenshots 31/32.

## Final runtime result
PASS: clean full 32-state isolated run completed with UI_SMOKE_PASS at `/private/tmp/kuanperion-ui-smoke-2.0.12-verified`, including all six actual preset clicks, full palette collection lifecycle, search, resize, keys, module routes and real sidebar hover/click/wheel ownership. The focused regression also completed cleanly. Screenshots 05 (six English palettes between collection/menu), 06 (profiles without built-ins), 08 (interface without duplicate InfoSetting), 12 (single-line target rows), 31/32 (occluded/unoccluded hover) were inspected.
