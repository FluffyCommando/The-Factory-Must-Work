package com.tfmgtweaks.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class TFMGTweaksConfig {

    public static final ModConfigSpec SPEC;

    // oil_rock
    public static final ModConfigSpec.IntValue OIL_ROCK_SPAWN_CHANCE;
    public static final ModConfigSpec.IntValue OIL_ROCK_MAX_NEARBY_DEPOSITS;
    public static final ModConfigSpec.IntValue OIL_ROCK_MIN_HEIGHT;
    public static final ModConfigSpec.IntValue OIL_ROCK_MAX_HEIGHT;   
    public static final ModConfigSpec.BooleanValue OIL_ROCK_REPLACES_OLD_OIL_NODES;
    public static final ModConfigSpec.BooleanValue OIL_ROCK_MIGRATE_OLD_DEPOSITS;
    public static final ModConfigSpec.BooleanValue OIL_ROCK_MIGRATE_SCAN_FULL_HEIGHT;    
    public static final ModConfigSpec.BooleanValue OIL_ROCK_FINITE_RESERVES;
    public static final ModConfigSpec.IntValue OIL_ROCK_RESERVES;
    public static final ModConfigSpec.IntValue OIL_ROCK_FLUID_TO_FULLY_CRACK;
    public static final ModConfigSpec.IntValue OIL_ROCK_FRACKING_DECAY_RATE;
    public static final ModConfigSpec.IntValue OIL_ROCK_FRACKING_DECAY_GRACE_TICKS;
    public static final ModConfigSpec.BooleanValue OIL_ROCK_REQUIRE_CRACKED_TO_EXTRACT;
    public static final ModConfigSpec.DoubleValue OIL_ROCK_BASE_EXTRACTION_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue OIL_ROCK_CRACKED_EXTRACTION_MULTIPLIER;
    public static final ModConfigSpec.IntValue PUMPJACK_WASTE_WATER_CAPACITY;
    public static final ModConfigSpec.IntValue PUMPJACK_STEAM_TANK_CAPACITY;
    public static final ModConfigSpec.IntValue PUMPJACK_STEAM_PROCESSING_RATE_PERCENT;
    


    // surface_scanner
    public static final ModConfigSpec.IntValue SURFACE_SCANNER_RESCAN_INTERVAL_TICKS;

    // oil_hammer
    public static final ModConfigSpec.BooleanValue OIL_HAMMER_DEBUG_MODE;

    // fuel_explosions
    public static final ModConfigSpec.BooleanValue FUEL_EXPLOSIONS_ENABLED;
    public static final ModConfigSpec.BooleanValue FUEL_EXPLOSIONS_REQUIRE_HARD_DIFFICULTY;
    public static final ModConfigSpec.IntValue FUEL_EXPLOSIONS_MIN_AMOUNT_MB;
    public static final ModConfigSpec.DoubleValue FUEL_EXPLOSIONS_POWER_PER_BUCKET;
    public static final ModConfigSpec.DoubleValue FUEL_EXPLOSIONS_MAX_POWER;
    public static final ModConfigSpec.IntValue FUEL_EXPLOSIONS_FIRE_RADIUS;
    public static final ModConfigSpec.DoubleValue FUEL_EXPLOSIONS_FIRE_CHANCE;
    public static final ModConfigSpec.IntValue FUEL_EXPLOSIONS_FALLING_FIRE_COUNT;
    public static final ModConfigSpec.DoubleValue FUEL_EXPLOSIONS_FALLING_DRAG;
    public static final ModConfigSpec.DoubleValue FUEL_EXPLOSIONS_FALLING_FLUID_DRAG;
    public static final ModConfigSpec.IntValue FUEL_EXPLOSIONS_MAX_FLUID_SPILL_COUNT;
    public static final ModConfigSpec.IntValue FUEL_EXPLOSIONS_SECONDARY_COUNT;
    public static final ModConfigSpec.IntValue FUEL_EXPLOSIONS_SECONDARY_RADIUS;
    public static final ModConfigSpec.DoubleValue FUEL_EXPLOSIONS_SECONDARY_POWER;
    public static final ModConfigSpec.IntValue FUEL_EXPLOSIONS_SECONDARY_MAX_DELAY_TICKS;
    public static final ModConfigSpec.IntValue FUEL_EXPLOSIONS_SECONDARY_FALLING_FIRE_COUNT;
    public static final ModConfigSpec.BooleanValue FUEL_EXPLOSIONS_SURROUNDING_BLOCKS_DAMAGED;
    public static final ModConfigSpec.IntValue FUEL_EXPLOSIONS_CHAIN_RADIUS;

    // fluid_ignition
    public static final ModConfigSpec.BooleanValue FLUID_IGNITION_ENABLED;
    public static final ModConfigSpec.IntValue FLUID_IGNITION_MAX_SPREAD;
    public static final ModConfigSpec.IntValue FLUID_IGNITION_SPREAD_PER_TICK;
    public static final ModConfigSpec.IntValue FLUID_IGNITION_PARTICLES_PER_TICK;
    public static final ModConfigSpec.IntValue FLUID_IGNITION_REMOVAL_PER_TICK;
    public static final ModConfigSpec.IntValue FLUID_IGNITION_SOURCE_SUPPORT_RADIUS;
    public static final ModConfigSpec.IntValue FLUID_IGNITION_SUPPORT_GRACE_PERIOD;
    public static final ModConfigSpec.IntValue FLUID_IGNITION_PROACTIVE_CHECK_PER_TICK;

    // air_intake_pollution_cleaning
    public static final ModConfigSpec.BooleanValue AIR_INTAKE_POLLUTION_CLEANING_ENABLED;
    public static final ModConfigSpec.DoubleValue AIR_INTAKE_POLLUTION_RATE_1X1;
    public static final ModConfigSpec.DoubleValue AIR_INTAKE_POLLUTION_RATE_2X2;
    public static final ModConfigSpec.DoubleValue AIR_INTAKE_POLLUTION_RATE_3X3;
    public static final ModConfigSpec.IntValue AIR_INTAKE_POLLUTION_SPEED_BONUS_BASELINE_RPM;
    public static final ModConfigSpec.IntValue AIR_INTAKE_GAS_PRODUCTION_HEIGHT_Y;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("oil_rock");

        OIL_ROCK_SPAWN_CHANCE = builder
                .comment("1-in-N chance for an Oil Rock cluster to attempt spawning per chunk.",
                        "Lower = more common.")
                .defineInRange("spawnChance", 20, 1, Integer.MAX_VALUE);

        OIL_ROCK_MAX_NEARBY_DEPOSITS = builder
                .comment("Maximum number of additional Oil Rock deposits that can spawn near a primary deposit")
                .defineInRange("maxNearbyDeposits", 6, 0, 10);

        OIL_ROCK_MIN_HEIGHT = builder
                .comment("Lowest Y level Oil Rock clusters can start spawning at.")
                .defineInRange("minHeight", -32, -64, 320);

        OIL_ROCK_MAX_HEIGHT = builder
                .comment("Highest Y level Oil Rock clusters can start spawning at.")
                .defineInRange("maxHeight", 60, -64, 320);

        OIL_ROCK_REPLACES_OLD_OIL_NODES = builder
                .comment("If true, Oil Rock replaces TFMG's own bedrock-locked oil deposits entirely.",
                        "If false, both spawn side by side.")
                .define("replacesOldOilNodes", true);

        OIL_ROCK_MIGRATE_OLD_DEPOSITS = builder
                .comment("If true, old, already generated TFMG oil deposits will be replaced with new oil rock deposits.")
                .define("migrateOldDeposits", false);

        OIL_ROCK_MIGRATE_SCAN_FULL_HEIGHT = builder
                .comment("If true, will scan every Y level instead of just scanning Y=-64 for old oil deposits.",
                        "Use this only if your old oil nodes spawned at any height.")
                .define("migrateScanFullHeight", false);

        OIL_ROCK_FINITE_RESERVES = builder
                .comment("If true, an Oil Rock's oil reserves are finite and deplete with extraction.",
                        "If false, reserves never run out.")
                .define("finiteReserves", true);

        OIL_ROCK_RESERVES = builder
                .comment("How much oil a single Oil Rock holds when finiteReserves is true.")
                .defineInRange("reserves", 10_000_000, 1, Integer.MAX_VALUE);

        OIL_ROCK_FLUID_TO_FULLY_CRACK = builder
                .comment("Total mB of Steam needed to crack an Oil Rock deposit for fracking to start.")
                .defineInRange("fluidToFullyCrack", 10_000, 1, Integer.MAX_VALUE);

        OIL_ROCK_FRACKING_DECAY_RATE = builder
                .comment("How much fracking progress an Oil deposit loses per tick.")
                .defineInRange("frackingDecayRate", 5, 0, Integer.MAX_VALUE);

        OIL_ROCK_FRACKING_DECAY_GRACE_TICKS = builder
                .comment("How many ticks with no Steam received before frackingDecayRate above starts")
                .defineInRange("frackingDecayGraceTicks", 60, 0, Integer.MAX_VALUE);

        OIL_ROCK_REQUIRE_CRACKED_TO_EXTRACT = builder
                .comment("If true, requires oil rock to be cracked from fracking to extract oil from it.")
                .define("requireCrackedToExtract", false);

        OIL_ROCK_BASE_EXTRACTION_MULTIPLIER = builder
                .comment("General multiplier applied to a pump jack's extraction rate before fracking")
                .defineInRange("baseExtractionMultiplier", 0.5, 0.1, 10.0);

        OIL_ROCK_CRACKED_EXTRACTION_MULTIPLIER = builder
                .comment("Extraction rate multiplier applied on top of base extraction when fracking.")
                .defineInRange("crackedExtractionMultiplier", 3.0, 1.0, 10.0);

        PUMPJACK_WASTE_WATER_CAPACITY = builder
                .comment("How much mB of waste water a pump jack can store internally.")
                .defineInRange("pumpjackWasteWaterCapacity", 4_000, 100, Integer.MAX_VALUE);

        PUMPJACK_STEAM_TANK_CAPACITY = builder
                .comment("How much mB of steam a pump jack can store internally.")
                .defineInRange("pumpjackSteamTankCapacity", 4_000, 100, Integer.MAX_VALUE);

        PUMPJACK_STEAM_PROCESSING_RATE_PERCENT = builder
                .comment("What percentage of the steam tank is converted into waste water per tick")
                .defineInRange("pumpjackSteamProcessingRatePercent", 5, 1, 100);

        builder.pop();

        builder.push("surface_scanner");

        SURFACE_SCANNER_RESCAN_INTERVAL_TICKS = builder
                .comment("How often a Surface Scanner re-scans for deposits",
                "Default = 2400 ticks")
                .defineInRange("rescanIntervalTicks", 2400, 1, Integer.MAX_VALUE);

        builder.pop();

        builder.push("oil_hammer");

        OIL_HAMMER_DEBUG_MODE = builder
                .comment("If true, the Oil Hammer's Oil Rock readout reports the exact coordinates",
                        "of the nearest Oil Rock instead of the normal vague \"traces of oil\" message.",
                        "Intended for debugging/development, not normal play.")
                .define("debugMode", false);

        builder.pop();

        builder.push("fuel_explosions");

        FUEL_EXPLOSIONS_ENABLED = builder
                .comment("If true, a tank filled with flammable fluid will explode when triggered by another explosion")
                .define("enabled", true);

        FUEL_EXPLOSIONS_REQUIRE_HARD_DIFFICULTY = builder
                .comment("If true, tanks will only explode on Hard difficulty.", 
                        "If false, it will explode on any difficutty.")
                .define("requireHardDifficulty", true);

        FUEL_EXPLOSIONS_MIN_AMOUNT_MB = builder
                .comment("How much mB of flammable fluid a tank needs stored to be able to explode")
                .defineInRange("minAmountMb", 500, 0, Integer.MAX_VALUE);

        FUEL_EXPLOSIONS_POWER_PER_BUCKET = builder
                .comment("Explosion power added per 1000 mB of flammable fluid")
                .defineInRange("powerPerBucket", 0.2, 0.01, 10.0);

        FUEL_EXPLOSIONS_MAX_POWER = builder
                .comment("Max power a tank explosion is capped at.")
                .defineInRange("maxPower", 64.0, 1.0, 128.0);

        FUEL_EXPLOSIONS_FIRE_RADIUS = builder
                .comment("Radius around the follow-up explosion to attempt to ignite.")
                .defineInRange("fireRadius", 6, 0, 16);

        FUEL_EXPLOSIONS_FIRE_CHANCE = builder
                .comment("Chance for each eligible position within fireRadius to catch fire.")
                .defineInRange("fireChance", 0.5, 0.0, 1.0);

        FUEL_EXPLOSIONS_FALLING_FIRE_COUNT = builder
                .comment("How many falling fire entitys will spawn on tank explosion.")
                .defineInRange("fallingFireCount", 24, 0, 128);

        FUEL_EXPLOSIONS_FALLING_DRAG = builder
                .comment("Horizontal-only velocity multiplier applied every tick to falling fire.")
                .defineInRange("fallingDrag", 0.9, 0.1, 1.0);

        FUEL_EXPLOSIONS_FALLING_FLUID_DRAG = builder
                .comment("Horizontal-only velocity multiplier applied every tick to falling fluid.")
                .defineInRange("fallingFluidDrag", 0.95, 0.1, 1.0);

        FUEL_EXPLOSIONS_MAX_FLUID_SPILL_COUNT = builder
                .comment("The max amount of falling fluid entitys that will spawn on tank explosion.")
                .defineInRange("maxFluidSpillCount", 8, 1, 64);

        FUEL_EXPLOSIONS_SECONDARY_COUNT = builder
                .comment("How many smaller secondary explosions go off at random spots near the main blast.")
                .defineInRange("secondaryExplosionCount", 6, 0, 32);

        FUEL_EXPLOSIONS_SECONDARY_RADIUS = builder
                .comment("Maximum horizontal and vertical distance a secondary explosion will spawn.")
                .defineInRange("secondaryExplosionRadius", 8, 1, 32);

        FUEL_EXPLOSIONS_SECONDARY_POWER = builder
                .comment("Explosion power of secondary blasts.")
                .defineInRange("secondaryExplosionPower", 1.5, 0.5, 32.0);

        FUEL_EXPLOSIONS_SECONDARY_MAX_DELAY_TICKS = builder
                .comment("The max amount of ticks a secondary explosion will wait before going off.")
                .defineInRange("secondaryExplosionMaxDelayTicks", 80, 0, 200);

        FUEL_EXPLOSIONS_SECONDARY_FALLING_FIRE_COUNT = builder
                .comment("How many falling fire debris entities spawn from each secondary explosion.")
                .defineInRange("secondaryExplosionFallingFireCount", 4, 0, 32);

        FUEL_EXPLOSIONS_SURROUNDING_BLOCKS_DAMAGED = builder
                .comment("If true, secondary explosions destroys nearby blocks.")
                .define("surroundingBlocksDamaged", true);

        FUEL_EXPLOSIONS_CHAIN_RADIUS = builder
                .comment("Max distance fluid tank can chain react with each other.")
                .defineInRange("chainExplosionRadius", 5, 0, 32);

        builder.pop();

        builder.push("fluid_ignition");

        FLUID_IGNITION_ENABLED = builder
                .comment("If true, flammable fluid will become ignited when lit on fire or exploded.")
                .define("enabled", true);

        FLUID_IGNITION_MAX_SPREAD = builder
                .comment("Maximum distance a ignition can spread across flammable fluid")
                .defineInRange("maxSpread", 128, 1, 512);

        FLUID_IGNITION_SPREAD_PER_TICK = builder
                .comment("How fast ignition will spread to nearby flammable fluid each tick",
                        "instantly covering it.")
                .defineInRange("spreadPerTick", 2, 1, 64);

        FLUID_IGNITION_PARTICLES_PER_TICK = builder
                .comment("How many client side particles will spawn on ignited fuel per tick")
                .defineInRange("particlesPerTick", 1, 0, 16);

        FLUID_IGNITION_REMOVAL_PER_TICK = builder
                .comment("How fast unsupported ignited fuel is removed per tick")
                .defineInRange("removalPerTick", 2, 1, 64);

        FLUID_IGNITION_SOURCE_SUPPORT_RADIUS = builder
                .comment("How far a non-source ignited fuel fragments can be from a source before it is removed")
                .defineInRange("sourceSupportRadius", 8, 1, 64);

        FLUID_IGNITION_SUPPORT_GRACE_PERIOD = builder
                .comment("How long a freshly ignited fragment will stay when unsupported.")
                .defineInRange("supportGracePeriod", 60, 0, 1200);

        FLUID_IGNITION_PROACTIVE_CHECK_PER_TICK = builder
                .comment("How many tracked ignited fuel fragments get re-checked for sources at random per tick")
                .defineInRange("proactiveCheckPerTick", 4, 0, 64);

        builder.pop();

        builder.push("air_intake_pollution_cleaning");

        AIR_INTAKE_POLLUTION_CLEANING_ENABLED = builder
                .comment("Whether an assembled air intake gradually reduces nearby pollution", 
                        "Requires Pollution of the Realms")
                .define("enabled", true);

        AIR_INTAKE_POLLUTION_RATE_1X1 = builder
                .comment("Base pollution quantity removed per minute by a single intake")
                .defineInRange("rate1x1PerMinute", 1.0, 0.0, 1000.0);

        AIR_INTAKE_POLLUTION_RATE_2X2 = builder
                .comment("Base pollution quantity removed per minute by a 2x2 air intake.")
                .defineInRange("rate2x2PerMinute", 5.0, 0.0, 1000.0);

        AIR_INTAKE_POLLUTION_RATE_3X3 = builder
                .comment("Base pollution quantity removed per minute by a 3x3 air intake.")
                .defineInRange("rate3x3PerMinute", 12.0, 0.0, 1000.0);

        AIR_INTAKE_POLLUTION_SPEED_BONUS_BASELINE_RPM = builder
                .comment("Multiplies the base rates by how fast they are spining starting at 1x with 128 Rpm up to 2x at max speed.")
                .defineInRange("speedBonusBaselineRpm", 128, 1, 1024);

        AIR_INTAKE_GAS_PRODUCTION_HEIGHT_Y = builder
                .comment("Fallback Y level to check for pollution when an air intake is",
                        "producing Carbon Dioxide or Sulfur Dioxide, only used if Pollution of",
                        "the Realms' own per-gas \"concentration altitude\" setting can't be",
                        "read directly (normally it can, and that value -- which may differ",
                        "between gases -- is used instead of this one).")
                .defineInRange("gasProductionHeightY", 192, -64, 320);

        builder.pop();

        SPEC = builder.build();
    }
}
