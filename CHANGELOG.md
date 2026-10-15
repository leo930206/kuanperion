# Changelog

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
