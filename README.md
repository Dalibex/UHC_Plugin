# ⚔️ UHC ELOUD

![Plugin Version](https://img.shields.io/badge/Plugin_Version-1.5.3--SNAPSHOT-orange)
![Minecraft Version](https://img.shields.io/badge/Minecraft_Version-26.2-gold)
![License](https://img.shields.io/badge/License-MIT-green)
![Platform](https://img.shields.io/badge/Platform-Spigot%20%7C%20Paper-blue)

A lightweight and efficient **UHC (Ultra Hardcore)** plugin. It automatically manages the game phases, the dynamic scoreboard, the victory system, and a visual anonymity system.

Based on **UHC ESPAÑA** created by **ElRichMC**.

---

## 🚀 What's New in Version 1.5.3-SNAPSHOT (Current)

This version polishes the administration experience and makes the identity rotation much more stable on real servers.

* 🎮 **Team Formation Episode:**
  - Choose the episode (1–10) in which teams are formed and the compasses are given out.
  - Configurable from the Admin Panel (*General Rules → Team Formation Episode*) or with the `/setteamepisode <1-10>` command.

* ⚔️ **PVP Episode:**
  - Choose the episode (1–10) in which the gentleman's pact ends and PVP is enabled.
  - Configurable from the Admin Panel (*General Rules → PVP Episode*) or with the `/setpvpepisode <1-10>` command.

* ⌨️ **Tab Completion in Commands:**
  - Autocomplete across all commands: border sizes in `/start`, languages in `/lang`, episodes in, and online players in `/assignteam` and `/abandon`.

* 🛠️ **Reworked Admin Panel:**
  - Dedicated sub-menus and clearer navigation (shulkers, episodes, rules, border, time).
  - Slots are centralized in a single source of truth (*AdminSlots*) to avoid mismatches between the menu and the clicks.

* 🎭 **More Stable Skin Rotation:**
  - Staggered application (1 player every 5 s) to avoid lag spikes at chapter changes.
  - Combat protection: if a player is in the middle of PvP, their rotation is postponed or skipped.
  - Offline players no longer waste their skin: they re-sync when they rejoin.
  - Identities revealed during an episode stay revealed for that episode only; at the next chapter rotation, surviving players are masked again with a fresh identity.

* 🔄 **`/reset` Restores Real Skins:**
  - On reset, every player gets their own skin back (internal cache + SkinsRestorer) instead of keeping the fake ones from the previous chapter.

* 📂 **Extended Modular Architecture:**
  - Skins, TAB, and world logic extracted into their own managers (`SkinsManager`, `TABManager`, `WorldManager`).

* 🔒 **Hardened Team & GUI Security:**
  - Teams are namespaced (`h_` prefix), so the plugin never touches teams from other plugins.
  - A deterministic team selector only runs in the lobby / with custom teams; admin menus validate recognized clicks (before inspecting items), protect against drag, and require the admin permission.

* 🎯 **Robust Game Lifecycle:**
  - New safe phases (initializing → preparing → countdown → running/ending).
  - Frozen roster, stored border size (confirmation never trusts its argument), planned scatter positions, and cancellable tasks: resets and cancellation invalidate pending operations.
  - Rejoins during the start sequence get their planned position and re-enter the game.

* 🧪 **Unit Tests and CI:**
  - JUnit 5 suite (**53 green tests**) covering pure policy logic (skin rotation, version comparison, sidebars, victory evaluation, command parsing, resource integrity).
  - GitHub Actions CI on Java 25 (`test`, `jar`, JaCoCo report, artifacts).

* 📋 **Resource Rush Hardening:**
  - Goals are awarded only on real pickups and verified crafting (not generic clicks); offline participant progress is tracked and an individual winner is supported.

---

## ✨ Main Features

* 🌍 **Multi-language System:** Dynamic support for **Spanish** and **English**. Instant scoreboard and message switching via `/lang`.
* 📊 **Dynamic Scoreboard:** Shows phase, chapter timer, total accumulated time, and teammates' health with real-time health icons.
* 🍎 **Golden Heads:** Craft fallen players' heads with 8 gold ingots. Grants Regeneration II (12 s) and Absorption II (5 min).
* ⚔️ **Hybrid 1.8 Combat:** Configurable from the panel: spam-click enabled, adjusted axe damage and *Sweep Attack* removal.
* 🧭 **Automatic Tracking:** Compass that automatically points to your nearest teammate.
* 🏨 **Lobby System:** Standby mode with `/reset` that prepares the world and the players for the start.
* 💬 **Chat Management:** Team chat by default and global chat via the `!` prefix.
* 📦 **Automatic Supplies:** Scheduled delivery of *Shulker Boxes* on key episodes.
* ⚙️ **Admin Panel (GUI):** Powerful visual interface (`/uhcadmin`) to manage all the rules and settings.

---

## 🛠️ Commands

| Command                  | Description                                        | Permission |
|:-------------------------|:---------------------------------------------------|:-----------|
| `/uhcadmin`              | Opens the main admin panel.                        | `admin`    |
| `/start <size>`          | Starts the game with the chosen team size.         | `admin`    |
| `/tpartes <M>`           | ...DEPRECATED - use `/settime`.                    | `admin`    |
| `/settime <h> <m> <s>`   | Sets the chapter time.                             | `admin`    |
| `/reset`                 | Clears states and prepares the lobby.              | `admin`    |
| `/asignarequipo <P> <C>` | ...DEPRECATED - use `/assignteam`.                 | `admin`    |
| `/assignteam <P> <C>`    | Assigns a player to a specific team.               | `admin`    |
| `/setteamepisode <1-10>` | Sets the team formation episode.                   | `admin`    |
| `/setpvpepisode <1-10>`  | Sets the episode where PVP becomes active.         | `admin`    |
| `/abandon [player]`      | Leaves the game or marks a player as eliminated.   | `user`     |
| `/lang <language>`       | Changes your personal language (`es` / `en`).      | `user`     |
| `/nequipo <name>`        | ...DEPRECATED - use `/team`.                       | `user`     |
| `/team [name]`           | Creates or renames your team.                      | `user`     |

---

## 🚀 Installation and Requirements

* **Requirements:**
  - Java 25
  - [SkinsRestorer](https://skinsrestorer.net/) and [TAB](https://github.com/NEZNAMY/TAB) (**hard dependencies**, required at startup)
* **PLATFORM/API:** Paper / Spigot / Purpur for MC 1.21.11 (Paper recommended)

1. Place `ELOUD_UHC.jar` in the `/plugins` folder.
2. Make sure SkinsRestorer and TAB are installed.
3. Restart the server.
4. Use `/reset` to prepare the world once all players joined (not required but recommended).
5. Use `/uhcadmin` to choose gamemode, teams, and configure all rules before starting.
6. Run `/start [size]` to start the game.
