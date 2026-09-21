# AGENTS.md

## Build And Verification

- Use Java 25 and the Gradle wrapper; the project targets Paper API `26.2.build.126-stable`.
- Full local verification on Windows: `.\gradlew.bat clean test jar jacocoTestReport --console=plain`.
- Focused test example: `.\gradlew.bat test --tests "me.dalibex.UHC_DBasic.managers.SkinRotationTest" --console=plain`.
- The JAR is produced in `build/libs/`; do not commit jars, `build/`, `run/`, or `runs/`.
- GitHub Actions runs on Ubuntu; `gradlew` must be executable or invoked via `bash gradlew`. CI error `./gradlew: Permission denied` means the git exec bit is missing.

## Runtime Requirements

- This is a Bukkit/Paper plugin: main class is `me.dalibex.UHC_DBasic.UHC_DBasic`, declared in `src/main/resources/plugin.yml`.
- TAB and SkinsRestorer are hard dependencies (`depend` in `plugin.yml`); manual server tests need both installed.
- `config.yml` controls TAB setup with `tab.setup-mode: off|once|force`; do not assume TAB config rewrites every startup.

## Architecture Notes

- `UHC_DBasic` is bootstrap only: registers managers, listeners, commands, TAB placeholders, then schedules `GameManager.fullReset()`.
- `GameManager` is the source of truth for phase, participants, eliminated players, chapter timing, startup/cancel/reset flow, and active gamemode.
- `SkinsManager` owns identity/skin rotation and must apply Bukkit mutations on the main thread after async SkinsRestorer lookups.
- `TABManager` owns TAB placeholders and tab/nametag formatting; scoreboard-team interactions can conflict with TAB sorting.
- `TeamManager` owns plugin teams and uses internal team names with `ScoreboardHelper.TEAM_PREFIX` (`h_`). Do not touch non-`h_` scoreboard teams.
- Gamemode shared flow lives in `AbstractUHCGameMode`; `Classic` and `ResourceRush` only own mode-specific victory/objective behavior.

## Code Gotchas

- `AsyncChatEvent` is not main-thread safe; hop with `Bukkit.getScheduler().runTask(...)` before reading scoreboards or plugin state.
- Use `TextUtil.deserialize(...)` for legacy text because `legacySection().deserialize()` parses `§`, not `&`.
- Use `TextUtil.item(...)` for item names/lore to explicitly disable default italic rendering in modern Minecraft.
- Public/protected API names are intentionally English; private fields/local variables may remain Spanish.
- Participant and identity state intentionally use player names, not UUIDs, because TAB placeholders and displayed identities are name-based.
- Tests are pure JUnit 5, no MockBukkit; place tests in the same package when package-private pure helpers need coverage.

## Repo Docs

- `README.md` is public-facing and may lag internal implementation details.
- `README_AI.md` is private working context and ignored by git; read it locally when present, but do not commit it.
- If docs conflict with `build.gradle`, `plugin.yml`, or executable code, trust the executable source.
