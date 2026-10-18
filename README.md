# Locks Reforged

An unofficial port of the **Locks** mod by [Melonslise](https://github.com/Melonslise) to **Minecraft NeoForge 1.21.1**.

> Locks is a small, but unique Minecraft mod that introduces flexible and universal locks which can be dynamically attached to multiple blocks of any kind, including other mods, a fun lock picking mechanic as well as lots of other little, but useful tools and utilities.

## Credits

- **Original Author:** [Melonslise](https://github.com/Melonslise) (MercurialPony)
- **Original Repository:** [github.com/Melonslise/Locks](https://github.com/Melonslise/Locks)
- **CurseForge:** [curseforge.com/projects/locks](https://minecraft.curseforge.com/projects/locks)
- **Textures:** Hoonts, Artsy (ydgy) from the RLCraft community
- **Sounds:** [freesound.org](https://freesound.org)
- **Coding help:** Diesieben07, Choonster, Gigaherz, Tterrag, McJty, and many others from the MMD community (see `mods.toml` for full credits)

This port preserves the original mod's license: **Attribution-NonCommercial 3.0 Unported (CC BY-NC 3.0)**.

## About This Port

The original Locks mod was built for Minecraft 1.16.5 (Forge 36.x). This port updates it to Minecraft 1.21.1 (NeoForge 21.1.x) while preserving the original gameplay mechanics, item IDs, config keys, and network protocol where possible.

**Version:** 1.8.0 | **Minecraft:** 1.21.1 | **NeoForge:** 21.1.216+ | **Java:** 21

## Features

### Universal Lock System
Locks can be dynamically attached to **any** block in the game -- not just chests. This includes doors, trapdoors, furnaces, dispensers, hoppers, and blocks from other mods. Locks are spatial, meaning a single lock can cover multiple blocks at once.

### Data-Driven Locks & Lock Picks
All lock and lock pick types are defined via JSON files, making it easy to add custom items or tweak stats without touching code. The mod ships with seven default tiers of each (Wood through Netherite), but you can add your own through the config folder or override stats via datapacks.

### Items

| Category | Items |
|----------|-------|
| **Locks** | Wood, Copper, Iron, Steel, Gold, Diamond, Netherite (+ custom) |
| **Keys** | Key Blank, Key, Master Key, Key Ring |
| **Lock Picks** | Wood, Bobby Pin (copper), Iron, Steel, Gold, Diamond, Netherite (+ custom) |
| **Components** | Spring, Wood/Copper/Iron/Steel Lock Mechanisms |

> **Steel comes from your modpack.** Locks Reforged does not ship a steel material. Every steel recipe uses the standard **`c:ingots/steel`** / **`c:nuggets/steel`** tags, so steel from Create, Immersive Engineering, Mekanism, Thermal, … works interchangeably.
>
> **The steel tier is craft-only.** Without a steel provider the recipes are not loaded at all — no broken recipes, no missing-ingredient placeholders — and with one, the steel lock, lock pick and steel mechanism can be **only** crafted, never found. They appear in no chest loot table, no villager trade and no wandering-trader trade, and the steel lock is absent from the default **Generated Locks** / **Loot Value Tiers** lists so it never spawns on a generated chest either.
>
> **The steel lock stands outside the progression.** It does not gate anything else: the gold and diamond locks are crafted with the **iron** mechanism, never the steel one.
>
> **A steel lock can never appear on a chest without a steel provider.** This is enforced in code, not just by the default config: `locks:steel_lock` is in the item tag **`locks:steel_provider_locks`**, and while `c:ingots/steel` is empty those locks are stripped out of both generation tables — even if a `locks-common.toml` written by an older Locks version still lists them. The config comment on `Generated Locks` is therefore only a convenience; a stale config cannot put the steel lock back into your world. An empty `c:ingots/steel` is logged once at data load, along with a warning naming any steel-gated lock that was skipped.
>
> **Modpack authors:** if your steel mod does *not* register its steel into `c:ingots/steel` / `c:nuggets/steel`, add a small datapack tag entry so Locks can use it. Add other steel-dependent locks to `locks:steel_provider_locks` to gate them the same way.

### Keys and Key Pairing

A Key Blank is uncut — it opens nothing until you pair it with a lock. Pairing copies the lock's ID onto the key, and it happens **before the lock is placed**:

1. Craft a **Key Blank**.
2. Before placing the lock, put one Key Blank and the lock together in any crafting grid — the 2×2 inventory grid works, and so does a crafting table.
3. The recipe returns the lock and gives you a **Key** carrying the same lock ID.
4. Place the lock. That Key now unlocks and re-locks it.
5. To make a spare, craft the paired Key together with another Key Blank.

The recipe is shapeless, and only one blank is consumed per key, so shift-crafting a stack of blanks gives you one key each.

> **A blank key cannot copy a lock that is already placed.** That is deliberate, not a limitation: if holding a blank against a placed lock cut a matching key, any visitor could manufacture one and the lock would protect nothing. Right-clicking a placed lock with a blank shows a reminder and changes nothing. If you placed a lock without pairing it first, remove it (sneak + empty hand on an unlocked lock), pair it, then place it again.

Beyond a single key:

- A **Key Ring** holds many keys at once; carrying it opens any lock whose key is inside. With Curios installed it also works from a curio slot, without occupying your hand.
- A **Master Key** opens any lock, regardless of ID.
- The **Awareness** enchantment binds a lock to whoever placed it. That lock then does not apply to them at all: an ordinary right-click opens the chest or door and leaves the lock untouched, so it stays shut to everyone else and is never left open behind you. Sneak + right-click with an empty hand opens the lock itself; sneak again on the open lock to take it off.

### Lock Picking Minigame
An interactive lock picking mechanic with a pin-matching system. Each lock has a unique combination based on its complexity. Higher-tier lock picks are more effective against tougher locks.

**Lock picks use durability (new in 1.7.3).** Every wrong pin costs the held pick a fixed amount of durability, and the pick breaks only when that durability reaches zero — there is no random break chance any more. How much a mistake costs is decided by the **lock** you are picking (its `pick_wear`), so a wrong pin on a netherite lock chews through a pick far faster than one on a wood lock. A pick's own `durability` decides how many of those mistakes it can absorb. The shipped values give a pick roughly **32 mistakes against a lock of its own tier**; take a wood pick to a netherite lock and a single wrong pin ends it.

| Tier | Pick durability | Lock `pick_wear` | Wrong pins, same tier |
|------|-----------------|------------------|-----------------------|
| Wood | 32 | 1 | 32 |
| Gold | 40 | 2 | 20 |
| Copper | 48 | 2 | 24 |
| Iron | 96 | 3 | 32 |
| Steel | 192 | 6 | 32 |
| Diamond | 384 | 12 | 32 |
| Netherite | 768 | 24 | 32 |

A guess that is off by exactly one pin costs about a third as much (never less than 1). Because picks are now damageable tools they stack to one, and Mending or an anvil-applied Unbreaking work on every tier. `strength` no longer affects breakage at all — it decides only which locks a pick may attempt in the first place (the Complexity gate).

**Itemless lock picking** is available as an opt-in server option. With `Allow Itemless Lock Picking = true`, right-clicking a locked block with an empty main hand plays the same pin minigame with no lock pick at all. Itemless attempts never consume or damage an item and are never blocked by Complexity, but a wrong pin drops every solved pin, so the minigame keeps a real failure cost. Physical lock picks are unchanged and always take precedence — as do a matching key, key ring, Curios ring, or Awareness ownership, so an empty hand never forces you into the minigame when you could simply open the lock.

### Enchantments

Locks define resistance; lock picks define technique. **Lock enchantments** go on the lock and make it harder to defeat; **lock pick enchantments** go on the pick and provide counterplay.

**Lock enchantments** (applied to locks):

| Enchantment | Max Level | Effect |
|-------------|-----------|--------|
| **Shocking** | V | Electrocutes players who fail to pick the lock (bypasses armor) |
| **Sturdy** | III | Makes every wrong pin cost the lock pick more durability |
| **Complexity** | III | Makes the lock impossible to pick with lower-tier lock picks |
| **Silent** | I | Suppresses the rattle sound when access is denied. Incompatible with Shocking |
| **Auto-Pick** | III | 10%/20%/30% chance to instantly open the lock, bypassing the minigame. Incompatible with Complexity |
| **Reinforced** | III | Increases explosion resistance by 50%/100%/150%. Protects against TNT and creepers |
| **Awareness** | I | The lock does not apply to whoever placed it — the block opens for them while staying locked to everyone else. Sneak + right-click with an empty hand to unlock it |

**Lock pick enchantments** (applied to lock picks — counterplay to the lock enchantments; new in 1.7.0):

| Enchantment | Max Level | Effect |
|-------------|-----------|--------|
| **Finesse** | III | Reduces the durability a wrong pin costs (counters Sturdy). Never affects Complexity. Incompatible with Last Catch |
| **Attunement** | II | Increases effective pick strength against complex locks (counters Complexity), letting a lower-tier pick cross a threshold |
| **Grounded** | III | Reduces damage taken from Shocking locks while holding the pick, in either hand (counters Shocking) |
| **Quiet Hand** | I | Reduces the sound of failed pin attempts |
| **Last Catch** | I | ~20% chance that a wrong pin costs no durability at all. Incompatible with Finesse |

Lock picks are enchantable at the enchanting table (see the `enchantment_value` field below). Each enchantment can be individually enabled — and its effect tuned — in the server config.

### World Generation
Locked chests spawn naturally in structure chests across the **Overworld, the Nether and the End**, as well as in modded dimensions. The lock tier is chosen by **loot value** — the richer a chest's loot table, the stronger its lock. Chests whose loot value falls **below the lowest configured threshold receive no lock at all**, while very valuable chests can receive diamond or netherite locks. The estimator weighs item rarity, enchantments, and stack counts (with sub-linear scaling so bulk common items don't inflate value) and supports per-item value overrides for materials like diamonds and netherite that are valuable despite a common rarity. The whole system can be switched to random weighted selection in the common config.

**The netherite lock is End-exclusive.** It only generates in End biomes, never in the Overworld, the Nether or a modded dimension — a chest there that qualifies for netherite drops to the next-best tier (diamond) rather than going unlocked. The same rule applies to locks randomised onto structure templates. Two datapack tags control it: `locks:biome_gated_locks` lists the restricted locks (ships with `locks:netherite_lock`), and `locks:biome_gated_lock_biomes` lists the biomes they may appear in (ships with `#minecraft:is_end`). Clear or extend either one to change the rule without touching the mod.

**Not every chest is locked.** The default **Chest Lock Chance** is 0.7, so roughly three in ten structure chests are left bare — a lock on a crate is meant to be something you come across, not a given. Set it to 1.0 to lock every chest or lower it to leave more of them open.

> **Can I make diamond/netherite locks appear only on high-value chests?** Yes — raise the upper **Loot Value Tiers** so only the richest loot reaches them, and raise the lowest threshold so poor chests get no lock. See [Configuration](#high-value-chest-locks-diamondnetherite) for a worked example. Note that the netherite tier additionally only rolls in the End, regardless of loot value.

### Villager & Wandering Trader Integration
A configurable villager profession (default toolsmith) sells lock picks and lock mechanisms, and the wandering trader offers lock picks and enchanted locks. Every category — lock picks, locks, and mechanisms — can be toggled independently in the server config, so you can disable easy lock picks while still selling locks. Villager lock sales (early-game wood/copper/iron locks) are available as an opt-in.

> **No steel is ever traded.** Neither a villager nor a wandering trader sells a steel lock pick, steel lock or steel mechanism — the steel tier is craft-only, from whatever steel another mod provides. The wandering trader's *lock mechanism* trade category is therefore gone as well, since the steel mechanism was its only entry.

### Loot Table Integration
Lock picks and lock mechanisms can be found in dungeon, temple, and other structure chests.

### Protection Features
- Locked doors cannot be opened by villagers or other door-opening AI — including on a lock's own owner's door, which stays shut to them
- Locked blocks resist redstone activation
- Locked containers block hopper extraction
- Locked blocks resist piston movement
- Locked blocks resist explosion damage
- Configurable break protection for locked blocks

## Custom Locks & Lock Picks

Locks Reforged uses a two-tier data-driven system for defining lock and lock pick items.

### Tier 1: Item Definitions (loaded at startup)

JSON files that define new items. These are read during mod initialization, before the game starts.

**Mod defaults** are shipped in the JAR under `data/locks/lock_types/` and `data/locks/lockpick_types/`.

**Custom items** go in the game's config folder:
- `config/locks/lock_types/<name>.json` — registers a new lock item as `locks:<name>`
- `config/locks/lockpick_types/<name>.json` — registers a new lock pick item as `locks:<name>`

On first launch, the mod creates these directories with a `_example.json.disabled` template in each.

**Lock definition schema:**
```json
{
  "length": 7,
  "enchantment_value": 14,
  "resistance": 12,
  "pick_wear": 3,
  "fire_resistant": false
}
```

| Field | Description |
|-------|-------------|
| `length` | Number of pins in the lock picking minigame |
| `enchantment_value` | Enchantability (higher = better enchantments) |
| `resistance` | Damage resistance of the lock |
| `pick_wear` | Lock pick durability a wrong pin costs on this lock, 1–1000 (optional, default 1) |
| `fire_resistant` | Whether the item survives in lava/fire (optional, default false) |

**Lock pick definition schema:**
```json
{
  "strength": 0.35,
  "enchantment_value": 10,
  "durability": 96,
  "fire_resistant": false
}
```

| Field | Description |
|-------|-------------|
| `strength` | Which locks the pick may attempt — the Complexity gate (0.0–10.0, higher = stronger). Does **not** affect how fast it wears out |
| `enchantment_value` | Enchantability at the enchanting table (higher = better enchantments; optional, default 0 = not table-enchantable) |
| `durability` | How many durability points the pick has before it breaks, 0–10000 (optional, default 64). **0 registers an unbreakable pick** that never wears down |
| `fire_resistant` | Whether the item survives in lava/fire (optional, default false) |

**Important:** Custom items added via config also need:
- A model JSON in a resource pack (`assets/locks/models/item/<name>.json`)
- A texture (`assets/locks/textures/item/<name>.png`)
- A GUI texture for the lock picking screen (`assets/locks/textures/lockpicking/<name>.png`) — falls back to the iron lock texture if missing
- A recipe (via datapack)
- Tag entries in `locks:locks` or `locks:lock_picks` (via datapack) for the mod to recognize them

### Tier 2: Stat Overrides (loaded per-world via datapacks)

Datapacks can override the stats of already-registered items without restarting the game. Overrides are applied on world load and on `/reload`.

Place override files at:
- `data/<namespace>/locks/lock_stat_overrides/<name>.json`
- `data/<namespace>/locks/lockpick_stat_overrides/<name>.json`

**Lock stat override schema:**
```json
{
  "item": "locks:iron_lock",
  "length": 3,
  "resistance": 50
}
```

Only fields present in the override are changed; omitted fields keep their default values. The `item` field is required and specifies which registered item to modify.

**Lock pick stat override schema:**
```json
{
  "item": "locks:iron_lock_pick",
  "strength": 0.5
}
```

A lock's `pick_wear` **can** be overridden this way, because it is read fresh on every wrong pin. A lock pick's `durability` **cannot** — it is baked into the item when it is registered at startup, long before any datapack loads, so a `durability` key here is ignored with a warning in the log. Set pick durability in `config/locks/lockpick_types/<name>.json` instead, which is read before registration.

> **Note:** Stat overrides cannot create new items — they can only modify items that were already registered at startup. Existing items in the world that have already had their stats baked into NBT (e.g., a lock whose length was written on first placement) will retain their original values.

## Configuration

### Common Config (`locks-common.toml`)
- **Chest Lock Chance** -- Chance (0.0–1.0) that a generated chest receives a lock *at all*. Default 0.7 (roughly 3 in 10 chests stay bare); 1.0 locks every chest, 0.0 disables lock generation. Renamed from `Lock Generation Chance` in 1.8.0 so existing configs pick up the new default instead of keeping their stored 1.0
- **Generation Enchant Chance** -- Probability of generated locks being enchanted (default: 40%)
- **Generated Locks & Generated Lock Chances** -- Which locks generate and their relative weights (used when Loot-Scaled Locks is disabled)
- **Randomize Loaded Locks** -- Whether to randomize lock combinations when loading them from structure files
- **Loot-Scaled Locks** -- When enabled (default), lock tier is chosen from a chest's loot value instead of random weights. Configurable item value formula with rarity multipliers, enchantment bonuses, per-tier value thresholds (**Loot Value Tiers**), sub-linear stack count scaling, and per-item value overrides. Chests below the lowest tier threshold receive **no lock**
- **Lock Stats & Lockpick Stats** -- Override built-in lock and lock pick stats without datapacks (set any value to -1 to keep the JSON default). Includes each lock's **Pick Wear**, the durability a wrong pin costs a lock pick. Pick durability is not here — it is fixed at registration; set it in `config/locks/lockpick_types/`

### Client Config (`locks-client.toml`)
- **Deaf Mode** -- Enables visual feedback for the lock picking mechanic

### Server Config (`locks-server.toml`)
- **Allow Removing Locks** -- Whether players can remove unlocked locks by shift-right-clicking
- **Protect Lockables** -- Whether locked blocks are protected from being broken
- **Max Lockable Volume / Lockable Blocks / Lockable Tags** -- What can be locked and how large a single lock may be
- **Display** (Hide Lock ID / Hide HUD Enchantments / Hide HUD Tooltip) -- Tooltip and HUD visibility toggles
- **Enchantment Toggles** -- Each of the 12 enchantments — 7 lock-side (Shocking, Sturdy, Complexity, Silent, Auto-Pick, Reinforced, Awareness) and 5 lock-pick-side (Finesse, Attunement, Grounded, Quiet Hand, Last Catch) — can be individually enabled or disabled, and the lock-pick enchantments' effects tuned, under the **Enchantments** section
- **Chest Lock Chance** (in `locks-common.toml`) -- Chance (0.0-1.0) that an eligible generated chest receives a lock at all, default 0.7 so about 30% of chests stay bare. `1.0` locks every chest, `0.0` disables lock generation
- **Shocking** subsection -- Tune Shocking damage and theft punishments (see below)
- **Netherite Lockpick Unbreakable** -- When enabled, netherite lock picks never break during lock picking (default: false)
- **Allow Itemless Lock Picking** -- When enabled, an empty main hand can play the lock picking minigame without a lock pick (default: false). Itemless attempts consume nothing, ignore Complexity, and reset progress on a wrong pin. Lock pick items, recipes, loot, trades and enchantments are untouched either way
- **Loot Table Injection Patterns** -- Which loot tables receive lock pick / key injection (default: `minecraft:chests/`)
- **Trades** subsection -- Configure villager and wandering trader sales (see below)

#### Shocking & Theft Punishment (`[Enchantments.Shocking]`)

The Shocking enchantment punishes thieves. **By default, Shocking deals 1.5 damage per enchantment level when a lock pick breaks** — exactly as in previous versions. The formula and what triggers it are now configurable:

| Option | Default | Description |
|--------|---------|-------------|
| `Shocking Damage Base` | 0.0 | Flat damage regardless of level (2.0 = 1 heart) |
| `Shocking Damage Per Level` | 1.5 | Damage per enchantment level. Final damage = Base + level × Per Level, clamped to Max |
| `Shocking Max Damage` | 1024.0 | Upper clamp (effectively uncapped) |
| `Shocking Requires Enchantment` | true | If false, *every* lock shocks (as level 1 when unenchanted) |
| `Shocking Cooldown Ticks` | 0 | Min ticks between shocks to the same player (0 = none). Raise to ~20 when enabling the triggers below |
| `Shocking Triggers On Pick Break` | true | The original behavior: shock when a lock pick breaks. Since 1.7.3 a pick breaks only when its durability runs out, so this fires far less often |
| `Shocking Triggers On Wrong Pin` | false | Shock on each wrong pin during picking |
| `Shocking Triggers On Unauthorized Interaction` | false | Shock when interacting with a locked block without a key |
| `Shocking Triggers On Block Break Attempt` | false | Shock when trying to break a protected locked block |

Creative players are exempt. To make locks aggressively punish theft, enable the extra triggers (and optionally set `Shocking Requires Enchantment = false` so even plain locks bite). To make Shocking hurt more, raise `Shocking Damage Per Level` or `Shocking Damage Base`.

#### Trades (`[Trades]`)

| Option | Default | Description |
|--------|---------|-------------|
| `Villager.Enable Villager Trades` | true | Master switch for villager lock trades |
| `Villager.Enable Villager Lockpick Trades` | true | Sell lock picks (set false to remove the powerful early lock picks) |
| `Villager.Enable Villager Lock Trades` | false | **Opt-in:** sell wood/copper/iron locks at levels 1–3 |
| `Villager.Enable Villager Lock Mechanism Trades` | true | Sell lock mechanisms |
| `Villager.Villager Profession` | `minecraft:toolsmith` | Which profession offers lock trades |
| `Wandering Trader.Enable Wandering Trader Trades` | true | Master switch for wanderer lock trades |
| `Wandering Trader.Enable Wandering Trader Lockpick Trades` | true | Sell lock picks |
| `Wandering Trader.Enable Wandering Trader Lock Trades` | true | Sell enchanted diamond/netherite locks |

**To disable lock pick sales while keeping locks purchasable** (lock picks are powerful; locks help early-game chest protection): set `Enable Villager Lockpick Trades = false` and `Enable Villager Lock Trades = true` (and similarly `Enable Wandering Trader Lockpick Trades = false` while keeping `Enable Wandering Trader Lock Trades = true`). All default trades are unchanged when the config is left at defaults.

#### High-Value Chest Locks (diamond/netherite)

Loot-scaled lock generation reserves the best locks for the best chests. The **Loot Value Tiers** list (in `locks-common.toml`) holds one minimum value per entry in **Generated Locks** (same order). A chest receives the highest tier whose threshold its loot value meets; below the lowest threshold it gets no lock.

To make **diamond or better appear only on high-value chests**, raise the upper thresholds and the floor. With the default six locks (`wood, copper, iron, gold, diamond, netherite` — the steel lock is deliberately not part of world generation):

```toml
[Loot-Scaled Locks]
    Enable Loot-Scaled Locks = true
    #                   wood  copper iron  gold  diamond netherite
    Loot Value Tiers = [10.0, 20.0,  35.0, 65.0, 90.0,   150.0]
```

The shipped defaults are `2, 5, 9, 13, 20, 21`, placed against vanilla 1.21.1's measured chest-loot distribution (`min 0.0, p25 4.9, median 9.8, p75 15.9, p90 22.2, max 50.4`). These live in **`config/locks-common.toml`** under `["Loot-Scaled Locks"]` as `"Loot Value Tiers"`, in the same order as **Generated Locks** — edit that file (with the game closed) to change them; the number in the source is only the default written on first start. Note that deleting a tier shifts the question of where the thresholds go: simply dropping a value would widen the band below it, and a too-wide band swallows most chests (removing steel without shifting left iron covering everything worth 10–24, i.e. nearly every structure chest). Re-space them when you remove a tier.

> **The diamond value is the Overworld's effective top tier.** A chest that would have earned a netherite lock falls back to diamond outside the End, so *every* chest worth at least the diamond value is a candidate for one.

### Tier chances — a qualifying tier is possible, not guaranteed

Reaching a tier does not hand it to you. A chest starts at the highest tier its loot value qualifies for, rolls **`Lock Tier Chances`** (also in `locks-common.toml`, one value per entry in `Generated Locks`), and on a failed roll steps down to the next tier and rolls again. Richer chests still skew high, because a chest only ever rolls tiers it actually qualifies for.

Defaults `1.0, 0.85, 0.7, 0.55, 0.35, 0.5` (wood→netherite), which produce:

| Chest | Outcome |
|---|---|
| Village (4.5) | wood 100% |
| Median (9.8) | iron 70%, copper 25%, wood 5% |
| p75 (15.9) | gold 55%, iron 32%, copper 12%, wood 2% |
| Ancient City (29.4, Overworld) | diamond 35%, gold 36%, iron 20%, copper 7%, wood 1% |
| End City (22.2) | netherite 50%, diamond 18%, gold 18%, iron 10%, copper 4%, wood 1% |

Set a tier to `1.0` to make it certain once reached, or `0.0` to drop it from the roll entirely. Without this, every chest above a threshold came out as the exact same tier.

> **Netherite is deliberately not 1.0.** It stays the most likely roll in the End — it is the reason to go there — but an end city chest can come up with any tier, not just netherite. Raise it toward `1.0` if you want the End to feel like a guaranteed jackpot.

**Tune them against your pack's economy, not guesswork.** The shipped values were placed against vanilla 1.21.1's measured chest-loot distribution (56 tables: `min 0.0, p25 4.9, median 9.8, p75 15.9, p90 22.2, max 50.4`). A modpack that inflates item values pushes every chest up the ladder, so re-space them if you also touch `Default Item Value`, the rarity multipliers or `Item Value Overrides`.

With the shipped defaults a village chest (worth ~4.5) always gets **wood**, a chest near the median (~9.8) lands on **iron** 70% of the time, an abandoned mineshaft or simple dungeon (~19.1) on **gold**, and in the Overworld only the genuinely rich tables — ancient city, bastion, buried treasure, shipwreck — reach **diamond**, and only 35% of the time. Trial chamber rewards (worth under 2) get **no lock** at all. Tune `Default Item Value`, the rarity multipliers, and `Item Value Overrides` to change how loot value is scored.

> **The netherite value must stay at or below ~22.** `chests/end_city_treasure` (worth 22.2) is the *only* End chest table in the game, and the netherite lock is End-gated. Any higher threshold makes the netherite lock impossible to generate anywhere, not merely rare.

Remember that the netherite tier is additionally **End-only** — a threshold of 150 only ever produces a netherite lock in the End; elsewhere that chest falls back to diamond. See [World Generation](#world-generation).

## Building from Source

**Requirements:** JDK 21

```bash
# Clone the repository
git clone <repo-url>
cd "Locks Reforged"

# Build the mod JAR
JAVA_HOME="/path/to/jdk-21" ./gradlew build

# Output: build/libs/locks_reforged-1.8.0.jar

# Run the development client
JAVA_HOME="/path/to/jdk-21" ./gradlew runClient
```

## Installation

1. Install [NeoForge for Minecraft 1.21.1](https://neoforged.net/) (21.1.216 or later)
2. Download `locks_reforged-1.8.0.jar` from the releases
3. Place the JAR in your `.minecraft/mods/` folder
4. Launch Minecraft with the NeoForge profile

## Technical Details

This port involved updating approximately 90 Java source files across the following API changes:

- **Registry System** -- Migrated to deferred lambda suppliers for all registry entries
- **Capabilities** -- Moved to NeoForge's attachment API
- **World Generation** -- Converted to data-driven JSON biome modifiers (`data/locks/neoforge/biome_modifier/`), restored and split per dimension group in 1.8.0 after the Forge port dropped them
- **Damage Types** -- Migrated to data-driven damage type system
- **Rendering** -- Updated to GuiGraphics API and JOML math library
- **Creative Tabs** -- Migrated from `ItemGroup` to `CreativeModeTab` via `DeferredRegister`
- **Mixins** -- All common mixins updated for 1.21.1 class/method changes
- **Loot Tables** -- Reimplemented injection via `LootTableLoadEvent`

See [KNOWN_ISSUES.md](KNOWN_ISSUES.md) for the full list of changes and current testing status.

## License

This project is distributed under the **Attribution-NonCommercial 3.0 Unported (CC BY-NC 3.0)** license, consistent with the original Locks mod.
