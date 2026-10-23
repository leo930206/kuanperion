# Kuanperion 2.0.11 verification

2026-10-23 / Minecraft 26.2 / Java 25 / Apple M1.

## Scope
- Sidebar hover does not invoke the generic outline. Module cards keep a fixed position and one hover outline interpolating to sky-blue accent; no floating shadow or second border.
- Empty palette collection renders only Add. Existing files remain visible, no automatic deletion/migration. Cached preview colors follow Theme.ENTRIES order, fixed-size spaced swatches wrap to avoid crowding and reserve two right-side controls.
- Save preserves overwrite confirmation; Delete backs up the snapshot to .deleted.bak and does not apply/change current theme. Deleted entries stay absent on restart; five-entry cap and free-slot reuse retained.
- New /home key starts unbound, supports modifiers, rejects duplicate chords and shares focus/screen/world/one-shot guards without changing /sethome.

## Verification
Test-first UI model failed on empty preset visibility against 2.0.10. Tests cover storage deletion/restart/backup/add cap, preview order and control geometry, independent /home and /sethome dispatch (including held/suppressed keys). Isolated client coverage includes real add/save/load/delete clicks, empty/fill/delete/re-add collection and visible module/sidebar hovers, with no live-server commands or real Webhook requests.

No personal preferences are stored in Git. Release must preserve own config/options hashes and unrelated mod JARs, compare rebuilt payload with runtime-tested JAR, retain only latest own JAR, and preserve attribution/licenses. Large feature migration remains paused.

## Runtime result
PASS: full logic suite, UI model and legacy migration; isolated 32-state client completed with UI_SMOKE_PASS at `/private/tmp/kuanperion-ui-smoke-2.0.11`. Real mouse tests cover add/fill-to-limit/load/confirmed-save/delete/free-slot reuse. Real key capture covers Ctrl+H for /home, duplicate rejection against /sethome and reset to unbound; configuration reload retains home modifiers.

Visually inspected screenshots 05-colors (empty with only Add), 13-color-presets (saved swatches, no names/status, two right controls), 31-sidebar-hover (no outline) and 32-module-hover (one blue border, no lift/double frame). Swatches wrap with fixed 18px size and 8px gaps; preview cache is refreshed only on actions, not disk reads in rendering. Live server commands/Discord have not been exercised; existing private game data remains outside Git.

Release PASS: complete regression suite, UI model, legacy migration and runtime-tested/rebuilt payload equality. Own config/options hashes and unrelated mods unchanged; only latest own JAR retained. Installed SHA-256: `141c73a222a89817c552b254331bdb4e6fab3638c5254641913ca63272e7af95`.
