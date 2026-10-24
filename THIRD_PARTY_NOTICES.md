# Third-party notices

## Adapted settings interface

The settings UI, UI models/controllers, theme presets, renderer and associated assets are adapted from **Aether 1.3.1-r6**, supplied as `aether-1.3.1-26.1.2-r6.jar` (SHA-256 `904a32484806b4d19a605d9a21503b9188324a80cafcbbf4f82b8d39c3313868`). The supplied archive includes `LICENSE_aether`, containing GNU GPL version 3. A copy is retained in `LICENSE`. Upstream update repository identified in the supplied code: <https://github.com/mizly/aether>. No claim of endorsement or authorship of upstream material is made.

Modifications by Kuan / Claudian, 2026-10-23: adapt to Minecraft 26.2, repackage to `dev.kuan.kuanperion`, repair decompiler reconstruction, replace the feature registry with local Kuanperion settings, adapt deferred OpenGL framebuffer integration, preserve legacy configuration, and remove upstream account/telemetry/remote-control/gameplay hooks and visible branding. The displayed K monogram is newly created for Kuanperion. Adapted files carry source notices. This combined release is provided under GPL-3.0-only; the corresponding modified source and build scripts are in this repository.

The required notices are retained here and in the distributed JAR. The visible interface is branded Kuanperion, not the upstream client. No original client JAR is executed or packaged as a dependency.

## NanoVG / NanoSVG / LWJGL

Only the five `lwjgl-nanovg` 3.4.1 platform/core JARs from the supplied archive are included under `src/main/resources/META-INF/jars`. These are drawing bindings/native libraries, not the upstream automation or network services. Their embedded license texts remain intact.

- NanoVG: <https://github.com/memononen/nanovg>, zlib license.
- NanoSVG: <https://github.com/memononen/nanosvg>, zlib license.
- LWJGL: <https://www.lwjgl.org>, BSD-style license; bundled distribution license applies.

## Fonts

The bundled Inter OTF files retain their original bytes and embedded font metadata: Copyright 2016 The Inter Project Authors; SIL Open Font License 1.1. The supplied OFL text is retained at `assets/kuanperion/fonts/scoreboard/LICENSE.txt` and copied to `assets/kuanperion/fonts/OFL.txt`. Font resource filenames are inherited from the supplied archive and do not imply that all three contain different font families. System Unicode fallback fonts, if used, are read from the local OS and are not redistributed.

## Other assets

Theme JSON and common interface SVG assets are adapted from the supplied GUI material under its included license unless separately marked. This release does not include the upstream logo, title-screen replacement, capes, gameplay textures, account services, or updater. A new local monogram replaces its interface logo.

Minecraft, Fabric API and Fabric Loader are external runtime dependencies; their licenses and trademarks remain with their respective owners. Kuanperion is not affiliated with or endorsed by Mojang, Microsoft, Hypixel or upstream UI authors.

## Selected automation migration (2.1.0 development)

The approved migration is selective, not an import of the upstream runtime. Stash signal/command behavior in `automation/StashTask.java` is adapted from the supplied Aether 1.3.1-r6 `bootstrap/AetherChatEvents.handleStashState` and `bootstrap/AetherAutomationTickHandler.handleStashPickup`, rewritten with bounded client-tick state transitions, source filtering, context/resource ownership, cancellation, retry/deadline limits and protected external preferences. GPL-3.0-only/source attribution remains. These references do not introduce any Aether service connection; no original client JAR/bootstrap/account/telemetry/worker is run or shipped. Core local safety/storage classes are newly implemented for Kuanperion.

NPC Auto Sell entry/menu behavior is selectively adapted from `modules/inventorymanager/AutoSellManager.java` (Booster Cookie menu) and its daily-limit signal from `bootstrap/AetherChatEvents.java` in the same supplied GPL-3.0-only source. `AutoSellTask` and `AutoSellService` replace the upstream worker/retry logic with bounded client-tick actions, exact ID/UUID authorization, player-inventory slot checks, revision-checked menu tickets, server-only inventory observations and revocable Farming handoffs. No upstream backend, worker, account or network integration is included.
