# Changelog

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
