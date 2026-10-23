# Kuanperion 2.0.0 verification

Date: 2026-10-23 (Asia/Taipei). Minecraft 26.2, Java 25, Fabric Loader 0.19.5, Fabric API 0.161.0+26.2, Apple M1 / OpenGL 4.1.

## Passed

- All 15 pre-existing regression groups in `scripts/test.sh`, with identity-only source-contract adjustments. Mining targeting/priority/input lock, Farming startup/tool/pause/stall/recovery, location bridge, notification channels/duration, Jerry detection/marker persistence/rendering were preserved.
- `scripts/test-ui-migration.sh`: missing legacy file, exact-byte copy, no overwrite of new config, original retained, old custom key mapping inherited only when no new binding exists.
- `scripts/test-ui.sh`: live setting callbacks, ranges/non-finite slider rejection, word navigation/caret bounds, Chinese wrapping, reference geometry, stopped-feature preferences, adaptive viewport scale, spring timing.
- Isolated actual 26.2 client: module overview, Farming/Mining/Jerry details, colors, profiles, keybinds, settings, search, close/reopen, Chinese font fallback. Ten screenshots captured outside the release/source tree.
- Actual rendered preference-row mouse click changes the existing backing value even when the feature is stopped; header checkbox reflects actual runtime state.
- Profile save/apply/rejection and synthetic Webhook exclusion. No real Webhook or credentials used by smoke test.
- NanoVG begin/end preserves deliberately changed stencil enable, scissor rectangle and active texture; broader state restoration reviewed in `GlFrameState`.
- Settings shortcut yields to focused text input; Esc leaves search first; settings shortcut then closes. Non-finite saved animation values fall back safely.
- Source allowlist review: GUI, rendering, local settings/profiles only. No upstream feature registry, authentication, telemetry, IRC, remote control, updater, or original client JAR loaded.

## Reproduce

```bash
./scripts/test.sh
./scripts/build.sh
./scripts/test-ui-migration.sh
./scripts/test-ui.sh
python3 scripts/ui-smoke/prepare.py
python3 scripts/ui-smoke/launch.py
```

Smoke tools use the local Prism runtime/libraries, create a separate `/private/tmp/kuanperion-ui-smoke/game`, and launch a visible test window with a fabricated username/UUID/token, no server arguments, and only Kuanperion, Fabric API and test instrumentation. They do not read Prism account credentials. Vanilla itself may make failed authentication/Realms requests; expected 401 errors are not a mod credential flow. `src/smoke` is not included in the production JAR. Script exits with failure if the PASS marker is absent or FAIL appears.

## Explicit boundaries

Not a claim of exhaustive testing or pixel-for-pixel identity on every platform. No Hypixel connection, live farming/mining session, real Discord webhook delivery, every possible text/color/slider gesture, Windows/Linux rendering, or full installed-mod interoperability was exercised in this isolated UI run. Gameplay logic is covered by the existing regression suite, not a fresh live-server certification. Rebuild/retest after future changes; GUI appearance still varies with window size, language and fonts.

Old private native-dialog classes remain for compatibility/source-contract coverage, but the registered settings key opens the new GUI and the new registry does not route to them. The existing repository working-directory/remote address is unchanged; runtime identity is Kuanperion. Legacy identity strings exist only for configuration migration and required provenance.
