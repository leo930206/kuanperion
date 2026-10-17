# Kuan Hypixel SkyBlock Assistant

Client-side Fabric mod for Minecraft `26.2`: Hypixel SkyBlock assistant for mining and farming.

Current mod version: `1.0.89`

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

1. Open settings with `'` and click the Mining `選擇目標方塊` button.
2. Enter a target count from 1 to 5; leaving it blank selects one type. Confirming discards the previous target list.
3. Aim at blocks and right-click until the requested number of unique block types is reached. Repeated types are ignored and count only once.
4. The settings list shows each block icon and supports reordering or deletion. Mining picks nearest first; list order only breaks ties.

### Farming Assistant

- The settings screen places Farming above Mining, with centered section headings and a blank-line gap between sections.

- Crop profiles ported from the old `bobscript` Forge mod: Wheat, Potato, Carrot, Nether Wart, Cactus, Sugar Cane, Pumpkim & Melon, Cocoa Bean, Mushroom, Sunflower/Moonflower, and Wild Rose.
- Holds attack, locks the configured yaw/pitch, and presses the profile movement key.
- Row, start, and end detection are permanently enabled and no longer editable in settings. Their hardcoded shared blocks are yellow stained glass, light blue stained glass, and pink stained glass respectively; each crop may still react differently to them.
- Every Farming start reads the block currently under the crosshair without breaking it, switches immediately to the matching crop profile, and saves the detected crop. Both `pumpkin` and the Hypixel farm's `carved_pumpkin` representation are recognized as Pumpkin; Pumpkin and Melon are distinguished for tool validation even though they share one movement profile. Red and brown Mushroom are distinguished to select the initial direction.
- Nether Wart/Carrot share a row-harvesting state machine at yaw `-90` and pitch `5.5`; Wheat/Potato use the mirrored yaw `+90`. Each starts by moving forward one block without attacking. Row changes move one block farther sideways, hold jump for three seconds, then hold jump plus the opposite sideways direction for the fourth second; attack remains held throughout the timed transition.
- For these four row crops, start markers are ignored until an end marker is reached. Attack is then released while movement continues in the current sideways direction for at least one block until the start marker is found, followed by a one-second wait before restarting.
- Sugar Cane farming locks yaw `45` and pitch `0`, harvests left until a row marker, moves forward five blocks and right one block, harvests backward until the next marker, then moves right seven blocks and repeats. Attack remains held throughout normal harvesting and row transitions.
- Sugar Cane checks the end marker only while harvesting backward. Attack remains held throughout every active Sugar Cane state, including backing up to the start marker and the two additional return blocks.
- Cocoa Bean farming locks yaw `0` and pitch `-45`, first moves left one block without attacking, harvests forward until a row marker, moves right one block, harvests backward, then moves right two blocks, forward one block, and left one block before repeating. Attack remains held during the normal cycle after the initial adjustment.
- Cocoa Bean checks the end marker only while harvesting backward. After it triggers, attack is released and the player moves left while ignoring row markers until the start marker is found, continues left for one measured block, then resumes forward harvesting.
- Cactus farming locks yaw `90` and pitch `0`, moves forward one block without attacking, then alternates left/right harvesting lanes. Each row change releases attack, moves one extra block in the current sideways direction, moves forward two blocks, waits one second without moving or attacking, and then resumes attack in the opposite direction.
- Cactus checks the end marker only while harvesting right. It then releases attack, backs up while ignoring row markers until the start marker, moves left one block, moves forward one block, and restarts leftward harvesting.
- Sunflower (including its Moonflower time-based appearance) and Wild Rose share a fixed yaw `-45` / pitch `0` state machine. It backs up one block, harvests right to a row marker, holds forward for one second with left added during the final half-second, harvests backward, then holds left for one second with forward added during the final half-second before repeating. Attack remains held throughout.
- Flower end detection is active only while harvesting backward. After the end marker, it keeps attack and backward held, ignores row markers until the start marker, backs up one additional measured block, and restarts rightward harvesting.
- The former crop-selection button is replaced by `工具偵測`. When enabled, the detected crop must match the held tool name before Farming continues. Supported mappings are Wheat → `Euclid's Wheat Sickle`, Carrot → `Gauss Carrot Shovel`, Potato → `Pythagorean Potato Shovel`, Sugar Cane → `Turing Sugar Cane Cutter`, Cactus → `Cactus Knife`, Cocoa Beans → `Cocoa Chopper`, Nether Wart → `Newton Nether Wart Cutter`, Mushroom → `Fungi Cutter`, Melon → `Melon Dicer`, Pumpkin → `Pumpkin Dicer`, Sunflower/Moonflower → `Eclipse Sickle`, and Wild Rose → `Wild Rose Cutter`. `Mk. I`, `Mk. II`, and `Mk. III` suffixes are accepted because matching uses the base name.
- Pumpkim & Melon uses a fixed west-facing view at yaw `90.0` and pitch `-57.0`, holds attack throughout harvesting and row changes, and follows its dedicated two-marker zigzag state machine. At the first marker it continues one block in its current lateral direction before moving forward to find the second marker. During the end-to-start return run it releases attack, ignores row markers, then moves back one block, left one block, and forward one block before restarting leftward harvesting.
- Every Pumpkim & Melon action described as “one block” is measured by actual player displacement rather than a fixed number of ticks, so increased movement speed does not intentionally lengthen the step. A step only falls back after ten consecutive ticks with no movement, allowing blocked actions to finish.
- The single `異常狀況處理` toggle covers five conditions after Farming was started in Garden: Island, Hub, Lobby, Limbo, and the `You fell into the void` chat message. The first four close Farming, stop inputs, execute their return commands, and finish once Garden is confirmed without `/home` or automatic Farming restart. A void fall only closes Farming. Hub sidebar detection takes priority over the held `Game Menu` Lobby signal.
- Optional `/sethome` on start/turn.
- First Fabric port; per-farm tuning may still be needed for your Garden layout.
- Farming holds the attack key and directly applies movement input every tick. Mushroom farming no longer performs automatic stuck-detection jumps.
- Farming pauses movement/attack while any GUI is open, including inventory, chat, the Esc menu, settings, and the mod settings screen; it continues when the GUI closes without turning Farming off.
- Farming settings include an optional Discord pest notification toggle and local webhook URL field for Hypixel pest chat messages. Saved `Visitor 通知` and `？？？通知` switches are present as behavior placeholders for later design.
- Toggle buttons display 開啟 in green and 關閉 in red while keeping the label white.
- Chat messages use a bold cyan-to-blue `[Kuan HSBA]` gradient prefix and consistent semantic colors. Routine Mining target availability and Farming movement, marker, boundary, direction, and successful tool-check messages are intentionally silent.
- Block names shown in chat omit the technical `minecraft:` namespace while retaining full IDs internally for matching and configuration.

## Build

This repository uses the Minecraft/Fabric jars already installed by PrismLauncher.

```bash
./scripts/build.sh
```

Output:

```text
build/libs/kuan-hypixel-skyblock-assistant-1.0.89.jar
```

## Maintenance rule

For every update:

1. Bump the mod version in `src/main/resources/fabric.mod.json`.
2. Update this README if behavior, controls, build steps, or setup changed.
3. Add a human-readable entry to `CHANGELOG.md`.
4. Commit and push to GitHub.
