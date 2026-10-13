# Changelog

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
