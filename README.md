# SomAI (NeoForge 1.21.1)

SomAI adds a humanoid companion entity controlled through a single item (the whistle), with automation delegated to **PlayerEngine**.

## Highlights
- Companion entity: ownership/taming, follow with an idle band, wait/follow toggle, vanilla combat goals.
- Companion inventory: 27-slot chest-like storage with persistence.
- Whistle is the single control surface: link/summon/recall + automation controls (no dismiss).
- Automation: uses PlayerEngine Automaton/Baritone APIs + command manager (no custom task framework).
- UI: Whistle Control screen + live status HUD (client) with server-authoritative actions.

## Dev notes
- Refresh deps: `./gradlew --refresh-dependencies`
- Clean build outputs: `./gradlew clean`
- PlayerEngine API dump (dev tooling): `./gradlew dumpPlayerEngineApi` (see [libs/PLAYERENGINE_API_DUMP.md](libs/PLAYERENGINE_API_DUMP.md))

## Version history

### 1.3.1 (2026-01-11)
- Made companion fully persistent: removed all “dismiss/discard” paths.
- Removed Shift-click whistle shortcuts; all actions now go through the UI.
- Improved Whistle Control UX: bigger screen, no overlapping widgets, fewer presets per page.
- Added UI-only summon flow (open UI anywhere → Summon) + “Reset link” for stale UUID links.

### 1.3.0 (2026-01-10)
- Added Whistle Control UI (buttons + command textbox) and live status HUD.
- Added client↔server payloads for actions, command execution, and status sync.
- Added centralized PlayerEngine command registry with categories, paging, and tooltips.

### 1.2.0
- Made PlayerEngine a hard dependency and introduced a direct bridge adapter for automation.
- Added initial whistle automation control surface delegating to PlayerEngine (later replaced by UI-only controls).

### 1.1.0
- Added humanoid companion entity (ownership/taming), follow/idle band, wait/follow toggle.
- Added 27-slot companion inventory UI + persistence.
- Added whistle link/summon/recall baseline behavior.

### 1.0.0
- Initial project scaffold.

## References
- NeoForge docs: https://docs.neoforged.net/
- NeoForged Discord: https://discord.neoforged.net/
- Mojang mappings license: https://github.com/NeoForged/NeoForm/blob/main/Mojang.md
