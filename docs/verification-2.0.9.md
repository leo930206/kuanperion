# Kuanperion 2.0.9 verification

2026-10-23 / Minecraft 26.2 / Java 25 / Apple M1.

Scope: Setting-to-footer distance reduced 8 logical px; collapsed metadata version and expanded `Mod Version <version>` without colon; centered using actual font width and current animated sidebar width. Short caption remains during transitions until full text fits. About click works in both states. No preference storage changes.

TDD: revised no-colon test failed against 2.0.8. UI tests cover compact/expanded labels, dynamic versions, centering at 60/100/194 widths, gap, click boundaries. Release gates: 19 logic groups, build, UI model, legacy migration and 22-state isolated game smoke with actual NanoVG font-width/centering checks.

No real Hypixel/full modpack/other-OS gameplay validation. Existing official game is not closed. Release only changes own JAR; config checks target own files/options, not unrelated mods' live logs.

PASS: all 19 logic groups, build, UI model, legacy migration and 22-state game smoke. Runtime `UI_SMOKE_CENTERED_FOOTER_PASS` verified compact and expanded text fit and centering; `UI_SMOKE_PASS` completed. Visually inspected `03-mining.png` (collapsed) and `17-setting-sidebar.png` (expanded): centered `2.0.9` / `Mod Version 2.0.9`, closer gear, no lower K/divider/MC version. Evidence: `/private/tmp/kuanperion-ui-smoke-2.0.9/client.log` and its game/screenshots directory.
