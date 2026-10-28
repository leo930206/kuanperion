# Isolated UI navigation diagnostics

Development only. This diagnoses the retained phase33 Mining-to-Trade route drift and earlier intermittent gear failure. It is not a production UI fix or server acceptance proof.

## Reproduce with call sites

Build the project first, then prepare a **fresh, separate** temporary game directory, never the user's actual instance. Preserve earlier failed directories rather than preparing over them.

```bash
KUANPERION_SMOKE_NAV_TRACE=1 KUANPERION_SMOKE_DIR=/private/tmp/kuanperion-ui-navigation-diagnostic python3 scripts/ui-smoke/prepare.py
KUANPERION_SMOKE_DIR=/private/tmp/kuanperion-ui-navigation-diagnostic python3 scripts/ui-smoke/launch.py
```

Preparing uses existing local pinned dependencies; launching opens a dummy-identity Minecraft26.2 client, no real login or server join. It still needs normal GUI permission. Do not rebuild the same root while a running client or native/model test reads its build/classes. The launcher requires both UI and native-shutdown success markers and process exit0; a navigation failure is not cleared by an unrelated shutdown-only pass.

Only the separate smoke mod optionally activates `UiNavigationTraceMixin`. It logs actual MainGUI mouse click, sidebar selection and navigation setter call stacks plus local page/module identifiers. Direct synthetic `UiSmoke.click` can be distinguished from a vanilla mouse-handler caller. It does not suppress, replay or insert native input, change production navigation, upload logs, inspect credentials or report telemetry. Physical input can still interfere: do not infer external-input causality unless a recorded call site establishes it. No trace for a previous uninstrumented run is invented.

The mixin lives in the dedicated `dev.kuan.kuanperion.smokemixin` namespace. Never use the production UI/entrypoint package as a mixin package: the loader reserves that entire namespace and rejects ordinary classes. The first diagnostic attempt exposed exactly this test-configuration error before the namespace correction. This was not a production initialization regression. `test-ui-smoke-trace.py` executes the actual opt-in metadata block and checks absent/zero/enabled modes, preservation of the normal entrypoint and the dedicated smoke-only package; the old broad-package mutation fails.

The opt-in is off by default and no smoke class/config belongs in the production JAR. The real GUI run and entry-by-entry artifact checks are separate evidence from this metadata test. A later diagnostic pass alone cannot establish a root-cause fix for earlier intermittent failures; retain the open release gates until investigated.

2026-10-29 checkpoint: the corrected instrumented run completed39 states/39 screenshots, both success markers and exit0, with73 fixture clicks and no other native mouse click. Earlier failures were not reproduced; this does not diagnose or fix them. The production JAR remained all554-entry identical to the preceding lifecycle checkpoint.
