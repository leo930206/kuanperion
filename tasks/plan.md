# Kuanperion 2.0.0 — approved scope (2026-10-23)

Rename the client, metadata, package, assets and artifact; preserve legacy configuration and bindings. Adapt the supplied GPL UI implementation (not the gameplay, remote services or branding) to 26.2. Keep source/license notices outside the visible interface.

Dependency order: rename + migration tests -> NanoVG render/state + mixin adapters -> original GUI components/transitions -> local-only registry bound to existing state -> tests/build -> isolated offline visual test -> release/install/commit/tag/push.

No automation behavior changes, account/OAuth/IRC/telemetry/update subsystem imports, fabricated UI validation, destructive removal of unrelated mods, or secret exports. Only source-approved NanoVG platform resources.

Verification: ./scripts/test.sh; ./scripts/build.sh; dedicated UI/rename tests; git diff --check; JAR entry/source audit; isolated no-account/no-server smoke test if launch permitted. Source and destination SHA checks before install. Old jar removal only after new jar built and verified. Retain backup outside mods.

Risk: 26.2 deferred rendering, GL state/stencil framebuffer, Chinese font fallback, close animation input locking, config migration, profile secret sanitization.

## 2.0.1 approved follow-up
White / sky-blue / blue-pink corner gradient; exact old-default-only migration with backup, preserve custom themes. Flexible exact-modifier hotkeys (single key, Ctrl/Alt/Shift/Command combinations), one-shot physical edge and no GUI/background triggers, preserve old bindings. Combine duration/toggle and count/start controls with separate hit regions; blank duration placeholder defaults to 3s. Explain controls inline, reserve CJK line heights. Show only actual selected blocks with runtime resource-pack material icons and live priority/delete controls. No gameplay algorithm changes, remote integrations or credential handling. Validate pure policies/models then real 26.2 isolated screenshots/input; back up, sync, rebuild, install only latest own JAR, commit/tag/push.

## 2.0.2 approved follow-up
Light sky main text/icons (no deep blue), neutral descriptions; more white by reducing editable wash coverage/strength; unchanged default Shift+M/F with Latin labels independent of IME; compact fields; shared configurable description gap and field height. Preserve custom palette/bindings, exact old-default-only migration with backup. No gameplay algorithm changes. Regression, isolated visual/input test, backup/sync/rebuild/payload compare/install/commit/tag/push.
