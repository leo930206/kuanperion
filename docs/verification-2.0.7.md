# Kuanperion 2.0.7 verification

2026-10-23 / Minecraft 26.2 / Java 25 / Apple M1.

## Scope

Four visible sidebar entries: Farming (Farming/Pest/Visitor), Mining (Mining), Other (Jerry/Discord Webhook), Setting (gear). Content-page IDs remain stable for search, profiles and launch targets. Module filter identifies the visible group; selecting a group clears stale detail/search/scroll and commits existing editors. Search/card/deep-link routes choose the owning group; detail-back remains there.

Setting retains four internal pages: Colors, Profiles, Keybinds, Interface Settings. A dedicated shared navigation strip sits above existing color/profile filters; content, hit tests and scroll bounds use the extra strip height. Existing five color slots and local profile operations are retained. No feature behavior or user config reset.

Upper avatar/name unchanged. Footer removes duplicate name and renders `Mod Version: <metadata version>` plus `Minecraft 26.2`. Only the footer K opens a separate About info page; no old About/extra sidebar settings buttons remain. The existing hover-expanded overlay behavior is retained.

## Evidence

- Test-first UiModelTest compilation rejected the missing sidebar navigation model; implemented exact four-entry routes/highlights, About separation and two-line dynamic version tests, then passed.
- All 19 existing regression groups passed; build, UI model tests and byte-preserving legacy config migration passed in staging. Release gate repeats these in the real repository and compares every rebuilt ZIP entry with the runtime-tested production artifact before installation.
- Actual isolated Minecraft client: final 22-state smoke passed, including real MainGUI sidebar hit regions, exact group membership, search jumps across all modules, detail-back, editor/search/scroll clearing, gear highlight across all four settings pages, actual rendered Setting tab clicks, and footer K About. Existing theme migration, avatar cache, keybind capture, inline duration, target icons/reordering, color-slot clicks, config secret exclusion and own-menu automation bridge checks remain.
- All 22 states captured; `14-farming-sidebar.png`, `17-setting-sidebar.png` and `22-about-sidebar.png` visually checked for the new navigation and footer. Upper name is retained; footer has only the two requested lines.
- Harness correction: macOS background windows ignored GLFW cursor warps, so an intermediate run correctly failed its new expansion assertion. Final harness feeds the vanilla MouseHandler.onMove callback using screen/GUI-scaled coordinates (no direct sidebar animation override; no moving the user's cursor); expanded hover reaches >0.95 and both icon/text-column navigation are exercised. Production code did not change for this harness correction.

## Release safety and limits

Only latest own JAR is installed; prior own JAR and tracked source are backed up. Release checks hash every unrelated mod to ensure no change. User configs and existing manual-test windows are not modified or closed. Test helper is sandbox-only, never bundled in the production JAR.

Isolated offline UI/runtime validation only: no Hypixel-connected harvesting session, real Webhook delivery, full modpack interoperability or Windows/Linux rendering claim. Version examples use 2.0.7 because every maintenance update must bump; no hardcoded 2.0.6 footer.
