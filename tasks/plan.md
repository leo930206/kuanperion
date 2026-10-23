# Kuanperion 2.0.0 — approved scope (2026-10-23)

Rename the client, metadata, package, assets and artifact; preserve legacy configuration and bindings. Adapt the supplied GPL UI implementation (not the gameplay, remote services or branding) to 26.2. Keep source/license notices outside the visible interface.

Dependency order: rename + migration tests -> NanoVG render/state + mixin adapters -> original GUI components/transitions -> local-only registry bound to existing state -> tests/build -> isolated offline visual test -> release/install/commit/tag/push.

No automation behavior changes, account/OAuth/IRC/telemetry/update subsystem imports, fabricated UI validation, destructive removal of unrelated mods, or secret exports. Only source-approved NanoVG platform resources.

Verification: ./scripts/test.sh; ./scripts/build.sh; dedicated UI/rename tests; git diff --check; JAR entry/source audit; isolated no-account/no-server smoke test if launch permitted. Source and destination SHA checks before install. Old jar removal only after new jar built and verified. Retain backup outside mods.

Risk: 26.2 deferred rendering, GL state/stencil framebuffer, Chinese font fallback, close animation input locking, config migration, profile secret sanitization.
