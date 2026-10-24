# 2.0.17 verification — 2026-10-24

## Scope

Stable 2.0.16 hotfix only; no 2.1.0 automation classes or Trade navigation are imported.

- Former /home shortcut displays and sends /warp garden, with matching local chat feedback. New/reset default is Shift+G; stable persisted key ID and modifier property remain unchanged, preserving user bindings (including cleared keys). Existing saved Shift+H is not silently overwritten; use Reset for the new default.
- With tool detection enabled, startup crop recognition keeps an already-correct hand or selects the first matching tool in hotbar slots 0..8. It never moves items from the main inventory. Only no match triggers the existing tool error/stop. Runtime manual wrong-tool changes still pause rather than fighting the user.
- The tool matcher retains existing crop-tool naming semantics, including reforges and tool tiers.

## Tests

Focused RED was observed before production changes: no startup selector and old /home label. After implementation, the source-extracted production selector passes every supported crop (13 block IDs) at every hotbar position, current-hand preservation, no main-inventory fallback, no-match selection preservation, Pumpkin/Melon distinction, case/reforge names, and unchanged runtime pause wiring. Shortcut dispatch covers exact command/feedback, modifiers, one-shot press and focus/world/menu guards. The first green attempt exposed an old English key translation; it was corrected before final validation.

Final suite/build/UI model/migration and isolated runtime results are recorded below after execution. No live Hypixel test or successful harvest is claimed by the fake-inventory boundary tests. No account login or server connection is performed for the isolated client.

## Installation / rollback

Only this mod's JAR is replaced after validation. Before installation, back up the installed 2.0.16 JAR and hash options/config plus unrelated mod JARs; do not write preferences or stop the running game. The running client requires a full restart to load 2.0.17. Restoring the backed-up 2.0.16 JAR rolls back this hotfix without restoring/overwriting user settings.

## Final result

- Full test.sh, build.sh, test-ui.sh, test-ui-migration.sh: PASS.
- Isolated Minecraft 26.2 full 34-state UI smoke: UI_SMOKE_PASS, including real default/reset/conflict/custom-binding checks and settings persistence. Screenshot 07-keybinds shows /warp garden / Shift+G; screenshot 08-settings shows footer 2.0.17, visually inspected.
- Production JAR metadata 2.0.17; smoke-tested payload will be compared byte-for-byte by ZIP entry before install; no smoke or 2.1.0 automation classes permitted.
- Logs: /private/tmp/kuanperion-2.0.17-{test,build,ui,migration}.log and /private/tmp/kuanperion-ui-smoke-2.0.17/client.log.
