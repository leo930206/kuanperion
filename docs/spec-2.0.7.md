# 2.0.7 Navigation specification

- Keep upper avatar and Kuanperion wordmark. Footer K opens About; beside it only two lines: `Mod Version: <actual metadata version>` and `Minecraft 26.2`.
- Four sidebar entries: Farming (Farming/Pest/Visitor), Mining (Mining), Other (Jerry/Webhook), Setting (gear). No feature behavior/config changes.
- Setting contains Colors, Profiles, Keybinds, Interface Settings; preserve all existing color slots, profile operations and searchable settings.
- Feature selection must reset stale detail/search/scroll; search/deep-links select correct group; back remains in that group.
- About is separate local version information, no duplicate settings shortcut. Collapsed/expanded sidebar and clicks agree.
- Version bump, regression/build/runtime checks, README/CHANGELOG, install only latest own jar, commit/tag/push.
