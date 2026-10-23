# Kuanperion 2.0.2 verification

2026-10-23. Minecraft 26.2 / Java 25 / Fabric Loader 0.19.5 / Apple M1 OpenGL 4.1.

## Approved appearance-only changes

- Light sky-blue accents/icons (#58B9EA), light sky-blue titles/labels/values (#50AFE1), neutral gray explanations. No deep-blue main text. This is the user's explicit pale-text preference: light labels are not represented as meeting WCAG 4.5:1; tests retain a visibility floor and verify neutral explanation contrast. All palette values remain editable.
- Less blue/pink background: default radial coverage 0.48 and strength 0.60; both editable in Colors.
- Uniform 6px title-to-explanation gap, slightly larger than the former group-header gap, shared by settings/groups/module header/cards/search/hover. Editable in Interface Settings (6–12).
- Compact 16px numeric/single-line time fields, smaller inner font and matching drawing/hit testing/caret bounds. Editable field height (14–24). Keybind capture buttons and multiline fields not shrunk.
- A–Z names independent of macOS IME; actual defaults remain Shift+M/F. No user binding reset. Smoke restores the demonstrated chord before screenshots.

## Verification

- `test.sh`: existing 15 regressions plus palette and hotkey policy (17 groups).
- Palette policy: light sky labels rather than deep blue; neutral explanations; exact 2.0.0/2.0.1 previous-default recognition; custom/rainbow/partial file preservation.
- IME regression: physical key 77 with translated name ㄇ displays M; key 70/ㄑ displays F. Actual bindings unchanged.
- UI model: compact field default/bounds/NaN, same group/row description gap and top-aligned title region; existing paired-control, CJK, target visibility, text/animation and geometry checks retained.
- Isolated actual client: all existing smoke checks, old sky default upgrade/backup and scale/custom preservation; compact active editor height; independent duration/toggle; restored default Latin key names;12 screenshot states after fresh frames.
- Standard release gate rebuilds from real repo, compares every JAR entry with tested payload, verifies installed SHA, retains only latest own JAR; other mod/config data untouched.

## Reproduce

```bash
./scripts/test.sh
./scripts/build.sh
./scripts/test-ui.sh
./scripts/test-ui-migration.sh
python3 scripts/ui-smoke/prepare.py
python3 scripts/ui-smoke/launch.py
```

No Hypixel connection, real Webhook delivery, Windows/Linux rendering, whole regular-instance interoperability or exhaustive OS shortcut routing was tested. Main visual check uses text scale 1.0; no exhaustive every-font/every-scale claim. The separate existing 2.0.0 manual sandbox is not automatically updated or closed.
