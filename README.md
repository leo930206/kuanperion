# Kuan Hypixel SkyBlock Assistant

Client-side Fabric mod for Minecraft `26.2`: Hypixel SkyBlock assistant for mining and farming.

Current mod version: `1.0.107`

## Features

### Mining Assistant

- Scan nearby loaded blocks around the player.
- Select one or more target block types in-game.
- Choose targets by configured block-list priority first, then by nearest distance within the same block type; all eligible priority-1 targets in range are mined before priority 2.
- Always rotate toward the locked target block position and mine only if the crosshair confirms that exact block; automatic rotation is permanently enabled and no longer configurable.
- Temporarily skip failed targets so mining does not bounce back and forth between the same blocks.
- The Mining `智慧暫停` toggle pauses target rotation and block breaking while any GUI is open—including inventory, chat, Esc, Bazaar, Auction House, and mod settings—and resumes the same target only after no GUI has been open for one continuous second, without turning Mining off. Any new GUI restarts the buffer, covering short Bazaar search transitions. When disabled, Mining continues through open GUIs. While Mining is enabled, manual forward/back/left/right/jump/sneak/sprint input is suppressed, horizontal movement is frozen, and manual mouse look is blocked while automatic target rotation continues to work.
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
4. The settings list shows each block icon and supports reordering or deletion. Mining exhausts eligible targets in list order first; distance is used only to choose the nearest target within the same priority.

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
- With `工具偵測` enabled, startup still rejects an incorrect tool immediately. After startup, the held tool is checked every tick against the initially detected crop, even while a GUI is open. A wrong tool or empty hand pauses movement, attack, and crop-state progression indefinitely without disabling Farming; switching back resumes the same state. Each correct-to-incorrect transition sends one in-game warning, with no repeated warnings while waiting. Switching back and then away again sends a new warning. Disabling tool detection removes this tool pause; abnormal recovery and GUI smart pause still take precedence over harvesting.
- Pumpkim & Melon uses a fixed west-facing view at yaw `90.0` and pitch `-57.0`, holds attack throughout harvesting and row changes, and follows its dedicated two-marker zigzag state machine. At the first marker it continues one block in its current lateral direction before moving forward to find the second marker. During the end-to-start return run it releases attack, ignores row markers, then moves back one block, left one block, and forward one block before restarting leftward harvesting.
- Every Pumpkim & Melon action described as “one block” is measured by actual player displacement rather than a fixed number of ticks, so increased movement speed does not intentionally lengthen the step. A step only falls back after ten consecutive ticks with no movement, allowing blocked actions to finish.
- The single `異常狀況處理` toggle covers five conditions while Farming is active, with no prior Garden recognition required: Island, Hub, Lobby, Limbo, and the `You fell into the void` chat message. The first four close Farming, stop inputs, and execute their return commands. As soon as Garden is confirmed, recovery finishes at the next client-tick start or before command dispatch, cancels any remaining commands, clears tool/GUI pause state, and releases movement/mouse-look locks without waiting for the command queue or GUI buffer. No `/home` or automatic Farming restart is performed. A void fall only closes Farming. Starting any abnormal return also clears a pending wrong-tool wait and its previous crop-tool requirement, including empty hands in Limbo or a Game Menu in Lobby. The saved `工具偵測` setting is not disabled; the next manual Farming start performs normal tool checks. Sidebar detection reads complete rendered score names, including team prefixes/suffixes and display-name overrides, and matches keywords ignoring color codes, icons, punctuation, spacing, invisible characters, and letter case. `Private Island` alone identifies Island; `Hub` identifies Hub and takes priority over Island and the held `Game Menu` Lobby signal. `Your Island` alone is not used because it may also appear in Hub.
- The four Island/Hub/Lobby/Limbo return triggers now require both their existing location condition and a Fabric `ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE` event recorded while Farming and abnormal handling are enabled. Events count even if the dimension ID stays the same; world load alone or location alone does not trigger return. A pending event can match a later location update, is consumed when recovery starts, and is cleared when Farming starts/stops or abnormal handling is toggled. Loads during recovery do not arm another recovery. The void-message stop does not require a world-load event. Existing Farming movement logic and return actions are otherwise unchanged.
- Every abnormal-return command has the same 60-client-tick wait (about three seconds), including the initial command: Island/Hub → wait 3s → `/warp garden`; Lobby → wait 3s → `/play sb` → wait 3s → `/warp garden`; Limbo → wait 3s → `/lobby` → wait 3s → `/play sb` → wait 3s → `/warp garden`. Arrival confirmation still cancels remaining commands immediately. Each of the four return locations additionally requires a client-world-load event during active Farming; no dimension-ID comparison is used.
- Location detection now prefers the optional SkyBlock API `LocationAPI` island enum used by CustomScoreboard, rather than assuming its generated Island/Hub labels exist in the native scoreboard. API Private Island, Hub, and Garden map directly to their locations; other known islands remain unhandled. Native sidebar and existing Limbo/Lobby checks remain fallbacks if the API is absent, outside SkyBlock, or not ready. No hard dependency is added. Location/source changes are logged once per transition in `logs/latest.log` as `[Kuan HSBA] Farming location=... skyblockApi=... worldChanged=... nativeSidebar=...`, without extra chat notifications.
- Optional `/sethome` on start/turn.
- First Fabric port; per-farm tuning may still be needed for your Garden layout.
- Farming holds the attack key and directly applies movement input every tick. While Farming is enabled, manual forward/back/left/right/jump/sneak/sprint input and manual mouse look are suppressed; only movement and fixed viewing angles requested by the active crop state machine are applied. Mushroom farming no longer performs automatic stuck-detection jumps.
- The `智慧暫停` toggle controls GUI pausing. When enabled, Farming pauses movement/attack while any GUI is open—including inventory, chat, Esc, Bazaar, Auction House, and mod settings—and resumes only after no GUI has been open for one continuous second, without turning Farming off. Any new GUI restarts the buffer, covering short Bazaar search transitions. Movement, attack, and crop-state progression remain paused during the buffer. When disabled, Farming continues through open GUIs.
- `害蟲通知`: turning it on opens a Mining-style number dialog. Only a single integer from `1` through `8` can be confirmed (blank, text, spaces, and multi-digit input are invalid); Cancel/Esc leaves it off. Notify Discord when the server Tab list's `Pests` → `Alive` count is **at least** the saved threshold. The enabled button shows the threshold; turn it off and on to choose another. Missing/invalid saved thresholds default to `1` for existing installations. Spawn-chat messages and sidebar pest counts no longer trigger this notification.
- `Visitor 通知`: notify Discord when the server Tab list's `Visitors: (number)` is **exactly `5`**. Both notifications use the existing local Discord Webhook field (`pestWebhookUrl`) and check cached game data every 20 client ticks (about one second), without holding Tab or enabling Farming/Mining. The server must supply these Tab fields; temporarily missing data is not treated as zero.
- Each notification fires once per qualifying episode, independently. Pest counts must fall below the threshold, or Visitors must leave `5`, before the next qualifying count notifies again; toggling that notification also rearms it. Missing Tab/world data does not rearm or spam notifications. No automatic action is taken on the pests or visitors. Webhook requests are asynchronous with a ten-second timeout; failures/status codes are logged without exposing the webhook token and are not retried during the same episode.
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
build/libs/kuan-hypixel-skyblock-assistant-1.0.107.jar
```

## Tests

Run the dependency-free tool detection, GUI smart-pause, abnormal-handling, and Tab notification/parser/threshold-screen regression tests (requires Python 3 and the PrismLauncher Java runtime):

```bash
./scripts/test.sh
```

## Maintenance rule

For every update:

1. Bump the mod version in `src/main/resources/fabric.mod.json` and `scripts/build.sh`.
2. Update this README and add a human-readable entry to `CHANGELOG.md`.
3. Run tests and build the jar with `./scripts/build.sh`.
4. Install into the Hypixel SkyBlock PrismLauncher instance, retaining only the latest assistant jar (leave other mods untouched).
5. Commit, tag the version, and push the commit and tag to GitHub.
