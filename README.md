# Kuan Hypixel SkyBlock Assistant

Client-side Fabric mod for Minecraft `26.2`: Hypixel SkyBlock assistant for mining and farming.

Current mod version: `1.0.8`

## Features

### Mining Assistant

- Scan nearby loaded blocks around the player.
- Select one or more target block types in-game.
- Prioritize selected block types from the settings screen.
- Automatically rotate toward and mine the best matching target.
- Avoid repeatedly targeting the same block position.
- Configurable scan radius and mining delay.

## Keybinds

| Key | Action |
| --- | --- |
| `'` | Open Setting |
| `B` | Toggle block selection mode |
| `M` | Toggle Mining Assistant |

## Selecting mining blocks

1. Press `B` to enter block selection mode. Existing targets are kept.
2. Aim at a block and right-click it to add its block type. If it already exists, it is moved to the newest position instead of duplicated.
3. Repeat for multiple block types.
4. Press `B` again to leave selection mode.
5. Open settings with `'` to reorder priorities.

## Build

This repository uses the Minecraft/Fabric jars already installed by PrismLauncher.

```bash
./scripts/build.sh
```

Output:

```text
build/libs/kuan-hypixel-skyblock-assistant-1.0.8.jar
```

## Maintenance rule

For every update:

1. Bump the mod version in `src/main/resources/fabric.mod.json`.
2. Update this README if behavior, controls, build steps, or setup changed.
3. Add a human-readable entry to `CHANGELOG.md`.
4. Commit and push to GitHub.
