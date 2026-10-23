# Kuanperion 2.0.5 verification

2026-10-23 / Minecraft 26.2 / Java25 / Apple M1.

## Scope

Upper-left logo now draws the exact bundled PNG referenced by fabric.mod.json. Original colors, nearest filtering and a single renderer-owned texture; native upload buffer freed immediately, texture released at renderer destruction. Existing K fallback only if asset unavailable; lower-left K unchanged. No skin/account/network lookup or changed PNG file.

The 2.0.4 final renderer already reserved left-hand description widths (the earlier supplied screenshot shows the pre-fix layout). This release retains that wrapping and adds a bounded description clip with balanced scissor cleanup, plus Mining selection count/action-column regressions at400/600/1000 logical widths. Inputs and checkboxes remain full-card centered.

## Checks

- TDD red: metadata/avatar resource test failed before avatar renderer constant/implementation.
- UI model: Fabric metadata path equals avatar renderer path, square decodable PNG; three selection-row widths keep description left of numeric/action controls; prior18-group behavior tests and UI preferences/presets checks retained.
- Actual isolated client: bundled PNG upload/draw succeeds and repeated calls reuse positive texture handle; all prior input, migration, palette/profile and five-slot workflows retained; thirteen screenshot states including Mining selected targets.
- Full regressions/build/UI model/legacy migration; release gate rebuilds actual repo, compares each JAR payload entry with runtime-tested JAR, verifies installed hash and removes only old own JAR.

Default text scale visually checked; no exhaustive every-font/scale/platform, live Hypixel, real Webhook or full regular-instance interoperability claim. Existing manual2.0.2 sandbox is not changed or closed.
