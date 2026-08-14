# Create: The Factory Must WORK

![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-62B47A)
![NeoForge](https://img.shields.io/badge/Loader-NeoForge-D7A65F)
![License: MIT](https://img.shields.io/badge/License-MIT-blue)
![Version 1.0.0](https://img.shields.io/badge/Version-1.0.0-orange)

A NeoForge addon for [The Factory Must Grow](https://www.curseforge.com/minecraft/mc-mods/tfmg) (TFMG), a [Create](https://www.curseforge.com/minecraft/mc-mods/create) addon. Adds a reworked oil extraction chain built around a new Steam fluid, plus a large collection of bug fixes for TFMG itself — multiblock reliability issues, missing pipe/fluid connectivity, crash loops, incorrect electrical/mechanical behavior, and JEI display bugs.

Packaged as its own mod jar rather than a fork of TFMG — bug fixes are applied via Mixin at runtime, so TFMG's own jar is never touched or recompiled.

## Requirements

- Minecraft 1.21.1, NeoForge
- [Create](https://www.curseforge.com/minecraft/mc-mods/create) and [TFMG](https://www.curseforge.com/minecraft/mc-mods/create-industry)

Optional, neither required:
- [Sable](https://www.curseforge.com/minecraft/mc-mods/sable) — if installed, the Surface Scanner correctly detects oil deposits when placed on a moving Sable physics object.
- [Pollution of the Realms](https://www.curseforge.com/minecraft/mc-mods/pollution-of-the-realms) — if installed, adds Polluted Water as a fracking biproduct and adds a Vat + Centrifuge recipe to recycle Polluted Water back into plain Water.

## Table of Contents

- [Installation](#installation)
- [New Content](#new-content)
- [Balance & Configuration](#balance--configuration)
- [Bug Fixes](#bug-fixes)
- [License](#license)

## Installation

1. Install NeoForge for Minecraft 1.21.1.
2. Download and install [Create](https://www.curseforge.com/minecraft/mc-mods/create) and [TFMG](https://www.curseforge.com/minecraft/mc-mods/create-industry).
3. Drop this mod's jar into your `mods` folder alongside them.

No config changes are required to get the new content and bug fixes working — everything ships with sensible defaults. See [Balance & Configuration](#balance--configuration) if you want to tune anything.

## New Content

**Oil Rock** — A new underground-spawning oil deposit, generated as large, organically-shaped clusters across a configurable height range, rather than TFMG's original oil deposits which only spawn at a single fixed bedrock-level Y. Can fully replace or supplement TFMG's own oil worldgen (configurable), and existing old-world oil deposits are automatically migrated the first time their chunk loads. Supports both infinite and finite (depletable) reserves. A Surface Scanner can also detect Oil Rock deposits and report distance/direction via redstone signal.

**Fracking** — Pump Steam into a connected Oil Rock deposit to crack it by feeding it Steam to stay cracked, boosting extraction speed.

**Steam & Boiler** — A new fluid, produced by a dedicated Boiler and consumed by pump jacks for fracking. The Boiler stacks vertically into a multiblock (like TFMG's own Steel Tank) — both tank capacity and Steam production rate scale linearly with height. Water fills from any side of any segment; Steam only extracts from the top of the tallest segment. You can filter for steam with a smart pipe using the steam bucket.

**Reworked pump jack fluid sides** — A wrench can be used on the Pump Jack base to set the output/input to oil, waste, Steam, or left unassigned.

**Sable Compatability** — If Sable is installed, The Scanner can be used on a sable sub level to find oil deposits. 

**Pollution of the Realms Compatability** — If Pollution of the Realms is installed, Polluted water will be a byproduct of Fracking and a Vat + Centrifuge recipe is added that turns Polluted Water back into plain Water, with Mud as a byproduct.

**Restored TFMG sounds** — the electric hum, generator hum, and switch open/close sounds, which were removed during TFMG's 1.0 → 1.2 rewrite, are added back.

**Thicker pump jack ropes** — the crank and head connector ropes are rendered wider and with more visual depth, matching the rest of the machine's chunky, mechanical look more closely than the original thin lines.

## Balance & Configuration

Every number below is configurable in the mod's config file.

- Boiler: 1000 mB water → 2000 mB Steam.
- Pump jack fracking: 1000 mB Steam → 500 mB waste (Polluted Water if Pollution of the Realms is installed, otherwise plain Water).
- Oil Rock: cluster size, spawn height range, number of nearby satellite deposits, reserve amount, extraction speed multipliers (base and cracked), decay rate, and whether cracking is required to extract at all.
- Surface Scanner: rescan interval.

## Bug Fixes

<details>
<summary><strong>Fluid & pipe connectivity</strong></summary>
<br>

A recurring category: several TFMG machines mutate their own fluid tank internally (finishing a recipe, venting excess gas, extracting oil) without ever telling the game their capability changed. A connected pipe that happened to check at the wrong moment caches a stale "nothing here" or "tank full" result and never looks again, appearing to silently stop working. Fixed for:

- Vat (also fixed separately: a full input side would block all output, since the combined input/output handler had no concept of which side external insertion should go into; and separately again, fluid output wasn't reliably extractable even with a matching filter, since drain requests searched input and output tanks together in one combined handler — the same root-cause shape as the pump jack's original Steam-rejection bug)
- Pump jack (oil filling up partway then appearing to stop for good)
- Casting Basin (stops accepting new input after the first recipe)
- Flarestack and Exhaust (stop accepting gas after venting some, backing up production upstream)
- Blast Furnace Output (both its own tanks and hot air consumption from its connected hatch)
- Distillation Controller (matches a reported "controllers reset heat level to 0" bug) and every one of its output blocks

</details>

<details>
<summary><strong>Multiblock reliability</strong></summary>
<br>

Several TFMG multiblocks only evaluate their own structure once, right after being built or on specific block-update events — if that first check runs before every neighboring segment's chunk has finished loading (much more likely on a server than in casual singleplayer testing), the result gets stuck wrong indefinitely, until the structure is manually broken and rebuilt. Fixed with periodic self-correction for:

- Vat (mixers/centrifuges/electrodes not working, especially on servers)
- Distillation tower (loses heat detection and its window/tank appearance after a world reload)
- Blast furnace (a fully reinforced furnace reverts to "regular" the first time an item is processed)
- Pump jack (the whole multiblock disassembles if any nearby chunk unloads even briefly, rather than tolerating momentary chunk-load hiccups)

Also fixed: adjacent Vats or Fluid Tanks of different materials (e.g. a Steel Vat next to a Cast Iron Vat, or an Aluminum Tank next to a Cast Iron Tank) would incorrectly merge into one mismatched-looking multiblock, since TFMG's own connectivity code only checks block-entity type, not which specific material variant. Fixed preventatively — a mismatched neighbor is now rejected before ever being absorbed, rather than merging and then having to be detected and split apart afterward.

Also fixed: different regular engine types (I/V/W/U/Boxer/Radial/Turbine — different piston arrangements and different speed/torque/efficiency values) could be chained together into one multi-engine structure, since the type-compatibility check for this existed in TFMG's own source but was never enabled.

Also fixed: a Coke Oven within scanning range of a second oven facing toward it would have its door state unnecessarily reset, since the scan direction used didn't match the direction the oven's own structure actually extends in.

</details>

<details>
<summary><strong>Crashes</strong></summary>
<br>

- Winding machine: dedicated server crash loop from an item missing an expected data component.
- Firebox: crash on login/chunk load from a null multiblock controller reference.
- Engines (regular, radial, turbine): crash while being carried by a Create contraption (e.g. a plane); lost piston/cylinder components on being broken or wrenched up.
- Large Transformer and Large Switch: crash loop when any other electric block is placed nearby.
- Large Switch: log-spamming Flywheel error every time one enters or leaves render distance.
- Electric network manager: unguarded map lookups that could NPE on level load/unload timing edge cases (affects every electric block).
- Cable connectors: incorrect endpoint resolution left the far end of a cable connection showing stale voltage/network data indefinitely after removal, and could leave network membership incomplete during normal play.

</details>

<details>
<summary><strong>JEI</strong></summary>
<br>

- Industrial Blasting recipes that need hot air never showed that requirement.
- Vat recipes with a minimum vat size never showed that requirement.
- A startup error from a duplicate fluid subtype registration that conflicted with Create's own.

</details>

<details>
<summary><strong>Other</strong></summary>
<br>

- Goggles blocked the multimeter overlay entirely, regardless of whether a multimeter was even held.
- Air Intake's air production silently truncates to 0 below a real, otherwise-invisible RPM threshold — now shown in the goggle tooltip along with the RPM actually needed.
- Regular/radial engine assembly tooltip said "Pistons Missing" for a part that's actually called an Engine Cylinder.
- A 3×3 Blast Stove would render with only its 4 corners visible; capped to the working 2×2 size.
- Surface Scanner performance: reduced stutter from unthrottled rescans and unstrided full-column scans, and compatibility with scanners placed on Sable physics objects.
- Traffic Light never reached green at short timer settings (including its own default) — the yellow-transition windows were fixed tick counts rather than scaled to the configured cycle length, so at short settings the math for reaching green became mathematically impossible.
- A rare client-side NPE during chunk rebuilds near certain fluid tank/pipe textures from an unguarded null case in a connected-texture lookup.

</details>

## License

[MIT](https://github.com/FluffyCommando/The-Factory-Must-Work/blob/main/LICENSE)
