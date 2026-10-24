# Kuanperion 2.0.13 verification

2026-10-24 / Minecraft 26.2 / Java 25 / Apple M1.

## Scope and safety
- Gray brackets, stronger sky/white/pink name gradient; no bold styles.
- Independent Farming/Mining runtime mute preferences. SoundEngine calculateVolume gain injection targets the actual 26.2 `(float, SoundSource)` method. Master category refresh on state edges restores current vanilla gains; neither Options volume nor options.txt are set to zero. World exit disables the runtime filter, original mute is preserved.
- Home commands report sent (not server acceptance), sending failures never report success. Jerry success joins the single chat-channel line or reports separately if Jerry chat is off. Discord/screen content remains unchanged.
- Remove Profiles navigation and canonicalize old profile routes to Interface. Existing snapshot files/backend are left alone; current functional/theme/hotkey persistence remains automatic and external to JAR.
- Clear controls share hit/render geometry and save unbound key plus zero modifiers; Reset retains original defaults.
- Independent configurable stall checkbox, positive decimal seconds (default 5). Preserve intentional wait/tool/GUI exclusions. Stop and release inputs before Discord; missing Webhook cannot prevent stop; no auto-restart.
- Existing introduction catalog and vault review note are unchanged. Large migration remains paused.

## Automated regression
Test-first preference model initially failed against the missing new policy/gradient and configurable guard API. Cover all 32 mute combinations and original 0/.25/1 gains; timing boundaries, fractional/custom intervals, duration edits and reset; actual shortcut command sender with fake network/player verifies one feedback per send and failure truthfulness; Jerry detection covers combined versus standalone feedback and disabled colors. Stall integration exercises production method with fake boundary types, 8-second stop+Discord and stop without Webhook. Existing crop/movement, UI-active farming, migration, preset/key/config tests remain required.

Runtime uses a separate fake-account client, no server join, no real Webhook/commands and no private credentials. Production JAR excludes smoke helpers. Release checks own config/options hashes and unrelated mods unchanged, backs up the previous JAR/source and compares rebuilt payload with runtime-tested payload.

## Resumed single-row palette requirement
- Fixed 50 logical px collection rows, vertically centered 18px swatches, responsive visible-count truncation. No second line and no shrink-to-fit crowding. Full snapshot data and Save/Delete controls are unchanged. Zero previews are allowed if no swatch fits before controls.
- UI model checks narrow/wide widths, fixed height, bounded visible count and no control overlap; runtime collection lifecycle checks the single-row preview without truncating stored colors.

## Final result
PASS: full 32-state isolated client at `/private/tmp/kuanperion-ui-smoke-2.0.13-final` completed UI_SMOKE_PASS. Actual 26.2 SoundEngine Mixin is applied; runtime checks independent mute settings and compact centered custom stall field, gray/nonbold saturated prefix styles, all five Clear/Reset click targets, external preference reload (mute/timeout included), removed profile navigation, collection lifecycle with a truncated single-row preview, responsive resizing, existing routes/search/keys and top-layer hover. Screenshots 02/03/07/13 visually inspected. No live server, Discord delivery or human-audible volume test is claimed.
