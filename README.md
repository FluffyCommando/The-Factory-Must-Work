# Create: The Factory Must WORK

![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-62B47A)
![NeoForge](https://img.shields.io/badge/Loader-NeoForge-D7A65F)
![License: MIT](https://img.shields.io/badge/License-MIT-blue)
![Version 1.0.0](https://img.shields.io/badge/Version-1.0.0-orange)

A NeoForge addon for [The Factory Must Grow](https://www.curseforge.com/minecraft/mc-mods/tfmg) (TFMG), a [Create](https://www.curseforge.com/minecraft/mc-mods/create) addon. Adds a reworked oil extraction chain built around a new Steam fluid, plus a large collection of bug fixes for TFMG itself — multiblock reliability issues, missing pipe/fluid connectivity, crash loops, incorrect electrical/mechanical behavior, and JEI display bugs.

Packaged as its own mod jar rather than a fork of TFMG — bug fixes are applied via Mixin at runtime, so TFMG's own jar is never touched or recompiled.

## Requirements

- Minecraft 1.21.1, NeoForge
- [Create](https://www.curseforge.com/minecraft/mc-mods/create) and [TFMG](https://www.curseforge.com/minecraft/mc-mods/tfmg)

Optional, neither required:
- [Sable](https://www.curseforge.com/minecraft/mc-mods/sable) — if installed, the Surface Scanner correctly detects oil deposits when placed on a moving Sable physics object.
- [Pollution of the Realms](https://www.curseforge.com/minecraft/mc-mods/pollution-of-the-realms) — if installed, adds a Vat + Centrifuge recipe to recycle Polluted Water back into plain Water.

## Table of Contents

- [Installation](#installation)
- [New Content](#new-content)
- [Balance & Configuration](#balance--configuration)
- [Bug Fixes](#bug-fixes)
- [Known TFMG Limitations (Not Fixed)](#known-tfmg-limitations-not-fixed)
- [Building From Source](#building-from-source)
- [License](#license)

## Installation

1. Install NeoForge for Minecraft 1.21.1.
2. Download and install [Create](https://www.curseforge.com/minecraft/mc-mods/create) and [TFMG](https://www.curseforge.com/minecraft/mc-mods/tfmg).
3. Drop this mod's jar into your `mods` folder alongside them.

No config changes are required to get the new content and bug fixes working — everything ships with sensible defaults. See [Balance & Configuration](#balance--configuration) if you want to tune anything.

## New Content

**Oil Rock** — a new underground-spawning oil deposit, generated as large, organically-shaped clusters (similar in spirit to vanilla ore veins) across a configurable height range, rather than TFMG's original oil deposits which only spawn at a single fixed bedrock-level Y. Can fully replace or supplement TFMG's own oil worldgen (configurable), and existing old-world oil deposits are automatically migrated the first time their chunk loads — including, optionally, deposits placed at non-standard heights by a different mod's worldgen override (e.g. a sky world generator), via a config option that widens the migration scan to check every Y level instead of just TFMG's own fixed spawn height. Supports both infinite and finite (depletable) reserves.

**Fracking** — pump Steam into a connected Oil Rock deposit to "crack" it, boosting extraction speed. Cracked status isn't a one-time unlock — it decays continuously, so a deposit has to keep being fed Steam to stay cracked. A Surface Scanner can also detect Oil Rock deposits and report distance/direction via redstone signal.

**Steam & Boiler** — a new fluid, produced by a dedicated Boiler and consumed by pump jacks for fracking. Crafted from 6 Cast Iron Sheets, 2 Cast Iron Pipes, and a Cast Iron Fluid Tank, yielding 4 at a time. The Boiler stacks vertically into a multiblock (like TFMG's own Steel Tank) — both tank capacity and Steam production rate scale linearly with height, so a taller boiler is both bigger and faster, not just bigger. Water fills from any side of any segment; Steam only extracts from the top of the tallest segment. Steam has its own bucket item so it can be configured in fluid filters, something TFMG's own gas fluids support but Steam wouldn't have had otherwise.

**Reworked pump jack fluid sides** — each of a pump jack base's 6 faces can be individually wrenched to oil, waste, Steam, or left unassigned, with a floating icon over each configured face showing what it does. Multiple faces can share the same role. Every side starts unassigned; nothing is exposed until you configure it.

**Polluted Water recycling** — if Pollution of the Realms is installed, a Vat + Centrifuge recipe turns Polluted Water back into plain Water, with Mud as a byproduct.

**Restored TFMG sounds** — the electric hum, generator hum, and switch open/close sounds, which were removed during TFMG's 1.0 → 1.2 rewrite, are added back.

**Thicker pump jack ropes** — the crank and head connector "ropes" are rendered wider and with more visual depth, matching the rest of the machine's chunky, mechanical look more closely than the original thin lines.

## Balance & Configuration

Every number below is configurable in the mod's config file.

- Boiler: 1000 mB water → 2000 mB Steam.
- Pump jack fracking: 1000 mB Steam → 500 mB waste (Polluted Water if Pollution of the Realms is installed, otherwise plain Water), rate-limited per tick rather than converting an entire tank instantly.
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

Also fixed: a wrenched pump jack face wouldn't be recognized by an already-placed pipe until the pipe was broken and replaced, both for the actual fluid transfer and (separately) for the pipe's own client-side connection rendering.

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
- A rare client-side NPE during chunk rebuilds near certain fluid tank/pipe textures (surfaced as a CodeChickenLib error printed to chat) from an unguarded null case in a connected-texture lookup.

</details>

## Known TFMG Limitations (Not Fixed)

A few things found during investigation turned out to be incomplete or disabled features in TFMG's own code, rather than something a bugfix addon can reasonably patch — noted here for transparency rather than left silent:

- **Accumulator** can't discharge back into TFMG's own electrical network — that logic exists in TFMG's source but is entirely commented out, and the block doesn't expose a wire connection point at all. It can still be charged and drained through the standard Forge Energy capability (e.g. via a Cable Insulator in input mode, or any other FE-compatible block), just not through TFMG's voltage-based wires directly.
- **Converter**, **Fuse Block**, and **Engine Controller** are not placeable at all in this TFMG version — their block registrations are commented out entirely, consistently across every place they'd need to be wired up (block, block entity, capability, menu, networking). These aren't things this addon disabled; they were already unreachable.

## Building From Source

### How this is wired up

- Depends on `create`, `ponder`, and `flywheel` the same way TFMG itself does (versions pinned in `gradle.properties` to match TFMG 1.2.3 — bump them if you're targeting a different TFMG version).
- Depends on TFMG itself as a **local jar** in `/libs`, not a Maven artifact (TFMG isn't published anywhere Gradle can resolve it from directly). NeoForge's moddev plugin detects the `mods.toml` inside that jar and loads TFMG as a real mod when you run the dev client, so mixins targeting TFMG classes work normally.
- Also depends on `sable-companion` as a local jar in `/libs` (`compileOnly`, not `implementation` — it's genuinely optional). Used only by `SurfaceScannerBlockEntityMixin`/`SableIntegration` to make the surface scanner work correctly when placed on a Sable physics object. Extracted from `META-INF/jarjar/sable-companion-common-*.jar` inside a real Sable release jar rather than pulled from a Maven repo, since Sable's own publishing location wasn't confirmed. If you update Sable, you likely don't need to touch this unless Sable Companion's own API changes — extract its updated jarjar copy the same way if so.
- Bug fixes go in as Mixins (`src/main/java/com/tfmgtweaks/mixin`, see the README in that folder) so TFMG's own code never has to be touched or recompiled. New content (blocks/items/recipes) gets added the normal Registrate/NeoForge way in `TFMGTweaks.java`, just like TFMG does it in `TFMG.java`.
- Pure visual/data fixes (block/item models, textures, lang, etc.) go in as **resource overrides**: a file at the exact same path TFMG uses under `src/main/resources/assets/tfmg/...` in this project. Because the `tfmg` dependency in `neoforge.mods.toml` is declared with `ordering = "AFTER"`, this mod's resources load after TFMG's and win the merge — no mixin or Java code needed at all. Current example: `assets/tfmg/models/block/pumpjack_hammer/{block,block_wide,item}.json` gives the pump jack frame's connector gussets actual thickness (they were shipped as literal 0-thick planes in TFMG, which is why they can look like a gap or vanish depending on which way the block is facing).

### Setup

`libs/tfmg-1_2_2.jar` is already included in this repo, so it should build as-is. If you swap in a different TFMG version later, drop the new jar into `/libs` (filename just needs to start with `tfmg` and end in `.jar`) — the old one can stay or go, only files matching `tfmg*.jar` are picked up.

1. `./gradlew genEclipseRuns` / open in IntelliJ and let it sync — first sync downloads NeoForge, Create, Ponder, and Flywheel from their maven repos, so it needs network access.
2. `./gradlew runClient` to test.

If the build fails with "No TFMG jar found in /libs", the jar either isn't in that folder or doesn't start with `tfmg`.

### Versioning

Bump `mod_version` in `gradle.properties` on every change you package up for actual use — same convention as the Sable/Flowing Fluids compat mod.

## License

[MIT](LICENSE)
