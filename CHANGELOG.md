# Changelog

## [1.0.87] - 2026-10-17

### Added

- Added native block icons to the left of the Farming row, start, and end block names.

## [1.0.86] - 2026-10-17

### Removed

- Removed Mushroom stuck detection and its automatic 12-tick jump response; Mushroom farming no longer presses jump.

### Fixed

- Moved the Mining target rows below the mining-delay controls so block icons and names no longer overlap the buttons.

## [1.0.85] - 2026-10-17

### Added

- Added saved `Visitor 通知` and `？？？通知` placeholder toggles for future notification behavior.
- Added native block icons beside Mining targets.

### Changed

- Merged Island, Hub, Lobby, Limbo, and void-fall handling under one `異常狀況處理` toggle.
- Island/Hub/Lobby/Limbo recovery now closes Farming and ends after Garden is confirmed, without `/home` or automatically restarting Farming; a void fall only closes Farming.
- Changed the shared Farming marker defaults to yellow stained glass, light blue stained glass, and pink stained glass.
- Mining target selection now replaces the old list, accepts 1–5 requested unique block types, ignores duplicate selections, and ends after the requested count.
- Reordered the Farming and Mining controls and removed the Mining target-list header.

## [1.0.84] - 2026-10-17

### Changed

- Removed the `Auto` word from Mining and Farming open, close, and unavailable messages.
- Removed Mining target waiting/resume notices and Farming movement, row-marker, start/end/return, mushroom-direction, and successful tool-confirmation messages.
- Farming tool mismatch messages now show only the detected crop and required tool, without the currently held item.

## [1.0.83] - 2026-10-17

### Added

- Added automatic Sunflower/Moonflower and Wild Rose farming at fixed yaw `-45` and pitch `0`.
- Added the shared flower right/back harvesting cycle, timed two-second row transitions, and the final-half-second diagonal inputs.
- Added flower end-to-start recovery while keeping attack held, plus `Eclipse Sickle` and `Wild Rose Cutter` validation.

## [1.0.82] - 2026-10-17

### Removed

- Removed the Farming Speed list, item icons, editable fields, and their saved configuration values from the settings screen.

## [1.0.81] - 2026-10-17

### Fixed

- Hub return now prioritizes the sidebar's `Hub` location over the held Game Menu Lobby signal.
- Starting Farming while already in Garden now arms teleport recovery immediately instead of waiting for the next periodic location scan.

## [1.0.80] - 2026-10-16

### Fixed

- Nether Wart, Wheat, Carrot, and Potato row changes now hold the jump key instead of the forward key: jump alone for three seconds, then jump plus the opposite sideways direction for the fourth second.

## [1.0.79] - 2026-10-16

### Changed

- Sugar Cane farming now keeps attack held during every active state, including the end-to-start return and the additional two backward blocks.

## [1.0.78] - 2026-10-16

### Changed

- Replaced the two-second stationary row-change wait for Nether Wart, Wheat, Carrot, and Potato with four seconds of forward input for water movement.
- The first three seconds hold forward only; the fourth second holds forward and the opposite sideways direction simultaneously, with attack held throughout.

## [1.0.77] - 2026-10-16

### Changed

- Nether Wart, Wheat, Carrot, and Potato row changes no longer move forward two blocks after the extra sideways block.
- They now wait two seconds without movement while keeping attack held, then perform the final one-block forward/sideways diagonal movement.

## [1.0.76] - 2026-10-16

### Changed

- Cactus farming now waits one second without moving or attacking after each two-block forward row transition, before starting the opposite harvesting direction.

## [1.0.75] - 2026-10-16

### Changed

- Cocoa Bean farming now continues left for one measured block without attacking after detecting the start marker, then resumes forward harvesting with attack held.

## [1.0.74] - 2026-10-16

### Changed

- Nether Wart, Wheat, Carrot, and Potato farming now move forward one measured block without attacking immediately after startup crop/tool detection, then begin their normal harvest direction.

## [1.0.73] - 2026-10-16

### Changed

- Cocoa Bean farming now moves left one measured block without attacking immediately after startup crop/tool detection, then begins forward harvesting with attack held.

## [1.0.72] - 2026-10-16

### Fixed

