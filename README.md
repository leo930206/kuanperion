# Kuan Hypixel SkyBlock Assistant

Client-side Fabric mod for Minecraft `26.2`: Hypixel SkyBlock assistant for mining and farming.

Current mod version: `1.0.29`

## Features

### Mining Assistant

- Scan nearby loaded blocks around the player.
- Select one or more target block types in-game.
- Always choose the nearest currently visible selected block that is not bedrock.
- Rotate toward the locked target block position and mine only if the crosshair confirms that exact block.
- Temporarily skip failed targets so mining does not bounce back and forth between the same blocks.
- Continue mining while game menus/inventories are open and disables pause-on-lost-focus while mining.
- Configurable scan radius, rotation speed, mining delay, and HUD position.
- HUD shows recent average blocks mined per second.

## Keybinds

| Key | Action |
| --- | --- |
| `'` | Open Setting |
| `Shift+B` | Toggle Mining block selection mode |
| `Shift+M` | Toggle Mining Assistant |
| `Shift+F` | Toggle Farming Assistant |

## Selecting mining blocks

1. Press `Shift+B` to enter block selection mode. Existing targets are kept.
2. Aim at a block and right-click it to add its block type. If it already exists, it is moved to the newest position instead of duplicated.
3. Repeat for multiple block types.
4. Press `Shift+B` again to leave selection mode.
5. Open settings with `'` to reorder the list. Mining picks nearest first; list order only breaks ties.

### Farming Assistant

- Crop profiles ported from the old `bobscript` Forge mod: Wheat, Potato, Carrot, Nether Wart, Cactus, Sugar Cane, Melon, Pumpkin, Cocoa Bean, and Mushroom.
- Holds attack, locks the configured yaw/pitch, and presses the profile movement key.
- Configurable marker-based row switching: use the Farming settings 「偵測方塊」 button, then right-click a block to add it as a row-switch marker. If no custom marker is set, crop defaults such as quartz blocks, trapdoors, ladders, sea lanterns, dirt, and glowstone are used.
- Farming supports separate row, start, and end detection blocks. End detection makes Farming stop harvesting, back up until the start block is found, shift right once, then resume forward harvesting.
- Optional `/sethome` on start/turn.
- First Fabric port; per-farm tuning may still be needed for your Garden layout.
- Farming directly applies player input and block-breaking actions every tick so it keeps trying to run while unfocused or while GUI/screens are open.

## Build

This repository uses the Minecraft/Fabric jars already installed by PrismLauncher.

```bash
./scripts/build.sh
```

Output:

```text
build/libs/kuan-hypixel-skyblock-assistant-1.0.29.jar
```

## Maintenance rule

For every update:

1. Bump the mod version in `src/main/resources/fabric.mod.json`.
2. Update this README if behavior, controls, build steps, or setup changed.
3. Add a human-readable entry to `CHANGELOG.md`.
4. Commit and push to GitHub.
