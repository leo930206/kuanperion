# Kuan Hypixel SkyBlock Assistant

Client-side Fabric mod for Minecraft `26.2`: Hypixel SkyBlock assistant for mining and farming.

Current mod version: `1.0.67`

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
- Row, start, and end detection settings are shared by every crop; the removed harvest-block detector is no longer shown or enforced. Each crop may still react differently to the same three shared blocks.
- Every Farming start reads the block currently under the crosshair without breaking it, switches immediately to the matching crop profile, and saves the detected crop. Both `pumpkin` and the Hypixel farm's `carved_pumpkin` representation are recognized as Pumpkin; Pumpkin and Melon are distinguished for tool validation even though they share one movement profile. Red and brown Mushroom are distinguished to select the initial direction.
- Nether Wart/Carrot share a row-harvesting state machine at yaw `-90` and pitch `5.5`; Wheat/Potato use the mirrored yaw `+90`. Row changes move one block farther sideways, two blocks forward, then one diagonal forward/sideways block while attacking.
- For these four row crops, start markers are ignored until an end marker is reached. Attack is then released while movement continues in the current sideways direction for at least one block until the start marker is found, followed by a one-second wait before restarting.
- The former crop-selection button is replaced by `工具偵測`. When enabled, the detected crop must match the held tool name before Farming continues. Supported mappings are Wheat → `Euclid's Wheat Sickle`, Carrot → `Gauss Carrot Shovel`, Potato → `Pythagorean Potato Shovel`, Sugar Cane → `Turing Sugar Cane Cutter`, Cactus → `Cactus Knife`, Cocoa Beans → `Cocoa Chopper`, Nether Wart → `Newton Nether Wart Cutter`, Mushroom → `Fungi Cutter`, Melon → `Melon Dicer`, and Pumpkin → `Pumpkin Dicer`. Flower tool names are reserved for future flower crop profiles: Sunflower/Moonflower → `Eclipse Sickle`, Wild Rose → `Wild Rose Cutter`. `Mk. I`, `Mk. II`, and `Mk. III` suffixes are accepted because matching uses the base name.
- Pumpkim & Melon uses a fixed west-facing view at yaw `90.0` and pitch `-57.0`, holds attack throughout harvesting and row changes, and follows its dedicated two-marker zigzag state machine. At the first marker it continues one block in its current lateral direction before moving forward to find the second marker. Every state transition is reported in chat for verification. During the end-to-start return run it releases attack, ignores row markers, then moves back one block, left one block, and forward one block before restarting leftward harvesting.
- Every Pumpkim & Melon action described as “one block” is measured by actual player displacement rather than a fixed number of ticks, so increased movement speed does not intentionally lengthen the step. A step only falls back after ten consecutive ticks with no movement, allowing blocked actions to finish.
- Farming auto-recovery has separate Island/Hub/Lobby/Limbo return toggles. Lobby recovery detects the held `Game Menu` item. After Farming has been confirmed in Garden, matching recovery conditions close Farming, return to Garden, run `/home`, then reopen Farming. Garden void-fall chat detection also closes Farming, runs `/home`, and reopens Farming.
- Optional `/sethome` on start/turn.
- First Fabric port; per-farm tuning may still be needed for your Garden layout.
- Farming holds the attack key and directly applies movement input every tick.
- Farming pauses movement/attack while any GUI is open, including inventory, chat, the Esc menu, settings, and the mod settings screen; it continues when the GUI closes without turning Farming off.
- Farming settings include an optional Discord pest notification toggle and local webhook URL field for Hypixel pest chat messages.
- The right side of the settings screen shows 13 Farming Speed fields with native item icons. Every field defaults to `200`, accepts manual numeric input, and is saved in the local config.
- Toggle buttons display 開啟 in green and 關閉 in red while keeping the label white.
- Chat messages use a bold cyan-to-blue `[Kuan HSBA]` gradient prefix and consistent semantic colors for Farming/Mining features, status, crops, blocks, locations, commands, warnings, and errors.
- Block names shown in chat omit the technical `minecraft:` namespace while retaining full IDs internally for matching and configuration.

## Build

This repository uses the Minecraft/Fabric jars already installed by PrismLauncher.

```bash
./scripts/build.sh
```

Output:

```text
build/libs/kuan-hypixel-skyblock-assistant-1.0.67.jar
```

## Maintenance rule

For every update:

1. Bump the mod version in `src/main/resources/fabric.mod.json`.
2. Update this README if behavior, controls, build steps, or setup changed.
3. Add a human-readable entry to `CHANGELOG.md`.
4. Commit and push to GitHub.