- Fixed Farming Speed input text becoming invisible immediately after editing by using opaque ARGB text colors.

## [1.0.71] - 2026-10-16

### Changed

- Changed the fixed Sugar Cane farming yaw from `60` to `45`; pitch remains `0`.

## [1.0.70] - 2026-10-16

### Added

- Added Cactus automatic farming at fixed yaw `90` and pitch `0`.
- Added its initial forward-one step and alternating left/right harvest loop with attack-free one-block sideways and two-block forward row transitions.
- Cactus start markers remain inactive until an end marker is detected while harvesting right.
- After the end marker, Cactus releases attack, backs up while ignoring row markers until the start marker, moves left one block, moves forward one block, and restarts leftward harvesting.

## [1.0.69] - 2026-10-16

### Added

- Added Cocoa Bean automatic farming at fixed yaw `0` and pitch `-45`.
- Added its forward harvest → right one → backward harvest → right two → forward one → left one loop while holding attack.
- Cocoa Bean start markers remain inactive until an end marker is detected during backward harvesting.
- After the end marker, Cocoa Bean releases attack, moves left while ignoring row markers until the start marker, and immediately restarts forward harvesting.

## [1.0.68] - 2026-10-16

### Added

- Added Sugar Cane automatic farming at fixed yaw `60` and pitch `0`.
- Added its left harvest → forward five → right one → backward harvest → right seven loop while holding attack.
- Sugar Cane start markers remain inactive until an end marker is detected during backward harvesting.
- After the end marker, Sugar Cane releases attack, backs up to the start marker, continues backward two measured blocks, and restarts leftward harvesting.

## [1.0.67] - 2026-10-16

### Added

- Added a shared automatic farming state machine for Nether Wart/Carrot using fixed yaw `-90` and pitch `5.5`.
- Added its mirrored state machine for Wheat/Potato using fixed yaw `+90` and pitch `5.5`.
- Added measured sideways, forward, and diagonal row transitions, end-to-start return handling without attack, and a one-second restart wait.
- Start markers for these four crops remain inactive until the end marker has triggered the return phase.

## [1.0.66] - 2026-10-16

### Changed

- Farming now pauses movement and attack whenever any GUI is open, including the Esc menu and settings screens, then resumes when the GUI closes.

## [1.0.65] - 2026-10-16

### Changed

- Pumpkin/Melon now continues backward for two measured blocks after detecting the start block, before moving left and forward to resume the cycle.

## [1.0.64] - 2026-10-16

### Changed

- Converted all 13 Farming Speed reference values into editable numeric fields.
- Every Farming Speed field now defaults to `200` and persists independently in the local configuration.

## [1.0.63] - 2026-10-16

### Changed

- Pumpkin/Melon now moves right by one block without attacking after detecting the end block, then begins moving backward toward the start block.

## [1.0.62] - 2026-10-16

### Fixed

- Pumpkin/Melon one-block movement now replaces the old movement key immediately when a measured step completes, preventing an extra forward/backward movement tick.

## [1.0.61] - 2026-10-16

### Added

- Added a read-only `Farming Speed：` reference list with native item icons on the right side of the settings screen.
- Listed all 13 crops separately; Pumpkin/Melon use `400`, Mushroom uses `233`, and unspecified speeds show `?`.

## [1.0.60] - 2026-10-16

### Fixed

- Changed every Pumpkim & Melon “one block” movement stage from fixed-time movement to an actual `1.0`-block displacement measurement, preventing high Speed from intentionally moving multiple blocks.
- Blocked one-block actions now finish only after ten consecutive ticks without position progress, preserving the earlier blocked-movement fallback without shortening normal slow movement.

## [1.0.59] - 2026-10-16

### Fixed

- Added `minecraft:carved_pumpkin` as a Pumpkin crop representation for automatic crosshair detection.
- Carved Pumpkin now selects the shared Pumpkin/Melon movement profile, reports `Pumpkin`, and validates against `Pumpkin Dicer`.

## [1.0.58] - 2026-10-16

### Changed

- All block names in Kuan HSBA chat messages now hide the `minecraft:` namespace, for example `minecraft:red_mushroom` is displayed as `red_mushroom`.
- Confirmed all module-generated status and detection messages continue to use the standard `[Kuan HSBA]` prefix; long messages may visually wrap onto a second Minecraft chat line.

