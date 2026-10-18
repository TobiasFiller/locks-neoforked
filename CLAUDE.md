# Locks Reforged — Forge 1.21.1 (Ported from 1.16.5)

## Quick Reference
- **Mod ID**: `locks`
- **Package**: `melonslise.locks` (original author's package, NOT com.otectus)
- **Version**: 1.8.0 (set in `gradle.properties` as `mod_version`; `neoforge.mods.toml` pulls it via `${mod_version}`)
- **MC**: 1.21.1 | **Forge**: 52.1.2 | **Java**: 21
- **Mappings**: Official
- **License**: CC BY-NC 3.0 (Attribution-NonCommercial)
- **Original Author**: Melonslise

## Purpose
Universal lock system for Minecraft. Locks can be dynamically attached to multiple blocks (including modded blocks). Features lock picking mechanic, keyring, master keys, and villager trades.

## Build
- `./gradlew build` — full build (output: build/libs/)
- `./gradlew compileJava` — compile-only
- `./gradlew runClient` — launch dev client
- `./gradlew runData` — run data generation

## Project Structure
```
src/main/java/melonslise/locks/
  ├── client/      — rendering, GUI, key bindings
  ├── common/      — items, blocks, capabilities, network, events
  └── mixin/       — 15 common mixins (chunk, level, door, piston, explosion, etc.)
src/main/resources/
  ├── META-INF/mods.toml
  ├── META-INF/accesstransformer.cfg  — ATs for Frustum, LevelRenderer, GameRenderer, VillagerTrades, PistonStructureResolver, Explosion, loot tables
  ├── locks.mixins.json               — 15 common mixins; the ONLY base manifest the build packages
  └── assets/locks/, data/locks/
source-1.16.5/   — archived original source (porting reference)
```

## Key Dependencies
- **Curios** (8.1.0+1.20.6 API) — optional, keyring as curio slot (compileOnly API)
- **Carry On** (2.2.2 for Forge 1.21.1) — optional, compiled only when vendored at `libs/carryon-forge-1.21.1-2.2.2.jar`; adds `locks_carryon.mixins.json`

## Porting Context
This mod was ported from 1.16.5 and now targets Minecraft 1.21.1 / Forge 52.x. Key references:
- `PORTING_NOTES.md` — detailed migration guide (class renames, API changes)
- `KNOWN_ISSUES.md` — runtime testing status and resolved issues
- `source-1.16.5/` — original source for comparison

## Conventions

### Git workflow (hard rules)
- **Every file in the working tree uses CRLF line endings.** Java, JSON, Markdown, TOML — all of it.
  `.gitattributes` has `* text=auto` and `core.autocrlf=true`, so Git normalizes to LF on commit and checks
  out CRLF on Windows. That only works if the file on disk actually contains CRLF: tools that write files
  directly (scripted edits, `Set-Content`, Python `write_text`) can silently produce LF-only files and trigger
  `LF will be replaced by CRLF the next time Git touches it`. After writing a file this way, normalize it:
  ```powershell
  $p = (Resolve-Path <file>).Path
  $c = [System.IO.File]::ReadAllText($p) -replace "`r`n","`n" -replace "`n","`r`n"
  [System.IO.File]::WriteAllText($p, $c)
  ```
  `[System.IO.File]::WriteAllText` is safe on both PowerShell 5.1 and 7 — it writes UTF-8 **without** a BOM
  (verified), so it does not corrupt files that must stay BOM-less. The `write`/`edit` tools are preferable for
  targeted edits since they preserve the file's existing line endings on their own.
- **`git add` every newly created file immediately**, in the same turn it is created. New files are untracked
  and will otherwise be missed when committing. This applies to new sources, resources, docs and scripts alike.
- `git rm` deletions are fine (they stage themselves); regular edits need `git add` before commit.
- Do not `git stash` on the user's behalf — if a stash is created, it must be popped or dropped in the same
  session, and a clean pop leaves no `refs/stash` reflog, so the stashed work becomes unrecoverable.

### Code conventions
- Registration: DeferredRegister on MOD bus
- Capabilities: AttachCapabilitiesEvent for lock storage on chunks/levels
- Mixins: `src/main/resources/locks.mixins.json` is the only manifest the build packages — add new mixins there.
  `locks_carryon.mixins.json` is a second, optional config present only when the Carry On jar is vendored.
  (1.7.2 deleted two dead root-level copies, `locks.mixins.json` and a stray `curios.mixins.json` that belonged
  to Curios itself; the project root is not a resource root, so neither was ever read.)
- Access Transformers: used for Frustum, renderers, villager trades, piston internals, explosion level, loot tables
- Official mappings (SRG names in AT config and mixin targets)
- Data generation: `./gradlew runData` with src/generated/resources/
- Tests: `./gradlew test` — plain JUnit 5, no FML/Bootstrap launch, so tests may only touch pure data classes
  (`BlockPos`, `CompoundTag`, `FriendlyByteBuf`, `Cuboid6i`, `Lock`) and pure policy classes. Anything needing
  `ItemStack`, config values, tags, `Level` or `Minecraft` must be manual QA — see KNOWN_ISSUES.md. No GameTest.
