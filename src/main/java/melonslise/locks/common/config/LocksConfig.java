package melonslise.locks.common.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import com.google.common.collect.Lists;

import melonslise.locks.Locks;
import melonslise.locks.common.init.LocksItemTags;
import melonslise.locks.common.init.LockTypeRegistry;
import melonslise.locks.common.util.LocksUtil;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class LocksConfig
{
	public static final ModConfigSpec SPEC;

	public static final ModConfigSpec.DoubleValue GENERATION_CHANCE;
	public static final ModConfigSpec.DoubleValue GENERATION_ENCHANT_CHANCE;
	public static final ModConfigSpec.ConfigValue<List<? extends String>> GENERATED_LOCKS;
	public static final ModConfigSpec.ConfigValue<List<? extends Integer>> GENERATED_LOCK_WEIGHTS;
	public static final ModConfigSpec.ConfigValue<List<? extends Double>> LOCK_TIER_CHANCES;
	public static final ModConfigSpec.BooleanValue RANDOMIZE_LOADED_LOCKS;

	// Lock stat overrides (sentinel -1 = use JSON default)
	public static final ModConfigSpec.IntValue WOOD_LOCK_LENGTH, WOOD_LOCK_ENCHANT, WOOD_LOCK_RESISTANCE, WOOD_LOCK_PICK_WEAR;
	public static final ModConfigSpec.IntValue COPPER_LOCK_LENGTH, COPPER_LOCK_ENCHANT, COPPER_LOCK_RESISTANCE, COPPER_LOCK_PICK_WEAR;
	public static final ModConfigSpec.IntValue IRON_LOCK_LENGTH, IRON_LOCK_ENCHANT, IRON_LOCK_RESISTANCE, IRON_LOCK_PICK_WEAR;
	public static final ModConfigSpec.IntValue STEEL_LOCK_LENGTH, STEEL_LOCK_ENCHANT, STEEL_LOCK_RESISTANCE, STEEL_LOCK_PICK_WEAR;
	public static final ModConfigSpec.IntValue GOLD_LOCK_LENGTH, GOLD_LOCK_ENCHANT, GOLD_LOCK_RESISTANCE, GOLD_LOCK_PICK_WEAR;
	public static final ModConfigSpec.IntValue DIAMOND_LOCK_LENGTH, DIAMOND_LOCK_ENCHANT, DIAMOND_LOCK_RESISTANCE, DIAMOND_LOCK_PICK_WEAR;
	public static final ModConfigSpec.IntValue NETHERITE_LOCK_LENGTH, NETHERITE_LOCK_ENCHANT, NETHERITE_LOCK_RESISTANCE, NETHERITE_LOCK_PICK_WEAR;

	// Lockpick stat overrides (sentinel -1.0 = use JSON default)
	public static final ModConfigSpec.DoubleValue WOOD_PICK_STRENGTH, COPPER_PICK_STRENGTH, IRON_PICK_STRENGTH, STEEL_PICK_STRENGTH;
	public static final ModConfigSpec.DoubleValue GOLD_PICK_STRENGTH, DIAMOND_PICK_STRENGTH, NETHERITE_PICK_STRENGTH;

	// Loot-scaled lock generation
	public static final ModConfigSpec.BooleanValue LOOT_SCALED_LOCKS;
	public static final ModConfigSpec.DoubleValue DEFAULT_ITEM_VALUE;
	public static final ModConfigSpec.DoubleValue ENCHANT_VALUE_BONUS;
	public static final ModConfigSpec.DoubleValue RARITY_COMMON_MULT;
	public static final ModConfigSpec.DoubleValue RARITY_UNCOMMON_MULT;
	public static final ModConfigSpec.DoubleValue RARITY_RARE_MULT;
	public static final ModConfigSpec.DoubleValue RARITY_EPIC_MULT;
	public static final ModConfigSpec.ConfigValue<List<? extends Double>> LOOT_VALUE_TIERS;
	public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_VALUE_OVERRIDES;

	// Rebuilt by init(), which now also runs on every tag load (a /reload can change BIOME_GATED_LOCKS), so these
	// are read by worldgen threads while the server thread may be replacing them. volatile + immutable TreeMaps
	// gives safe publication.
	public static volatile NavigableMap<Integer, Item> weightedGeneratedLocks;
	public static volatile int weightTotal;
	public static volatile NavigableMap<Double, Item> lootValueTierMap;
	public static Map<ResourceLocation, Double> itemValueOverrides;

	/**
	 * The same two tables with every {@link LocksItemTags#BIOME_GATED_LOCKS} lock removed. Generation draws from
	 * these whenever the chest's biome is not in {@link LocksBiomeTags#BIOME_GATED_LOCK_BIOMES}, so a gated lock
	 * never leaks into the wrong dimension. Empty when no lock is gated.
	 */
	public static volatile NavigableMap<Integer, Item> ungatedWeightedGeneratedLocks;
	public static volatile int ungatedWeightTotal;
	public static volatile NavigableMap<Double, Item> ungatedLootValueTierMap;

	/**
	 * Generation tables with steel-gated locks stripped out — used while {@code c:ingots/steel} is empty. The
	 * ungated variants additionally strip biome-gated locks. Same reasoning as the ungated tables: a lock that may
	 * not appear should neither appear nor steal weight from the rest.
	 */
	public static volatile int noSteelWeightTotal;
	public static volatile NavigableMap<Integer, Item> noSteelWeightedGeneratedLocks;
	public static volatile NavigableMap<Double, Item> noSteelLootValueTierMap;
	public static volatile int noSteelUngatedWeightTotal;
	public static volatile NavigableMap<Integer, Item> noSteelUngatedWeightedGeneratedLocks;
	public static volatile NavigableMap<Double, Item> noSteelUngatedLootValueTierMap;

	/**
	 * Set of items in {@link LocksItemTags#BIOME_GATED_LOCKS}, snapshotted on every tag load because a datapack
	 * reload can change it independently of the config.
	 */
	private static volatile Set<Item> biomeGatedLocks = Set.of();

	/**
	 * Set of items in {@link LocksItemTags#STEEL_PROVIDER_LOCKS}, snapshotted on every tag load for the same
	 * reason.
	 */
	private static volatile Set<Item> steelProviderLocks = Set.of();

	/**
	 * Whether any mod populates {@code c:ingots/steel}. Decides whether {@link #steelProviderLocks} may be
	 * generated at all. This is a hard gate that outranks the config: a stale {@code locks-common.toml} written by
	 * an older version still lists {@code locks:steel_lock}, and changing the default alone would never remove it.
	 */
	private static volatile boolean steelAvailable = false;

	/**
	 * Per-tier keep-chance from {@link #LOCK_TIER_CHANCES}, keyed by lock item so the parallel config lists stay
	 * positional. Rebuilt in {@link #init()}.
	 */
	private static volatile Map<Item, Double> tierChances = Map.of();

	/** Guards the "configured steel lock, but no steel provider" warning so it is logged once per session, not per tag reload. */
	private static final java.util.concurrent.atomic.AtomicBoolean warnedAboutMissingSteel = new java.util.concurrent.atomic.AtomicBoolean();

	static
	{
		ModConfigSpec.Builder cfg = new ModConfigSpec.Builder();

		// 0.7 rather than 1.0: not every chest should be locked, otherwise a lock on a crate stops being a find.
		// Renamed from 'Lock Generation Chance' in 1.8.0 so that existing configs pick up the new default instead of
		// keeping their stored 1.0 — a key whose default changed would otherwise silently not apply to any server
		// that has already run the mod once.
		GENERATION_CHANCE = cfg
			.comment("Chance that a generated chest receives a lock at all. The default 0.7 leaves roughly three in",
				"ten chests bare, so locks stay worth finding. 1.0 locks every chest; 0.0 disables lock generation.")
			.defineInRange("Chest Lock Chance", 0.7d, 0d, 1d);
		GENERATION_ENCHANT_CHANCE = cfg
			.comment("Chance to randomly enchant a generated lock during world generation. Set to 0 to disable")
			.defineInRange("Generation Enchant Chance", 0.4d, 0d, 1d);
		GENERATED_LOCKS = cfg
			.comment("Items that can be generated as locks (must be instance of LockItem in code!)",
				"NOTE: 'locks:steel_lock' is deliberately NOT in this default list. The steel lock is not part of the",
				"natural progression (it needs a steel provider from another mod), so it never spawns on a generated",
				"chest. Add it here explicitly if you want it to.")
			.defineList("Generated Locks", Lists.newArrayList("locks:wood_lock", "locks:copper_lock", "locks:iron_lock", "locks:gold_lock", "locks:diamond_lock", "locks:netherite_lock"), e -> e instanceof String);
		GENERATED_LOCK_WEIGHTS= cfg
			.comment("WARNING: THE AMOUNT OF NUMBERS SHOULD BE EQUAL TO THE AMOUNT OF GENERATED LOCK ITEMS!!!", "The relative probability that the corresponding lock item will be generated on a chest. Higher number = higher chance to generate")
			.defineList("Generated Lock Chances", Lists.newArrayList(3, 3, 3, 2, 1, 1), e -> e instanceof Integer);
		LOCK_TIER_CHANCES = cfg
			.comment("Chance (0.0-1.0) that a chest keeps the highest tier its loot value qualifies for, one value per",
				"entry in 'Generated Locks' (same order). Only used by loot-scaled generation; the weighted 'Generated",
				"Lock Chances' list is the fallback when loot scaling is off or the loot value is unknown.",
				"On a failed roll the chest drops to the next tier down and rolls again, so a value that qualifies for",
				"diamond is not guaranteed to get one — the tier is merely POSSIBLE. Richer chests still skew high,",
				"because a chest only ever rolls tiers it actually qualifies for.",
				"A value of 1.0 makes that tier certain once reached; 0.0 removes it from the roll entirely.",
				"Netherite is deliberately NOT 1.0: it is the End's signature reward and still the most likely roll",
				"there, but an end city chest should be able to come up with any tier, not just netherite.",
				"Defaults: wood=1.0, copper=0.85, iron=0.7, gold=0.55, diamond=0.35, netherite=0.5.",
				"Resulting spread: a very rich overworld chest (diamond is the top eligible tier) comes out diamond 35%,",
				"gold 36%, iron 20%, copper 7%, wood 1%; a median chest is iron 70% / copper 25% / wood 5%; a village",
				"chest is always wood; an end city chest (worth 22.2) is netherite 50%, diamond 18%, gold 18%, iron",
				"10%, copper 4%, wood 1%.")
			.defineList("Lock Tier Chances", Lists.newArrayList(1.0, 0.85, 0.7, 0.55, 0.35, 0.5), e -> e instanceof Double);
		RANDOMIZE_LOADED_LOCKS = cfg
			.comment("Randomize lock IDs and combinations when loading them from a structure file. Randomization works just like during world generation")
			.define("Randomize Loaded Locks", false);

		// Lock Stats section
		cfg.comment("Override built-in lock stats without datapacks. Set any value to -1 to use the JSON default.",
				"'Pick Wear' is how much durability this lock takes off a lock pick on every wrong pin. It is the",
				"main difficulty knob alongside 'Length', since a pick breaks only when its durability runs out.").push("Lock Stats");

		cfg.push("Wood Lock");
		WOOD_LOCK_LENGTH = cfg.comment("Number of pins (1-20). JSON default: 5. Set to -1 to use default.").defineInRange("Length", -1, -1, 20);
		WOOD_LOCK_ENCHANT = cfg.comment("Enchantability (1-50). JSON default: 15.").defineInRange("Enchantment Value", -1, -1, 50);
		WOOD_LOCK_RESISTANCE = cfg.comment("Explosion resistance (0-1000). JSON default: 4.").defineInRange("Resistance", -1, -1, 1000);
		WOOD_LOCK_PICK_WEAR = cfg.comment("Lock pick durability a wrong pin costs on this lock (1-1000). JSON default: 1.").defineInRange("Pick Wear", -1, -1, 1000);
		cfg.pop();

		cfg.push("Copper Lock");
		COPPER_LOCK_LENGTH = cfg.comment("Number of pins (1-20). JSON default: 6. Set to -1 to use default.").defineInRange("Length", -1, -1, 20);
		COPPER_LOCK_ENCHANT = cfg.comment("Enchantability (1-50). JSON default: 16.").defineInRange("Enchantment Value", -1, -1, 50);
		COPPER_LOCK_RESISTANCE = cfg.comment("Explosion resistance (0-1000). JSON default: 8.").defineInRange("Resistance", -1, -1, 1000);
		COPPER_LOCK_PICK_WEAR = cfg.comment("Lock pick durability a wrong pin costs on this lock (1-1000). JSON default: 2.").defineInRange("Pick Wear", -1, -1, 1000);
		cfg.pop();

		cfg.push("Iron Lock");
		IRON_LOCK_LENGTH = cfg.comment("Number of pins (1-20). JSON default: 7. Set to -1 to use default.").defineInRange("Length", -1, -1, 20);
		IRON_LOCK_ENCHANT = cfg.comment("Enchantability (1-50). JSON default: 14.").defineInRange("Enchantment Value", -1, -1, 50);
		IRON_LOCK_RESISTANCE = cfg.comment("Explosion resistance (0-1000). JSON default: 12.").defineInRange("Resistance", -1, -1, 1000);
		IRON_LOCK_PICK_WEAR = cfg.comment("Lock pick durability a wrong pin costs on this lock (1-1000). JSON default: 3.").defineInRange("Pick Wear", -1, -1, 1000);
		cfg.pop();

		cfg.push("Steel Lock");
		STEEL_LOCK_LENGTH = cfg.comment("Number of pins (1-20). JSON default: 9. Set to -1 to use default.").defineInRange("Length", -1, -1, 20);
		STEEL_LOCK_ENCHANT = cfg.comment("Enchantability (1-50). JSON default: 12.").defineInRange("Enchantment Value", -1, -1, 50);
		STEEL_LOCK_RESISTANCE = cfg.comment("Explosion resistance (0-1000). JSON default: 20.").defineInRange("Resistance", -1, -1, 1000);
		STEEL_LOCK_PICK_WEAR = cfg.comment("Lock pick durability a wrong pin costs on this lock (1-1000). JSON default: 6.").defineInRange("Pick Wear", -1, -1, 1000);
		cfg.pop();

		cfg.push("Gold Lock");
		GOLD_LOCK_LENGTH = cfg.comment("Number of pins (1-20). JSON default: 6. Set to -1 to use default.").defineInRange("Length", -1, -1, 20);
		GOLD_LOCK_ENCHANT = cfg.comment("Enchantability (1-50). JSON default: 22.").defineInRange("Enchantment Value", -1, -1, 50);
		GOLD_LOCK_RESISTANCE = cfg.comment("Explosion resistance (0-1000). JSON default: 6.").defineInRange("Resistance", -1, -1, 1000);
		GOLD_LOCK_PICK_WEAR = cfg.comment("Lock pick durability a wrong pin costs on this lock (1-1000). JSON default: 2.").defineInRange("Pick Wear", -1, -1, 1000);
		cfg.pop();

		cfg.push("Diamond Lock");
		DIAMOND_LOCK_LENGTH = cfg.comment("Number of pins (1-20). JSON default: 11. Set to -1 to use default.").defineInRange("Length", -1, -1, 20);
		DIAMOND_LOCK_ENCHANT = cfg.comment("Enchantability (1-50). JSON default: 10.").defineInRange("Enchantment Value", -1, -1, 50);
		DIAMOND_LOCK_RESISTANCE = cfg.comment("Explosion resistance (0-1000). JSON default: 100.").defineInRange("Resistance", -1, -1, 1000);
		DIAMOND_LOCK_PICK_WEAR = cfg.comment("Lock pick durability a wrong pin costs on this lock (1-1000). JSON default: 12.").defineInRange("Pick Wear", -1, -1, 1000);
		cfg.pop();

		cfg.push("Netherite Lock");
		NETHERITE_LOCK_LENGTH = cfg.comment("Number of pins (1-20). JSON default: 14. Set to -1 to use default.").defineInRange("Length", -1, -1, 20);
		NETHERITE_LOCK_ENCHANT = cfg.comment("Enchantability (1-50). JSON default: 8.").defineInRange("Enchantment Value", -1, -1, 50);
		NETHERITE_LOCK_RESISTANCE = cfg.comment("Explosion resistance (0-1000). JSON default: 200.").defineInRange("Resistance", -1, -1, 1000);
		NETHERITE_LOCK_PICK_WEAR = cfg.comment("Lock pick durability a wrong pin costs on this lock (1-1000). JSON default: 24.").defineInRange("Pick Wear", -1, -1, 1000);
		cfg.pop();

		cfg.pop(); // Lock Stats

		// Lockpick Stats section
		cfg.comment("Override built-in lockpick stats without datapacks. Set any value to -1.0 to use the JSON default.",
				"Strength decides which locks a pick may attempt at all (the Complexity gate). Since 1.7.3 it no",
				"longer affects how fast a pick wears out - that is the lock's 'Pick Wear' above.",
				"Durability cannot be set here: it is fixed when the item is registered, before this file is read.",
				"Set it in config/locks/lockpick_types/<pick>.json instead.").push("Lockpick Stats");
		WOOD_PICK_STRENGTH = cfg.comment("Strength (0.01-10.0). JSON default: 0.2. Set to -1.0 to use default.").defineInRange("Wood Lockpick Strength", -1.0, -1.0, 10.0);
		COPPER_PICK_STRENGTH = cfg.comment("Strength (0.01-10.0). JSON default: 0.28.").defineInRange("Copper Lockpick Strength", -1.0, -1.0, 10.0);
		IRON_PICK_STRENGTH = cfg.comment("Strength (0.01-10.0). JSON default: 0.35.").defineInRange("Iron Lockpick Strength", -1.0, -1.0, 10.0);
		STEEL_PICK_STRENGTH = cfg.comment("Strength (0.01-10.0). JSON default: 0.7.").defineInRange("Steel Lockpick Strength", -1.0, -1.0, 10.0);
		GOLD_PICK_STRENGTH = cfg.comment("Strength (0.01-10.0). JSON default: 0.25.").defineInRange("Gold Lockpick Strength", -1.0, -1.0, 10.0);
		DIAMOND_PICK_STRENGTH = cfg.comment("Strength (0.01-10.0). JSON default: 0.85.").defineInRange("Diamond Lockpick Strength", -1.0, -1.0, 10.0);
		NETHERITE_PICK_STRENGTH = cfg.comment("Strength (0.01-10.0). JSON default: 0.95.").defineInRange("Netherite Lockpick Strength", -1.0, -1.0, 10.0);
		cfg.pop(); // Lockpick Stats

		// Loot-Scaled Locks section
		cfg.comment("When enabled, lock tier is determined by the value of a chest's loot table contents instead of random weighted selection. Chests below the minimum value threshold get no lock at all.").push("Loot-Scaled Locks");
		LOOT_SCALED_LOCKS = cfg
			.comment("Enable loot-value-based lock tier selection. When disabled, the random weighted system (Generated Locks + Generated Lock Chances) is used instead")
			.define("Enable Loot-Scaled Locks", true);
		DEFAULT_ITEM_VALUE = cfg
			.comment("Base value assigned to each item before multipliers")
			.defineInRange("Default Item Value", 1.0d, 0.01d, 1000d);
		ENCHANT_VALUE_BONUS = cfg
			.comment("Value multiplier added per enchantment level on an item (e.g. 0.25 means Sharpness V adds 1.25x)")
			.defineInRange("Enchantment Value Bonus", 0.25d, 0d, 10d);
		RARITY_COMMON_MULT = cfg
			.comment("Value multiplier for COMMON rarity items")
			.defineInRange("Common Rarity Multiplier", 1.0d, 0.01d, 100d);
		RARITY_UNCOMMON_MULT = cfg
			.comment("Value multiplier for UNCOMMON rarity items")
			.defineInRange("Uncommon Rarity Multiplier", 1.5d, 0.01d, 100d);
		RARITY_RARE_MULT = cfg
			.comment("Value multiplier for RARE rarity items")
			.defineInRange("Rare Rarity Multiplier", 2.5d, 0.01d, 100d);
		RARITY_EPIC_MULT = cfg
			.comment("Value multiplier for EPIC rarity items")
			.defineInRange("Epic Rarity Multiplier", 5.0d, 0.01d, 100d);
		LOOT_VALUE_TIERS = cfg
			.comment("Minimum loot value thresholds for each lock in the 'Generated Locks' list (same order).",
				"WARNING: THE AMOUNT OF NUMBERS SHOULD BE EQUAL TO THE AMOUNT OF GENERATED LOCK ITEMS!!!",
				"A chest's total loot value is compared against these thresholds and it receives the highest tier it qualifies for.",
				"If the value is below the lowest threshold, NO lock is generated for that chest.",
				"NOTE: these defaults were placed against the measured loot-value distribution of vanilla 1.21.1",
				"(56 chest tables: min 0.0, p25 4.9, median 9.8, p75 15.9, p90 22.2, max 50.4). Re-space these against",
				"your own pack's economy when you add or remove a tier.",
				"The netherite value MUST stay at or below ~22: 'chests/end_city_treasure' (22.2) is the only",
				"End chest table, and the netherite lock is End-gated, so a higher threshold makes the netherite",
				"lock impossible to generate anywhere.",
				"The diamond value is the Overworld's effective TOP tier: a chest that would have earned netherite",
				"falls back to diamond outside the End, so EVERY chest worth at least the diamond value gets a",
				"diamond lock in the Overworld. Lowering it is what made diamond locks far too common; 20 leaves only",
				"the genuinely rich tables (ruined portal, desert pyramid, shipwreck, ancient city, bastion, buried",
				"treasure) while dungeons and mineshafts drop to gold.",
				"Defaults: wood=2, copper=5, iron=9, gold=13, diamond=20, netherite=21.",
				"Example - make diamond (or better) locks exclusive to high-value chests: raise the diamond/netherite",
				"thresholds (e.g. 25 and 35) so only the richest loot tables reach them, and raise the lowest threshold",
				"to skip locks on poor chests. With [10, 20, 35, 50, 65, 80, 140] a chest worth 90 gets diamond and one",
				"worth 150 gets netherite, while a chest worth 8 gets no lock at all.")
			.defineList("Loot Value Tiers", Lists.newArrayList(2.0, 5.0, 9.0, 13.0, 20.0, 21.0), e -> e instanceof Double);
		ITEM_VALUE_OVERRIDES = cfg
			.comment("Override the base value for specific items. Format: 'modid:item_name=value'.",
				"Items not listed here use the Default Item Value.",
				"Useful for materials like diamonds or emeralds that have COMMON rarity but are inherently valuable.")
			.defineList("Item Value Overrides", Lists.newArrayList(
				"minecraft:diamond=5.0",
				"minecraft:emerald=4.0",
				"minecraft:netherite_ingot=10.0",
				"minecraft:netherite_scrap=6.0",
				"minecraft:enchanted_golden_apple=15.0",
				"minecraft:totem_of_undying=12.0",
				"minecraft:elytra=20.0",
				"minecraft:heart_of_the_sea=8.0",
				"minecraft:golden_apple=3.0",
				"minecraft:ender_pearl=2.0",
				"minecraft:experience_bottle=2.0",
				"minecraft:saddle=3.0",
				"minecraft:name_tag=3.0",
				"minecraft:iron_ingot=2.0",
				"minecraft:gold_ingot=3.0"
			), e -> e instanceof String && ((String) e).contains("="));
		cfg.pop(); // Loot-Scaled Locks

		SPEC = cfg.build();
	}

	private LocksConfig() {}

	// https://gist.github.com/raws/1667807
	public static void init()
	{
		weightedGeneratedLocks = new TreeMap<>();
		weightTotal = 0;
		ungatedWeightedGeneratedLocks = new TreeMap<>();
		ungatedWeightTotal = 0;
		noSteelWeightedGeneratedLocks = new TreeMap<>();
		noSteelWeightTotal = 0;
		noSteelUngatedWeightedGeneratedLocks = new TreeMap<>();
		noSteelUngatedWeightTotal = 0;
		List<? extends String> locks = GENERATED_LOCKS.get();
		List<? extends Integer> weights = GENERATED_LOCK_WEIGHTS.get();
		List<? extends Double> chances = LOCK_TIER_CHANCES.get();
		if (chances.size() != locks.size())
			Locks.LOGGER.warn("Lock Tier Chances list size ({}) doesn't match Generated Locks list size ({}). Missing entries default to a certain tier.", chances.size(), locks.size());
		Map<Item, Double> parsedChances = new HashMap<>();
		for (int a = 0; a < Math.min(locks.size(), chances.size()); ++a)
		{
			double chance = Math.clamp(chances.get(a), 0.0, 1.0);
			parsedChances.put(BuiltInRegistries.ITEM.get(ResourceLocation.parse(locks.get(a))), chance);
		}
		tierChances = Map.copyOf(parsedChances);
		if (weights.size() != locks.size())
			Locks.LOGGER.warn("Generated Lock Chances list size ({}) doesn't match Generated Locks list size ({}). Some locks may not appear.", weights.size(), locks.size());
		int blockedSteelLocks = 0;
		for(int a = 0; a < Math.min(locks.size(), weights.size()); ++a)
		{
			Item lock = BuiltInRegistries.ITEM.get(ResourceLocation.parse(locks.get(a)));
			weightTotal += weights.get(a);
			weightedGeneratedLocks.put(weightTotal, lock);
			if(!biomeGatedLocks.contains(lock))
			{
				ungatedWeightTotal += weights.get(a);
				ungatedWeightedGeneratedLocks.put(ungatedWeightTotal, lock);
			}
			// Hard gate: a steel-provider lock never reaches a chest while c:ingots/steel is empty, whatever the
			// config says. A locks-common.toml written before the steel rework still lists locks:steel_lock.
			if (isSteelGated(lock))
			{
				if (!steelAvailable)
					blockedSteelLocks++;
				continue;
			}
			noSteelWeightTotal += weights.get(a);
			noSteelWeightedGeneratedLocks.put(noSteelWeightTotal, lock);
			if(!biomeGatedLocks.contains(lock))
			{
				noSteelUngatedWeightTotal += weights.get(a);
				noSteelUngatedWeightedGeneratedLocks.put(noSteelUngatedWeightTotal, lock);
			}
		}
		// Warned once per session: init() re-runs on every config load and every tag reload, and a server admin only needs
		// to be told once that a configured steel lock is currently being skipped.
		if (blockedSteelLocks > 0 && warnedAboutMissingSteel.compareAndSet(false, true))
			Locks.LOGGER.warn("{} lock(s) in 'Generated Locks' need a steel provider, but c:ingots/steel is empty — they are excluded from chest generation for now.", blockedSteelLocks);

		// Build loot value tier map
		lootValueTierMap = new TreeMap<>();
		ungatedLootValueTierMap = new TreeMap<>();
		noSteelLootValueTierMap = new TreeMap<>();
		noSteelUngatedLootValueTierMap = new TreeMap<>();
		List<? extends Double> tiers = LOOT_VALUE_TIERS.get();
		if (tiers.size() != locks.size())
			Locks.LOGGER.warn("Loot Value Tiers list size ({}) doesn't match Generated Locks list size ({}). Some lock tiers may be unreachable.", tiers.size(), locks.size());
		for(int a = 0; a < Math.min(locks.size(), tiers.size()); ++a)
		{
			Item lock = BuiltInRegistries.ITEM.get(ResourceLocation.parse(locks.get(a)));
			lootValueTierMap.put(tiers.get(a), lock);
			if(!biomeGatedLocks.contains(lock))
				ungatedLootValueTierMap.put(tiers.get(a), lock);
			if(!isSteelGated(lock))
			{
				noSteelLootValueTierMap.put(tiers.get(a), lock);
				if(!biomeGatedLocks.contains(lock))
					noSteelUngatedLootValueTierMap.put(tiers.get(a), lock);
			}
		}

		// Parse item value overrides
		itemValueOverrides = new HashMap<>();
		for (String entry : ITEM_VALUE_OVERRIDES.get())
		{
			int eq = entry.indexOf('=');
			if (eq <= 0 || eq >= entry.length() - 1)
			{
				Locks.LOGGER.warn("Invalid item value override entry: '{}'. Expected format: 'modid:item=value'", entry);
				continue;
			}
			try
			{
				ResourceLocation itemId = ResourceLocation.parse(entry.substring(0, eq));
				double value = Double.parseDouble(entry.substring(eq + 1));
				itemValueOverrides.put(itemId, value);
			}
			catch (Exception e)
			{
				Locks.LOGGER.warn("Failed to parse item value override '{}': {}", entry, e.getMessage());
			}
		}
	}

	public static boolean canEnchant(RandomSource rng)
	{
		return LocksUtil.chance(rng, GENERATION_ENCHANT_CHANCE.get());
	}

	/**
	 * Snapshots the tags the generation tables are built from — {@link LocksItemTags#BIOME_GATED_LOCKS},
	 * {@link LocksItemTags#STEEL_PROVIDER_LOCKS} and whether {@code c:ingots/steel} has any member at all. Called
	 * on every tag load: a datapack reload changes all three without touching the config, so the tables have to be
	 * rebuilt from {@link #init()} whenever it does.
	 */
	public static void refreshGenerationTags(RegistryAccess registryAccess)
	{
		var itemRegistry = registryAccess.registry(Registries.ITEM);
		biomeGatedLocks = itemRegistry
			.flatMap(registry -> registry.getTag(LocksItemTags.BIOME_GATED_LOCKS))
			.map(holderSet -> {
				Set<Item> gated = new HashSet<>();
				for (var holder : holderSet)
					gated.add(holder.value());
				return Set.copyOf(gated);
			})
			.orElse(Set.of());
		steelProviderLocks = itemRegistry
			.flatMap(registry -> registry.getTag(LocksItemTags.STEEL_PROVIDER_LOCKS))
			.map(holderSet -> {
				Set<Item> gated = new HashSet<>();
				for (var holder : holderSet)
					gated.add(holder.value());
				return Set.copyOf(gated);
			})
			.orElse(Set.of());
		steelAvailable = itemRegistry
			.flatMap(registry -> registry.getTag(ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots/steel"))))
			.map(holderSet -> holderSet.size() > 0)
			.orElse(false);
		if (SPEC.isLoaded())
			init();
	}

	/** True if this lock may only be generated in {@link LocksBiomeTags#BIOME_GATED_LOCK_BIOMES}. */
	public static boolean isBiomeGated(Item item)
	{
		return biomeGatedLocks.contains(item);
	}

	/**
	 * True if this lock needs a steel provider. Locks in {@link LocksItemTags#STEEL_PROVIDER_LOCKS} are never
	 * generated while {@code c:ingots/steel} is empty.
	 */
	public static boolean isSteelGated(Item item)
	{
		return steelProviderLocks.contains(item);
	}

	/**
	 * Returns a lock ItemStack based on the total loot value, or {@link ItemStack#EMPTY} if the value
	 * is below all thresholds (meaning no lock should be generated for this chest).
	 */
	public static ItemStack getLockForLootValue(double lootValue, RandomSource rng)
	{
		return getLockForLootValue(lootValue, rng, true);
	}

	/** The configured keep-chance for this tier; certain if the list is too short or holds a stale entry. */
	private static double tierChance(Item lock)
	{
		Double chance = tierChances.get(lock);
		return chance == null ? 1.0 : chance;
	}

	/**
	 * @param biomeGatedAllowed whether the chest's biome is in {@link LocksBiomeTags#BIOME_GATED_LOCK_BIOMES}. When
	 *                         false, gated locks are skipped entirely and the chest drops to the next-best tier
	 *                         instead of falling back to no lock at all.
	 */
	public static ItemStack getLockForLootValue(double lootValue, RandomSource rng, boolean biomeGatedAllowed)
	{
		NavigableMap<Double, Item> tiers = !steelAvailable
			? (biomeGatedAllowed ? noSteelLootValueTierMap : noSteelUngatedLootValueTierMap)
			: (biomeGatedAllowed ? lootValueTierMap : ungatedLootValueTierMap);
		// Start at the highest tier the chest qualifies for, then step down on a failed roll. A chest that reaches
		// diamond therefore gets "diamond is possible" rather than "diamond, guaranteed": the strict highest-fit rule
		// made every chest above a threshold come out identical, so one busy band pushed everything above it into the
		// same tier. Richer chests still skew high, because a chest only ever rolls tiers it actually qualifies for.
		var entry = tiers.floorEntry(lootValue);
		while (entry != null)
		{
			Item lock = entry.getValue();
			double chance = tierChance(lock);
			if (chance >= 1.0 || LocksUtil.chance(rng, chance))
				return new ItemStack(lock);
			entry = tiers.lowerEntry(entry.getKey());
		}
		return ItemStack.EMPTY;
	}

	public static ItemStack getRandomLock(RandomSource rng)
	{
		return getRandomLock(rng, true);
	}

	/**
	 * @param biomeGatedAllowed see {@link #getLockForLootValue(double, RandomSource, boolean)}. The draw is scaled to
	 *                         the ungated weight total, so gated locks neither appear nor steal weight from the rest.
	 */
	public static ItemStack getRandomLock(RandomSource rng, boolean biomeGatedAllowed)
	{
		NavigableMap<Integer, Item> table = !steelAvailable
			? (biomeGatedAllowed ? noSteelWeightedGeneratedLocks : noSteelUngatedWeightedGeneratedLocks)
			: (biomeGatedAllowed ? weightedGeneratedLocks : ungatedWeightedGeneratedLocks);
		int total = !steelAvailable
			? (biomeGatedAllowed ? noSteelWeightTotal : noSteelUngatedWeightTotal)
			: (biomeGatedAllowed ? weightTotal : ungatedWeightTotal);
		if (total <= 0 || table.isEmpty())
			return ItemStack.EMPTY;
		var entry = table.ceilingEntry(rng.nextInt(total) + 1);
		if (entry == null)
			return ItemStack.EMPTY;
		ItemStack stack = new ItemStack(entry.getValue());
		return stack;
	}

	public static void applyConfigStatOverrides()
	{
		if (!SPEC.isLoaded())
			return;

		applyLockConfigOverride("wood_lock", WOOD_LOCK_LENGTH, WOOD_LOCK_ENCHANT, WOOD_LOCK_RESISTANCE, WOOD_LOCK_PICK_WEAR);
		applyLockConfigOverride("copper_lock", COPPER_LOCK_LENGTH, COPPER_LOCK_ENCHANT, COPPER_LOCK_RESISTANCE, COPPER_LOCK_PICK_WEAR);
		applyLockConfigOverride("iron_lock", IRON_LOCK_LENGTH, IRON_LOCK_ENCHANT, IRON_LOCK_RESISTANCE, IRON_LOCK_PICK_WEAR);
		applyLockConfigOverride("steel_lock", STEEL_LOCK_LENGTH, STEEL_LOCK_ENCHANT, STEEL_LOCK_RESISTANCE, STEEL_LOCK_PICK_WEAR);
		applyLockConfigOverride("gold_lock", GOLD_LOCK_LENGTH, GOLD_LOCK_ENCHANT, GOLD_LOCK_RESISTANCE, GOLD_LOCK_PICK_WEAR);
		applyLockConfigOverride("diamond_lock", DIAMOND_LOCK_LENGTH, DIAMOND_LOCK_ENCHANT, DIAMOND_LOCK_RESISTANCE, DIAMOND_LOCK_PICK_WEAR);
		applyLockConfigOverride("netherite_lock", NETHERITE_LOCK_LENGTH, NETHERITE_LOCK_ENCHANT, NETHERITE_LOCK_RESISTANCE, NETHERITE_LOCK_PICK_WEAR);

		applyPickConfigOverride("wood_lock_pick", WOOD_PICK_STRENGTH);
		applyPickConfigOverride("copper_lock_pick", COPPER_PICK_STRENGTH);
		applyPickConfigOverride("iron_lock_pick", IRON_PICK_STRENGTH);
		applyPickConfigOverride("steel_lock_pick", STEEL_PICK_STRENGTH);
		applyPickConfigOverride("gold_lock_pick", GOLD_PICK_STRENGTH);
		applyPickConfigOverride("diamond_lock_pick", DIAMOND_PICK_STRENGTH);
		applyPickConfigOverride("netherite_lock_pick", NETHERITE_PICK_STRENGTH);
	}

	private static void applyLockConfigOverride(String name, ModConfigSpec.IntValue length, ModConfigSpec.IntValue enchant, ModConfigSpec.IntValue resistance, ModConfigSpec.IntValue pickWear)
	{
		int l = length.get();
		int e = enchant.get();
		int r = resistance.get();
		int w = pickWear.get();
		if (l >= 0 || e >= 0 || r >= 0 || w >= 0)
		{
			ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Locks.ID, name);
			LockTypeRegistry.applyConfigLockOverride(id, l, e, r, w);
		}
	}

	private static void applyPickConfigOverride(String name, ModConfigSpec.DoubleValue strength)
	{
		double s = strength.get();
		if (s >= 0)
		{
			ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Locks.ID, name);
			LockTypeRegistry.applyConfigPickOverride(id, (float) s);
		}
	}
}