## [1.0.57] - 2026-10-16

### Changed

- Simplified Farming crop detection to inspect the block directly under the crosshair without attacking or breaking it.
- Pumpkin and Melon now report their exact detected type and use separate tool validation while continuing to share the `Pumpkim & Melon` movement profile.
- Red and Brown Mushroom are identified directly from the aimed block and still select their respective initial Farming directions.

## [1.0.56] - 2026-10-16

### Fixed

- Fixed crop auto-detection doing nothing when the aimed block was not already in the known crop-ID list. Farming now captures any aimed non-air block first and reports its ID.
- Crop probing now combines a continuously held attack key with Minecraft's direct start/continue-destroy actions for one second, ensuring the initial crop is actually hit and broken before identification.

## [1.0.55] - 2026-10-16

### Added

- Added tool validation mappings for Wheat (`Euclid's Wheat Sickle`), Carrot (`Gauss Carrot Shovel`), Potato (`Pythagorean Potato Shovel`), Sugar Cane (`Turing Sugar Cane Cutter`), Cactus (`Cactus Knife`), Cocoa Beans (`Cocoa Chopper`), and Nether Wart (`Newton Nether Wart Cutter`).
- Reserved the supplied future flower mappings in code: Sunflower and Moonflower use `Eclipse Sickle`; Wild Rose uses `Wild Rose Cutter`. These become active when the flower block IDs and movement profiles are added.

### Fixed

- Crop auto-detection now genuinely holds the attack key continuously for at least 20 ticks (one second) without resetting it every tick, then identifies the broken crop.

## [1.0.54] - 2026-10-16

### Added

- Added a saved `工具偵測` toggle in place of the obsolete crop-selection button.
- After automatic crop identification, enabled tool detection now verifies the held item name before Farming continues. Mushroom requires `Fungi Cutter`, Melon requires `Melon Dicer`, and Pumpkin requires `Pumpkin Dicer`; Mk. suffixes and other name decorations do not affect the base-name match.
- Tool mismatches now close Farming and report the detected crop, required tool, and currently held item in the standard `[Kuan HSBA]` chat format.

### Removed

- Removed the manual crop-selection popup because Farming now always detects the crop automatically.

## [1.0.53] - 2026-10-16

### Added

- Farming now attacks the crop under the crosshair on every start, identifies the broken crop block, and automatically switches to the detected crop profile regardless of the previous menu selection.
- Pumpkim & Melon now reports every row-change and return-state action in chat with the standard `[Kuan HSBA]` prefix.

### Changed

- Removed harvest-block detection and its settings controls. Row, start, and end blocks are now one shared configuration used by every crop, while crop-specific reactions remain separate.
- Pumpkim & Melon now moves one additional block in its existing lateral direction immediately after the first row-marker detection, then continues the forward/second-marker sequence.
- Pumpkim & Melon forward marker transitions use a minimum held-input duration and no longer appear idle while waiting to clear the first marker.

## [1.0.52] - 2026-10-16

### Changed

- `Pumpkim & Melon` startup now holds the forward input for 10 ticks (about half a second, approximately one normal walking block) before beginning continuous leftward harvesting, regardless of whether collision allows actual movement.

## [1.0.51] - 2026-10-16

### Changed

- `Pumpkim & Melon` startup now performs the forward input once and immediately begins continuous leftward harvesting; it no longer waits for actual forward displacement when the player is blocked.

## [1.0.50] - 2026-10-16

### Fixed

- Prevented `Pumpkim & Melon` one-block movement stages from getting stuck forever when collision or server position correction prevents reaching the exact distance threshold; each stage now falls back after one second so startup proceeds to continuous leftward harvesting.

## [1.0.49] - 2026-10-15

### Added

- Added a unified semantic chat-message color system: gradient/bold `[Kuan HSBA]`, matching Farming/Mining feature colors, green success, red failure, orange warnings, yellow blocks, aqua locations, gold commands, and yellow-green crop names.
- Mining now refuses to start with an empty target list and reports the reason.
- Mining now reports once when waiting for an available target and once when a target returns.

