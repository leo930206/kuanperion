# Kuan Hypixel SkyBlock Assistant

Client-side Fabric mod for Minecraft `26.2`: Hypixel SkyBlock assistant for mining and farming.

Current mod version: `1.0.52`

## Features

### Mining Assistant

- Scan nearby loaded blocks around the player.
- Select one or more target block types in-game.
- Always choose the nearest currently visible selected block that is not bedrock.
- Rotate toward the locked target block position and mine only if the crosshair confirms that exact block.
- Temporarily skip failed targets so mining does not bounce back and forth between the same blocks.
- Continue mining while game menus/inventories are open and disables pause-on-lost-focus while mining.
- Configurable scan radius, rotation speed, and mining delay.

## Keybinds

| Key | Action |
| --- | --- |
| `'` | Open Setting |
| `Shift+M` | Toggle Mining Assistant |
| `Shift+F` | Toggle Farming Assistant |

## Selecting mining blocks

1. Open settings with `'` and click the Mining `選擇方塊` button.
2. The settings screen closes. Aim at one block and right-click it to add its block type; selection then ends automatically.
3. Repeat from the settings button for additional block types. Existing targets are kept, and selecting a duplicate moves it to the newest position.
4. Reorder the list in settings when needed. Mining picks nearest first; list order only breaks ties.

### Farming Assistant

- The settings screen places Farming above Mining, with centered section headings and a blank-line gap between sections.

- Crop profiles ported from the old `bobscript` Forge mod: Wheat, Potato, Carrot, Nether Wart, Cactus, Sugar Cane, Pumpkim & Melon, Cocoa Bean, and Mushroom.
- Holds attack, locks the configured yaw/pitch, and presses the profile movement key.
- Configurable marker-based row switching: use the Farming settings 「偵測方塊」 button, then right-click a block to add it as a row-switch marker. If no custom marker is set, crop defaults such as quartz blocks, trapdoors, ladders, sea lanterns, dirt, and glowstone are used.
- Farming has independent per-crop row, start, end, and harvest block settings. End detection makes Farming stop harvesting, back up until the start block is found, shift right once, then resume forward harvesting.
- When harvest-block detection is enabled, Farming only operates while standing on that crop profile’s configured harvest block. If the player remains off it for 3 seconds, Farming automatically turns off.
- When Mushroom Farming starts, it first breaks one targeted mushroom to confirm its color. Red starts in the original direction; brown starts in the post-row-switch direction.
- Pumpkim & Melon uses a fixed west-facing view at yaw `90.0` and pitch `-57.0`, holds attack throughout harvesting and row changes, and follows its dedicated two-marker zigzag state machine. During the end-to-start return run it releases attack, ignores row markers, then moves back one block, left one block, and forward one block before restarting leftward harvesting.
- Farming auto-recovery has separate Island/Hub/Lobby/Limbo return toggles. Lobby recovery detects the held `Game Menu` item. After Farming has been confirmed in Garden, matching recovery conditions close Farming, return to Garden, run `/home`, then reopen Farming. Garden void-fall chat detection also closes Farming, runs `/home`, and reopens Farming.
- Optional `/sethome` on start/turn.
- First Fabric port; per-farm tuning may still be needed for your Garden layout.
- Farming holds the attack key and directly applies movement input every tick.
- Farming pauses movement/attack while player inventory or chat is open, then continues when closed without turning Farming off.
- Farming settings use a centered crop-selection popup with native Minecraft item icons, plus an optional Discord pest notification toggle and local webhook URL field for Hypixel pest chat messages.
- Toggle buttons display 開啟 in green and 關閉 in red while keeping the label white.
- Chat messages use a bold cyan-to-blue `[Kuan HSBA]` gradient prefix and consistent semantic colors for Farming/Mining features, status, crops, blocks, locations, commands, warnings, and errors.

## Build

This repository uses the Minecraft/Fabric jars already installed by PrismLauncher.

```bash
./scripts/build.sh
```

Output:

```text
build/libs/kuan-hypixel-skyblock-assistant-1.0.52.jar
```

## Maintenance rule

For every update:

1. Bump the mod version in `src/main/resources/fabric.mod.json`.
2. Update this README if behavior, controls, build steps, or setup changed.
3. Add a human-readable entry to `CHANGELOG.md`.
4. Commit and push to GitHub.
