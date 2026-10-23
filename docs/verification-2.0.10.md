# Kuanperion 2.0.10 verification

2026-10-23, Java 25 / Minecraft 26.2 / macOS Apple M1.

## Scope (13 requests)
1. Slightly smaller responsive panel; recenter on resize/DPI/scale and disable panel dragging.
2. Search placeholder/caret; live query edits do not restart page transitions.
3. Common 12px left inset, regular labels, title/description block positioning and control centering.
4. Duration/beam fields share 66px centered numeric sizing; threshold help only names its own feature.
5. No hover tooltip text overlays; outline/brightness feedback instead.
6. No color/profile subfilters; retained content headings, presets first; Setting order Profiles/Interface/Colors/Keys.
7. Webhook target width 390px with narrow-window fit, requested explanation.
8. Zero disables/removes beams; opt-in Jerry sethome before stop/notification.
9. Once-only Discord origin/arrival message after automatic recovery reaches Garden, handler enabled and webhook present.
10. Four independent Jerry color flags in equal-width cells, no master switch.
11. Card background independent of enabled state; checkbox still reflects actual state.
12. Three-stop prefix gradient and no bold in own chat messages.
13. New initially-unbound chord-capable sethome hotkey, edge-triggered/focus/screen-gated.

## Safety and rollback
New preferences additive and default off/unbound. Existing properties/theme/options/color profiles not reset. No new backend/API integration. Project renamed and remote points only to https://github.com/leo930206/kuanperion.git. Private pre-change source bundle/JAR/settings snapshot stored outside Git under kuanperion-backups (contains secrets, not published).

## Validation plan
Pure/model and production-method fake-boundary tests plus 30-state isolated actual-client test: live Jerry cells, resize small/restore/large, stable search, real numeric/inline hit regions, Webhook width, color/profile/key navigation, zero marker, gradient prefix, preference reload. No real Hypixel command/transaction or real Discord request performed. Do not claim live-server validation from offline smoke.

## Runtime result
PASS: final 30-state isolated Minecraft run at `/private/tmp/kuanperion-ui-smoke-2.0.10-final`, including real resize roundtrips, Jerry cell clicks, both Setting navigation directions, Search transitions and input regions. Screenshots inspected: overview, Jerry, color presets, Webhook, Visitor, empty focused Search caret and delayed hover without tooltip. Final font-metric wrapping removes unused estimated description whitespace; wide fields reserve readable label space on narrow windows. All four Jerry cell labels use the same adaptive font size.

Production-method boundary tests cover auto-sethome only for selected Jerry colors, recovery origin/arrival notice once and suppression without handler/Webhook, and new sethome key unbound default/one press/held press/modifier/focus/GUI/world guards. Marker test covers zero even after wall-clock rollback. Chat prefix test inspects non-bold style and gradient endpoints; new settings reload test preserves existing nondefault preferences. The installed build will be compared entry-by-entry against this runtime-tested JAR; release runs the full suite and verifies own config/options hashes and all unrelated mod JARs.

Release PASS: full regression suite (including new sethome dispatch), UI model, legacy migration and rebuilt JAR payload equality. Own config/options hashes and unrelated mods unchanged; only latest own JAR retained. Installed SHA-256: `b2e17773addd86a77a4d453c5ee06d5a70988f324741c9cbc10658b6fe9c860a`. Private rollback snapshots are outside Git.