### Changed

- Renamed every chat prefix from `[Kuan SBA]` to `[Kuan HSBA]`.
- Standardized all Farming and Mining status, selection, detection, recovery, success, warning, and error messages.
- Mushroom startup now reports `Auto Farming 開啟，方向確認：Red Mushroom` or `Brown Mushroom`; all crop names share the same yellow-green style.

## [1.0.48] - 2026-10-15

### Added

- Added the dedicated `Pumpkim & Melon` Farming state machine with fixed yaw `90.0` and pitch `-57.0`.
- Added distance-based one-block forward, backward, left, and right movement steps.
- Added two-stage row-marker handling that requires leaving the first marker before the second marker can trigger.

### Changed

- Merged the separate Pumpkin and Melon crop choices into `Pumpkim & Melon`, using the pumpkin icon and migrating legacy Pumpkin/Melon block settings.
- Pumpkim & Melon now holds attack during all normal harvesting and row-change steps, releases attack only between end-block and start-block detection, and ignores row markers during that return run.
- After reaching the start block, the return sequence is back one block, left one block, forward one block, then continuous leftward harvesting.

## [1.0.47] - 2026-10-15

### Changed

- Removed the `Shift+B` Mining block-selection shortcut and its keybinding entry.
- Mining block selection can now only be started from the settings screen. It selects one right-clicked block and then ends automatically, matching the Farming block-selection flow.

## [1.0.46] - 2026-10-15

### Changed

- Moved the Farming settings section above Mining.
- Centered the Farming and Mining section headings horizontally.
- Added a blank-line gap between settings sections for clearer separation.

## [1.0.45] - 2026-10-15

### Changed

- Replaced the expanding crop list with a centered crop-selection popup arranged in a two-column grid.
- Added native Minecraft item icons to every crop choice.
- Renamed the webhook label to `Discord Webhook`, shortened the input field, and removed its gray suggestion text.

## [1.0.44] - 2026-10-15

### Added

- Added a fourth per-crop Farming block setting: harvest-block detection. Farming pauses when the player is not standing on the configured block and automatically turns off after 3 seconds.
- Mushroom Farming now breaks and confirms one starting mushroom before moving; red uses the original direction and brown uses the post-row-switch direction.

### Changed

- Row, start, end, and harvest detection toggles and block choices are now stored independently for every crop profile. Existing legacy settings migrate to Mushroom.
- Farming block controls now use four equal-width columns without increasing the original total width.

## [1.0.43] - 2026-10-15

### Changed

- Moved the Discord webhook input from the center of the Farming controls to the bottom-left corner of the settings screen.

## [1.0.42] - 2026-10-15

### Changed

- Lobby recovery now triggers from the held `Game Menu` item alone; the `Evacuating to Hub...` chat message is no longer required.
- All recovery flows still require Farming to have been active and confirmed in Garden first.

## [1.0.41] - 2026-10-14

### Added

- Farming now pauses movement and attack while the player inventory or chat screen is open, then resumes automatically after closing the screen.

## [1.0.40] - 2026-10-14

### Added

- Added Garden void-fall recovery: when Farming is active in Garden and chat says `You fell into the void`, Farming closes, runs `/home`, then reopens.

## [1.0.39] - 2026-10-14

### Changed

- Reworked Farming auto-recovery to require a confirmed Garden state before Island/Hub/Lobby/Limbo recovery can trigger.
- Recovery now closes Farming before sending return commands and reopens Farming after returning to Garden and running `/home`.
- Added separate settings toggles for Island, Hub, Lobby, and Limbo return flows.

## [1.0.38] - 2026-10-13

### Added

- Farming auto-recovery now also triggers from the Hypixel chat message `Evacuating to Hub...`.

## [1.0.37] - 2026-10-13

### Fixed

- Prevented stale saved Farming enabled state from triggering auto-recovery immediately after joining a server.
- Improved Farming auto-recovery location detection for Hub/Lobby/Limbo and checks Limbo before generic lobby text.

## [1.0.36] - 2026-10-13

### Changed

- Removed the hard-coded Discord pest webhook URL from code and added an in-game settings field saved to the local config file.

## [1.0.35] - 2026-10-13

