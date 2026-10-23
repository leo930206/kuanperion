# Kuanperion 2.0.3 verification

2026-10-23 / Minecraft 26.2 / Java 25 / Apple M1.

## Scope

Ordinary toggles and inline notification checkboxes center against the entire card, including explanations but excluding its 5px inter-row gap. Label/description placement, input placement, palette and key defaults are unchanged. Full-card hit regions no longer reject the checkbox lower edge when it extends below the title/control region. Duration editing and toggling remain independent.

## Checks

- TDD red: UI geometry test rejects missing shared full-card positioning helper before implementation.
- UI model: no-help, single-line and wrapped-description row heights; visible checkbox center equals card center.
- Actual isolated client: ordinary no-help and wrapped-help checkbox lower-edge clicks, live explained toggle click and inline checkbox lower-edge click; time input commit/blank duration remains independent. Twelve screenshot states.
- Full 17-group regressions, build, UI model, legacy migration and release-payload/install checks.

No live Hypixel/Webhook/full regular-instance interoperability claim. Manual 2.0.2 window is not modified or closed by this release.
