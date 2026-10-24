# Kuanperion 2.0.14 verification

2026-10-24 / Minecraft 26.2 / Java 25 / Apple M1.

## Scope
- `/kuanperion` is case-insensitive and exact (apart from surrounding whitespace), canceled locally through Fabric ClientSendMessageEvents.ALLOW_COMMAND. Deferred one-shot handling after ChatScreen closes reuses the apostrophe-key UI toggle; no server packet is sent for this command. Other commands are untouched. API and cancellation behavior inspected in the installed 26.2 Fabric message API bytecode.
- `/barn` uses the existing home-command sender with exact modifier, one-press, world/focus/GUI guards and chat feedback. It participates in duplicate checks, Clear/Reset, Minecraft options.txt key persistence and properties modifier persistence.
- Defaults: sethome Shift+S, home Shift+H, barn Shift+B. Existing saved/custom/cleared keys and modifiers are not rewritten; new defaults apply on fresh use or Reset. Gameplay/theme settings and help descriptions remain untouched.
- Prefix endpoints #13A9F2 and #E64478; quadratic interpolation narrows the central white highlight. Gray brackets and nonbold style unchanged.

## Tests
Test-first failures observed for missing local command handler, absent barn dispatch and previous prefix endpoints. Actual extracted production methods are exercised with fake physical key/focus/player/connection boundaries: deferred local command cancellation and case variants, unrelated commands, barn once-per-press, Shift exactness, no repeat when focus/world/GUI returns while held, clear and one chat feedback. Palette checks endpoints, reduced white region and single-character boundary. Existing full regression suite, compile, UI model and legacy migration tests are required. Runtime uses an isolated fake-account client without server connection, real commands, credentials or Discord Webhook. No actual Hypixel command acceptance is claimed.

## Result
PASS: full scripts/test.sh, production build, UI model and legacy migration checks. Isolated client `/private/tmp/kuanperion-ui-smoke-2.0.14` completed all 32 states with UI_SMOKE_PASS. Runtime verified registered Fabric command event cancellation for mixed case, delayed MainGUI opening after simulated chat close and no repeated opening; six Clear/Reset targets; exact new default labels; barn duplicate rejection both directions; custom Alt+J persisted in options.txt; custom/zero home/sethome/barn modifiers survived reload; prefix actual Component endpoint/gray/nonbold styles. Screenshot 07-keybinds visually inspected: all six rows and controls fit without overlaps. Existing descriptions byte-identical; production JAR has correct metadata and excludes smoke classes. No live server or actual Hypixel command execution was tested.