### Changed

- Updated all settings toggle buttons to show 開啟/關閉, with green enabled text and red disabled text while keeping labels white.

## [1.0.34] - 2026-10-13

### Added

- Added a Farming crop dropdown selector and a Discord pest notification toggle for Hypixel pest chat messages.

### Changed

- Replaced the previous Farming crop previous/next buttons plus crop label with one crop selector button.

## [1.0.33] - 2026-10-13

### Changed

- Reverted Farming harvesting to simple held attack-key behavior, removing forced vanilla attack reflection used for GUI/background harvesting attempts.

## [1.0.32] - 2026-10-13

### Fixed

- Farming now refreshes Minecraft's native crosshair target before continuing the vanilla held-left-click attack flow, improving harvesting while inventory or other screens are open.

## [1.0.31] - 2026-10-13

### Added

- Added Farming auto-recovery for Hub, Lobby, Limbo, and Island displacement using Hypixel sidebar detection.
- Auto-recovery now stops inputs, runs `/lobby` then `/play sb` from Limbo, returns to Garden, runs `/home`, and resumes Farming.

## [1.0.30] - 2026-10-13

### Removed

- Removed the Mining HUD and all HUD position controls from settings.

## [1.0.29] - 2026-10-13

### Fixed

- Farming now starts the vanilla attack once and then only continues it each tick, matching held left-click behavior instead of repeated clicking.

## [1.0.28] - 2026-10-13

### Fixed

- Farming now uses Minecraft's native left-click attack flow (`startAttack` / `continueAttack`) instead of custom reach-based raycasting, matching manual harvest distance and hit behavior more closely.

## [1.0.27] - 2026-10-13

### Fixed

- Reduced Farming direct block-breaking reach so automated harvesting matches manual left-click reach more closely and avoids hitting farther rows.

## [1.0.26] - 2026-10-13

### Added

- Added separate Farming start and end detection blocks with independent toggles.
- Added one-shot block-pick buttons for row, start, and end Farming detection blocks.
- End detection now backs up until the start block is detected, shifts right once, then resumes normal forward harvesting.

### Changed

- Reworked the Farming settings marker UI into two rows of three controls and removed the long crop help line.

## [1.0.25] - 2026-10-13

### Fixed

- Farming now directly sends block-breaking actions every tick, improving harvesting while inventory, settings, or other GUI screens are open.

## [1.0.24] - 2026-10-13

### Changed

- Farming now directly applies player movement input every tick so harvesting keeps running through focus changes and open GUI/screens as much as the client/server allow.

## [1.0.23] - 2026-10-13

### Changed

- Mushroom now shifts left before reversing from both forward and backward row marker detections.
- Mushroom keeps holding attack while performing the left-shift row change.

## [1.0.22] - 2026-10-13

### Changed

- Replaced separate Red/Brown Mushroom crop profiles with a single Mushroom profile.
- Mushroom now shifts left briefly before reversing when a row marker is detected while moving forward.

## [1.0.21] - 2026-10-13

### Fixed

- Fixed Farming repeatedly re-triggering the same marker before moving away, which caused it to idle at row-switch blocks.

## [1.0.20] - 2026-10-13

### Added

- Added configurable Farming detection blocks from the settings screen.
- Added chat notifications when Farming detects a row-switch marker block.

### Changed

- Farming uses custom detection blocks when configured, otherwise it falls back to crop defaults.

## [1.0.19] - 2026-10-13

### Changed

- Changed automation hotkeys to `Shift+M` for Mining, `Shift+F` for Farming, and `Shift+B` for Mining block selection mode.

## [1.0.18] - 2026-10-13

### Added

- Added the first Farming Assistant port inspired by the old `bobscript` Forge mod.
- Added crop profiles for Wheat, Potato, Carrot, Nether Wart, Cactus, Sugar Cane, Melon, Pumpkin, Cocoa Bean, Red Mushroom, and Brown Mushroom.
- Added Farming settings for crop selection, marker-based row switching, and optional `/sethome`.
- Added `N` as the default Farming toggle key.

### Changed

- Mining and Farming now turn each other off so only one automation mode runs at a time.

## [1.0.17] - 2026-10-13

### Added

