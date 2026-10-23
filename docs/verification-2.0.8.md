# Kuanperion 2.0.8 verification

2026-10-23 / Minecraft 26.2 / Java 25 / Apple M1.

## Scope and acceptance

- Remove only the lower-left K and divider below Setting. Keep upper avatar/name, divider above Setting, all feature groups and Setting subpages.
- One bottom caption: `Mod Version: <metadata>`, 9 logical px font in a 22 px footer, 2 px from panel bottom. Existing theme values are retained without automatic palette/input-height migration.
- Expanded caption opens existing About; collapsed footer has no invisible old icon hit target. Text/hit geometry share MainGUISidebar constants.

## Checks

Test-first UiModelTest failed on the former two-line footer. Updated tests cover single-line text and dynamic version, bottom placement, Setting separation, full caption hit target and outside/collapsed exclusions. Standard 19 regression groups, build, UI model and legacy migration are release gates. Actual isolated 22-state client smoke retains previous navigation/input checks and asserts the caption fits expanded width, opens About by clicking its text, and is not clickable when hidden.

Deployment backs up previous own JAR/tracked sources, compares rebuilt payload against runtime-tested payload, verifies installed SHA, keeps only latest own JAR and hashes all unrelated mods to ensure no change. User configs and existing manual windows remain untouched. No live Hypixel/full-modpack/Webhook or other-OS validation is claimed.

## Result

PASS: 19 logic groups, build, UI model, legacy-file migration, and all 22 isolated Minecraft UI states. Visually inspected `14-farming-sidebar.png`: upper avatar/name and divider above Setting retained; lower K/divider/MC version removed; `Mod Version: 2.0.8` alone at bottom. Caption click opens About. Initial smoke exposed a hover-animation timing race at the final page; the harness now waits up to 140 ticks for the real animation (still fails if it does not expand), rather than asserting immediately at 35 ticks.

Runtime preference fixtures verify legacy/default/custom theme files are not rewritten on load, saved input height 16 stays 16, explicit preset/reset still works, and nondefault Farming/Mining/notification/modifier values survive save/load. Existing tests cover legacy key overrides (new keys take precedence) and five-slot restart persistence. Main keys remain in Minecraft options.txt; runtime macro on/off is intentionally not restored. Invalid/corrupt files and arbitrary future incompatible/downgrade formats are not promised lossless recovery. Actual release checks hashes of the instance config directory and options.txt without logging contents.

Runtime evidence: `/private/tmp/kuanperion-ui-smoke-2.0.8-final/client.log`, `UI_SMOKE_THEME_PASS`, `UI_SMOKE_PREFERENCES_PASS`, `UI_SMOKE_VERSION_FOOTER_PASS`, `UI_SMOKE_PASS`.

Release note: all build/test/payload/install checks passed, but the broad config-directory equality gate failed because the already-running official Minecraft instance was writing SkyHanni/SkyBlockAPI/other-mod data/logs. The installer had already finished replacing the JAR when this check ran. No game was stopped and no config rollback was attempted. Kuanperion config/options modification times were separately checked against release start; installation code writes only own mod JARs. This is not claimed as a successful whole-directory byte-equality check.
