# Kuanperion 2.0.4 verification

2026-10-23 / Minecraft 26.2 / Java 25 / Apple M1.

## Scope

- Single-line text/numeric input default 22 logical px, inner text 12px; full-card vertical centers match checkboxes, slider tracks and paired selection buttons. Title/help spacing unchanged; explanation wrapping reserves the right-hand centered controls column. Configurable height 14–24 remains available.
- Input-height upgrade: schema<3 exact old default16 becomes22 with first backup; other old values and schema3 explicit16 retained. An explicit old16 cannot be distinguished from its old default and also upgrades. Presets loading colors do not touch sizes.
- Colors → 配色收藏: five fixed local quick slots. Click saved row to load, separate save button, second click within5s to overwrite. Slots contain only 25 menu tokens, rainbow selection and gradient controls; no HUD, layout, keys, gameplay or Webhook data. General named theme profiles remain separate.
- Store uses canonical allowlist, 64KiB limit, defensive copies, atomic replacement (same-directory rename fallback where atomic move unsupported), prior-file backup and per-slot damaged-file isolation. No new network or dependencies. Slots read at registry initialization; externally edited files appear on next launch.

## Verification

- TDD red: new full-card/enlarged input geometry tests and five-slot store/action tests failed before corresponding implementation.
- UI model:22/12 defaults, full-card center, invalid/bounded preferences, old-default/custom/new-explicit migration, reserved explanation width, empty/save/load and overwrite expiry/confirmation.
- ColorPresetStoreTest: five independent slots and restart, index0/6 rejection, colors-only/secret exclusion, deep copies, no unconfirmed overwrite, backups, invalid color/non-finite gradient, corrupt/oversized file isolation, explicit repair and temp cleanup.
- Actual isolated client: centered notification duration and numeric threshold editor bounds; previous checkbox hit regressions; real empty/save/load/overwrite/confirmation clicks, tab reachability, five-slot count, layout isolation; input migration backup/persistence/custom preservation. Thirteen screenshot states at the default text scale; no exhaustive every-font/every-scale claim.
- Standard18 regression groups, build, UI model and legacy migration; final repo rebuild payload comparison and installed SHA check.

No live Hypixel, real Webhook delivery, complete regular-instance mod interoperability, or Windows/Linux rendering claim. Existing 2.0.2 manual window is left untouched and must be reopened for new code.
