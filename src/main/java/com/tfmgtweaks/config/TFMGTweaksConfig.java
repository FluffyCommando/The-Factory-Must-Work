package com.tfmgtweaks.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Config for the new "Oil Rock" feature (see OilRockBlock/OilRockBlockEntity
 * and the worldgen classes in com.tfmgtweaks.worldgen).
 *
 * - oilRockReplacesOldOilNodes: when true (default), TFMG's own
 *   bedrock-locked oil_well/oil_deposit worldgen features are suppressed
 *   (via OilNodeConfigFilter, a placement modifier overriding their placed
 *   feature JSON) so Oil Rock is the only way to find oil. When false,
 *   both systems spawn side by side.
 * - oilRockFiniteReserves: when true (default), an Oil Rock deposit (a
 *   whole connected cluster, not a single block -- see
 *   OilRockBlockEntity's controller/member pattern) has a limited amount
 *   of oil (oilRockReserves) that depletes as pump jacks extract from it,
 *   giving fracking (which speeds up extraction) an actual tradeoff to
 *   make. When false, reserves never deplete, matching how TFMG's own oil
 *   deposits already behave by default.
 * - spawnChance / minHeight / maxHeight: control how often Oil Rock
 *   clusters attempt to spawn and what Y range they can start in. Read at
 *   runtime by OilRockRarityFilter/OilRockHeightRangePlacement rather than
 *   baked into the placed_feature JSON, specifically so they're adjustable
 *   via config without needing a resource pack.
 *
 * Fracking uses a dedicated Steam fluid (see com.tfmgtweaks.content.fluid),
 * piped into a pump jack's own dedicated Steam tank and consumed from
 * there over time (see PumpjackFrackingWrapper) rather than TFMG's Hot
 * Air or plain Water.
 */
public class TFMGTweaksConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue OIL_ROCK_REPLACES_OLD_OIL_NODES;
    public static final ModConfigSpec.BooleanValue OIL_ROCK_FINITE_RESERVES;
    public static final ModConfigSpec.IntValue OIL_ROCK_RESERVES;
    public static final ModConfigSpec.IntValue OIL_ROCK_FLUID_TO_FULLY_CRACK;
    public static final ModConfigSpec.IntValue OIL_ROCK_FRACKING_DECAY_RATE;
    public static final ModConfigSpec.DoubleValue OIL_ROCK_CRACKED_EXTRACTION_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue OIL_ROCK_REQUIRE_CRACKED_TO_EXTRACT;
    public static final ModConfigSpec.DoubleValue OIL_ROCK_BASE_EXTRACTION_MULTIPLIER;
    public static final ModConfigSpec.IntValue OIL_ROCK_SPAWN_CHANCE;
    public static final ModConfigSpec.IntValue OIL_ROCK_MIN_HEIGHT;
    public static final ModConfigSpec.IntValue OIL_ROCK_MAX_HEIGHT;
    public static final ModConfigSpec.IntValue PUMPJACK_WASTE_WATER_CAPACITY;
    public static final ModConfigSpec.IntValue PUMPJACK_STEAM_TANK_CAPACITY;
    public static final ModConfigSpec.IntValue PUMPJACK_STEAM_PROCESSING_RATE_PERCENT;
    public static final ModConfigSpec.IntValue OIL_ROCK_MAX_NEARBY_DEPOSITS;
    public static final ModConfigSpec.BooleanValue OIL_ROCK_MIGRATE_OLD_DEPOSITS;
    public static final ModConfigSpec.BooleanValue OIL_ROCK_MIGRATE_SCAN_FULL_HEIGHT;
    public static final ModConfigSpec.IntValue BOILER_TANK_CAPACITY;
    public static final ModConfigSpec.IntValue BOILER_STEAM_PER_HEAT_PER_TICK;
    public static final ModConfigSpec.IntValue BOILER_MAX_HEIGHT;
    public static final ModConfigSpec.IntValue SURFACE_SCANNER_RESCAN_INTERVAL_TICKS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("oil_rock");

        OIL_ROCK_REPLACES_OLD_OIL_NODES = builder
                .comment("If true, Oil Rock replaces TFMG's own bedrock-locked oil wells/deposits entirely.",
                        "If false, both spawn side by side.")
                .define("replacesOldOilNodes", true);

        OIL_ROCK_FINITE_RESERVES = builder
                .comment("If true, an Oil Rock's oil reserves are finite and deplete with extraction.",
                        "If false, reserves never run out (matching TFMG's own default oil deposit behavior).")
                .define("finiteReserves", true);

        OIL_ROCK_RESERVES = builder
                .comment("How much oil (in the same units pump jacks extract per tick) a single Oil Rock holds,",
                        "when finiteReserves is true.")
                .defineInRange("reserves", 3_000_000, 1, Integer.MAX_VALUE);

        OIL_ROCK_FLUID_TO_FULLY_CRACK = builder
                .comment("Total mB of Steam-derived fracking progress an Oil Rock deposit needs to be",
                        "considered cracked. No longer permanent once reached -- see fractingDecayRate below;",
                        "continued Steam input is needed to stay above this threshold.")
                .defineInRange("fluidToFullyCrack", 10_000, 1, Integer.MAX_VALUE);

        OIL_ROCK_FRACKING_DECAY_RATE = builder
                .comment("How much fracking progress (mB) an Oil Rock deposit loses per tick when not being",
                        "actively fed enough Steam to offset it. Being cracked is no longer a permanent,",
                        "one-time unlock -- progress decays continuously, so a pump jack must keep receiving",
                        "Steam fast enough to outpace this decay to stay above fluidToFullyCrack and keep the",
                        "extraction bonus. Default of 5/tick fully decays the default 10,000 threshold in",
                        "about 100 seconds of no Steam input at all.")
                .defineInRange("frackingDecayRate", 5, 0, Integer.MAX_VALUE);

        OIL_ROCK_CRACKED_EXTRACTION_MULTIPLIER = builder
                .comment("Extraction rate multiplier applied once an Oil Rock is fully cracked, on top of",
                        "baseExtractionMultiplier below. e.g. 2.0 means pump jacks extract twice as fast",
                        "from a fully-cracked rock as they would from that same rock uncracked.")
                .defineInRange("crackedExtractionMultiplier", 2.0, 1.0, 10.0);

        OIL_ROCK_REQUIRE_CRACKED_TO_EXTRACT = builder
                .comment("If true, a pump jack extracts nothing at all from an Oil Rock deposit until it's",
                        "been fully cracked via fracking -- extraction isn't just slower before that, it's",
                        "zero. If false (default), extraction works before cracking too, cracking just",
                        "speeds it up via crackedExtractionMultiplier above.")
                .define("requireCrackedToExtract", false);

        OIL_ROCK_BASE_EXTRACTION_MULTIPLIER = builder
                .comment("General multiplier applied to a pump jack's extraction rate from Oil Rock",
                        "specifically, independent of cracked state -- lets you tune Oil Rock's overall",
                        "extraction speed (e.g. relative to TFMG's own oil deposits) without touching the",
                        "cracked bonus itself.")
                .defineInRange("baseExtractionMultiplier", 1.0, 0.1, 10.0);

        OIL_ROCK_SPAWN_CHANCE = builder
                .comment("1-in-N chance for an Oil Rock cluster to attempt spawning per chunk.",
                        "Lower = more common. Matches vanilla's own rarity_filter convention.")
                .defineInRange("spawnChance", 60, 1, Integer.MAX_VALUE);

        OIL_ROCK_MIN_HEIGHT = builder
                .comment("Lowest Y level Oil Rock clusters can start spawning at.")
                .defineInRange("minHeight", -16, -64, 320);

        OIL_ROCK_MAX_HEIGHT = builder
                .comment("Highest Y level Oil Rock clusters can start spawning at.")
                .defineInRange("maxHeight", 60, -64, 320);

        PUMPJACK_WASTE_WATER_CAPACITY = builder
                .comment("How much waste water (mB) a pump jack's internal buffer can hold before it needs",
                        "to be drained. Fracking stops accepting more fluid once this fills up.")
                .defineInRange("pumpjackWasteWaterCapacity", 4_000, 100, Integer.MAX_VALUE);

        PUMPJACK_STEAM_TANK_CAPACITY = builder
                .comment("How much Steam (mB) a pump jack's dedicated internal input buffer can hold. Steam",
                        "piped in fills this tank like any real fluid tank, and is drained from it into",
                        "fracking progress + waste over time rather than being consumed instantly on arrival.")
                .defineInRange("pumpjackSteamTankCapacity", 4_000, 100, Integer.MAX_VALUE);

        PUMPJACK_STEAM_PROCESSING_RATE_PERCENT = builder
                .comment("What percentage of the Steam tank's own capacity (pumpjackSteamTankCapacity) can be",
                        "converted into fracking progress + waste per tick, at most. Previously uncapped --",
                        "a full tank would drain in a single tick regardless of size, converting the instant",
                        "Steam arrived. Lower values spread that out over more time instead; e.g. 25 (the",
                        "default) means a full tank takes roughly 4 ticks to fully process rather than 1 --",
                        "lower this further for a more gradual, visibly-draining rate.")
                .defineInRange("pumpjackSteamProcessingRatePercent", 25, 1, 100);

        OIL_ROCK_MAX_NEARBY_DEPOSITS = builder
                .comment("Maximum number of additional, independent Oil Rock deposits that can spawn near",
                        "each primary deposit (each is its own separate cluster with its own reserves, just",
                        "co-located) -- when above 0, at least 1 always spawns (count is randomized between",
                        "1 and this value), so there's reliably a small group to extract from rather than",
                        "just one. Set to 0 to disable entirely.")
                .defineInRange("maxNearbyDeposits", 6, 0, 10);

        OIL_ROCK_MIGRATE_OLD_DEPOSITS = builder
                .comment("If true (and replacesOldOilNodes is also true), TFMG's own bedrock-locked oil_well/",
                        "oil_deposit markers found in already-generated chunks (e.g. from a world you're adding",
                        "this mod to partway through) get replaced with a new Oil Rock deposit nearby the first",
                        "time that chunk loads -- since normal worldgen only ever applies to newly-generated",
                        "chunks, this is the only way to retrofit existing terrain. Has no effect on chunks",
                        "that haven't generated yet; those already get Oil Rock normally.")
                .define("migrateOldDeposits", true);

        OIL_ROCK_MIGRATE_SCAN_FULL_HEIGHT = builder
                .comment("TFMG's own oil_well/oil_deposit are hardcoded to only ever generate at Y=-64, so",
                        "migrateOldDeposits normally only checks that one Y level per column -- cheap, since",
                        "it's a single block check per column instead of the whole chunk height. Turn this on",
                        "only if you're migrating a world where a DIFFERENT mod's worldgen override let old",
                        "deposits spawn at other heights too (e.g. a sky world generator) -- every Y level in",
                        "every loaded column gets checked instead, which is meaningfully more expensive per",
                        "chunk load and runs for as long as this stays on. Meant to be turned on temporarily,",
                        "swept through the relevant chunks once (flying/exploring so they actually load), then",
                        "turned back off -- not left on permanently.")
                .define("migrateScanFullHeight", false);

        builder.pop();

        builder.push("boiler");

        BOILER_TANK_CAPACITY = builder
                .comment("Capacity (mB) of the Boiler's water input tank and Steam output buffer (each sized",
                        "the same).")
                .defineInRange("tankCapacity", 4_000, 100, Integer.MAX_VALUE);

        BOILER_STEAM_PER_HEAT_PER_TICK = builder
                .comment("mB of Steam produced per tick, per point of heat level (matching TFMG's own Vat",
                        "heat scale -- a regular firebox reaches heat level 1-2, a hotter/multiple heat",
                        "sources scale higher), while water is available. Water is consumed at half the rate",
                        "of Steam produced -- 1000 mB water yields 2000 mB Steam -- so this figure is a",
                        "Steam production rate, not a water consumption rate. Scales linearly with the",
                        "boiler's stacked height -- this is the rate for a single block.")
                .defineInRange("steamPerHeatPerTick", 25, 1, 1000);

        BOILER_MAX_HEIGHT = builder
                .comment("Maximum number of Boiler blocks that can be stacked vertically into one",
                        "multiblock. Both tank capacity and Steam production rate scale linearly with",
                        "height -- a stack of 4 holds 4x the fluid and produces Steam 4x as fast as a",
                        "single block.")
                .defineInRange("maxHeight", 8, 1, 64);

        builder.pop();

        builder.push("surface_scanner");

        SURFACE_SCANNER_RESCAN_INTERVAL_TICKS = builder
                .comment("How often (in ticks, 20 = 1 second) a Surface Scanner re-scans for deposits while",
                        "running. TFMG's own default behavior re-scans every second (its lazy tick rate) --",
                        "since the scan itself runs client-side, that showed up as a visible stutter every",
                        "second. Default here is 2400 (2 minutes). Regardless of this setting, the scanner",
                        "always re-scans immediately if its actual position has changed since the last scan",
                        "(e.g. moved via a Sable physics object), rather than waiting out the interval.")
                .defineInRange("rescanIntervalTicks", 2400, 1, Integer.MAX_VALUE);

        builder.pop();

        SPEC = builder.build();
    }
}