- Added a Mining HUD showing recent average blocks mined per second, with position controls in settings.

### Changed

- Mining now always selects the nearest currently visible selected block that is not bedrock; target list order only breaks distance ties.
- Mining locks the selected block position, rotates to its center, and mines only if the crosshair confirms that exact block.
- Failed/unreachable candidates are skipped longer to prevent immediate back-and-forth jitter.
- Mining disables pause-on-lost-focus while enabled and continues running through in-game menus/inventories where the server accepts mining packets.

## [1.0.16] - 2026-10-13

### Fixed

- After a mined target changes to bedrock or another block, that position is treated as depleted and ignored until the original target block respawns.
- Removed the old last-target retry behavior that could cause the miner to keep revisiting depleted bedrock positions.

## [1.0.15] - 2026-10-13

### Fixed

- Bedrock is now hard-blocked from target selection, saved target lists, and mining scans.
- Existing saved `minecraft:bedrock` targets are automatically ignored when config loads.

## [1.0.14] - 2026-10-13

### Fixed

- Targets that cannot be mined after rotating are temporarily skipped, preventing repeated back-and-forth jitter between unreachable blocks.

## [1.0.13] - 2026-10-13

### Changed

- Simplified mining validation: after rotating, the mod only mines if the crosshair is on the selected candidate block and that block type is in the target list.
- Removed extra line-of-sight and edge-center rejection logic that caused valid targets to be skipped.

## [1.0.12] - 2026-10-13

### Fixed

- Fixed Mining idling after line-of-sight checks by aiming slightly inside the visible block face instead of outside it.
- Slightly relaxed the face-center hit margin so valid targets are less likely to be rejected.

## [1.0.11] - 2026-10-13

### Changed

- Split the settings screen into visibly separated Mining and Farming sections.
- Removed the bottom Clear Targets button and centered the Done button.

## [1.0.10] - 2026-10-13

### Changed

- Reworked the Mining settings layout into four rows.
- Added configurable rotation speed controls in the settings screen.

## [1.0.9] - 2026-10-13

### Changed

- Increased smooth rotation speed from `7°/tick` to `13°/tick`.
- Mining now aims at the visible face center of the target block.

### Fixed

- Mining will not start if the crosshair hits the target block too close to an edge, reducing accidental mining of adjacent blocks such as bedrock.

## [1.0.8] - 2026-10-13

### Fixed

- Mining now only selects blocks with a clear line of sight.
- Before mining, the crosshair must be pointing at the exact target block; obstructed targets are skipped.

## [1.0.7] - 2026-10-13

### Changed

- Block selection mode no longer clears existing targets when enabled.
- Re-selecting an existing block moves it to the newest position without creating duplicates.
- Reordered the scan radius controls to show minus, plus, then current radius.

## [1.0.6] - 2026-10-13

### Fixed

- Mining now holds the attack key while working on the active target so block breaking behaves like holding left click.

## [1.0.5] - 2026-10-13

### Fixed

- Mining completion is now detected when the target block changes, including server-side replacement with bedrock.

## [1.0.4] - 2026-10-13

### Changed

- Mining Assistant now freezes player movement while enabled.
- Target rotation is now smoothed over multiple ticks instead of snapping instantly.

## [1.0.3] - 2026-10-13

### Fixed

- Mining now locks onto the current target until the block is mined, changes, or goes out of range instead of jumping between blocks mid-break.

## [1.0.2] - 2026-10-13

### Changed

- Renamed the keybind category to "Kuan Hypixel SkyBlock Assistant".
- Renamed keybind actions to "Select Blocks", "Open Setting", and "Toggle Auto Mining".
- Changed the default settings key from `Right Shift` to `'`.

## [1.0.1] - 2026-10-13

### Changed

- Updated the mod description to: "Hypixel SkyBlock assistant for mining and farming".

## [1.0.0] - 2026-10-13

### Added

- Initial Fabric client mod for Minecraft `26.2`.
- Mining Assistant with nearby block scanning, selectable target block types, target priorities, auto-rotation, and auto-mining.
- In-game settings screen opened with `Right Shift`.
- Block selection mode toggled with `B`.
- Mining toggle with `M`.
- KuanCapoo mod icon.
