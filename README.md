<div align="center">
  <img src="https://github.com/FluffyCommando/The-Factory-Must-Work/blob/main/logo.png" width="100">
  <h1>Create: The Factory Must WORK!</h1>
  <a href="https://github.com/FluffyCommando/The-Factory-Must-Work/releases/tag/2.1.1"><picture><img alt="Latest Release" src="https://img.shields.io/badge/Version-2.1.1-orange"></picture></a>
  <br>
  <a href="https://www.curseforge.com/minecraft/mc-mods/tfmw"><picture><source srcset="https://img.shields.io/badge/CurseForge-202830?style=for-the-badge&logo=curseforge" media="(prefers-color-scheme: dark)"><img src="https://img.shields.io/badge/CurseForge-white?style=for-the-badge&logo=curseforge" alt="CurseForge"></picture></a>
</div>

A NeoForge addon for [The Factory Must Grow](https://www.curseforge.com/minecraft/mc-mods/tfmg) (TFMG), a [Create](https://www.curseforge.com/minecraft/mc-mods/create) addon. Adds a reworked oil extraction chain built around a new Steam fluid, plus a large collection of bug fixes for TFMG itself — multiblock reliability issues, missing pipe/fluid connectivity, crash loops, incorrect electrical/mechanical behavior, and JEI display bugs.

Packaged as its own mod jar rather than a fork of TFMG — bug fixes are applied via Mixin at runtime, so TFMG's own jar is never touched or recompiled.

> **This is the lighter build for TFMG: Community Edition 1.3.2+.** It carries every new feature, but only the bug fixes TFMG:CE hasn't already fixed itself. For original TFMG 1.2.x, use the standard [TFMG](https://github.com/FluffyCommando/The-Factory-Must-Work) build instead. This build refuses to load without TFMG:CE.

## Requirements

- Minecraft 1.21.1, NeoForge
- [Create](https://www.curseforge.com/minecraft/mc-mods/create) and TFMG: Community Edition 1.3.2 or newer

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

Only fixes that TFMG:CE 1.3.2 still needs are included. Everything else from the TFMG 1.2.x build has already been fixed in TFMG:CE itself.

<details>
<summary><strong>Fluid & pipe connectivity</strong></summary>
<br>

Several machines fill their own output tanks internally without telling the game their capability changed, so a connected pipe can cache a stale "nothing here" result and never look again. Fixed for:

- Vat (plus: a hopper could spill into the output item slots once the input filled, starving recipe completion)
- Pump jack
- Exhaust
- Blast Furnace Output
- Blast Stove outputs
- Firebox exhaust
- Distillation Controller and every one of its output blocks
- Engine exhaust

</details>

<details>
<summary><strong>Multiblocks & recipes</strong></summary>
<br>

- Air Intake: a premature 2×2 could permanently block a valid 3×3, and group membership wasn't persisted across reloads.
- Vat: a simpler recipe (e.g. water into Steam) could win over a more specific one (e.g. concrete) when both matched, and a cached recipe was never re-checked once a more specific one became available.

</details>

<details>
<summary><strong>Crashes & rendering</strong></summary>
<br>

- Engines: crash while being carried by a Create contraption.
- Create's segmented display: client disconnect from an unguarded null in Create's DynamicComponent.
- Create's connected textures: render errors from an unguarded null target sprite.
- Create's fluid tanks: a split multiblock wasn't corrected on the client.
- Air Intake: disappeared into a dark void past 64 blocks.
- Electric Diode and Fuse: broken model parent and particle texture.

</details>

<details>
<summary><strong>Other</strong></summary>
<br>

- Regular/radial engine assembly tooltip said "Pistons Missing" for a part that's actually called an Engine Cylinder.
- Steel Mechanism sequenced assembly now accepts any nickel/lead plate by tag.

</details>

## Known TFMG Limitations (Not Fixed)

A few things found during investigation turned out to be incomplete or disabled features in TFMG's own code, rather than something a bugfix addon can reasonably patch — noted here for transparency rather than left silent:

- **Accumulator** can't discharge back into TFMG's own electrical network — that logic exists in TFMG's source but is entirely commented out, and the block doesn't expose a wire connection point at all. It can still be charged and drained through the standard Forge Energy capability (e.g. via a Cable Insulator in input mode, or any other FE-compatible block), just not through TFMG's voltage-based wires directly.
- **Converter**, **Fuse Block**, and **Engine Controller** are not placeable at all in this TFMG version — their block registrations are commented out entirely, consistently across every place they'd need to be wired up (block, block entity, capability, menu, networking). These aren't things this addon disabled; they were already unreachable.

## Building From Source

### How this is wired up

- Depends on `create`, `ponder`, and `flywheel` the same way TFMG itself does (versions pinned in `gradle.properties` to match TFMG:CE 1.3.2 — bump them if you're targeting a different TFMG:CE version).
- Depends on TFMG:CE itself as a **local jar** in `/libs`, not a Maven artifact (TFMG isn't published anywhere Gradle can resolve it from directly). NeoForge's moddev plugin detects the `mods.toml` inside that jar and loads TFMG as a real mod when you run the dev client, so mixins targeting TFMG classes work normally.
- Also depends on `sable-companion` as a local jar in `/libs` (`compileOnly`, not `implementation` — it's genuinely optional). Needed only at compile time, because `SurfaceScannerBlockEntityMixin` calls TFMG:CE's own `SurfaceScannerSable` helper to resolve the scanner's real position on a Sable physics object. Extracted from `META-INF/jarjar/sable-companion-common-*.jar` inside a real Sable release jar rather than pulled from a Maven repo, since Sable's own publishing location wasn't confirmed. If you update Sable, you likely don't need to touch this unless Sable Companion's own API changes — extract its updated jarjar copy the same way if so.
- Also depends on Pollution of the Realms (modid `adpother`) as a local jar in `/libs` (`compileOnly`, same reasoning as `sable-companion` — genuinely optional). Used only by `AirIntakeBlockEntityMixin`'s pollution-cleaning tick logic and `PollutionIntegration`. Not bundled in this repo — drop your own `AdPother-*.jar` into `/libs` if you want this feature to compile; if it's absent, `PollutionIntegration.java`'s imports won't resolve and the build fails the same way a missing TFMG jar would. `PollutionCompat` is what keeps the feature itself runtime-optional (does nothing if the mod isn't loaded) — this `/libs` jar is purely about letting this mod's own code compile against its API in the first place. Also needs `ForgeEndertech-*.jar` alongside it — Pollution of the Realms' own required dependency (declared as `modId="forgeendertech"` in its own `neoforge.mods.toml`, a shared library the same author uses across their other mods), since `ChunkPollution`/`PollutionInfo`'s public API reaches into that library's own classes too. If you have Pollution of the Realms actually working in a real instance, you already have this jar in your mods folder — copy it into `/libs` the same way.
- Bug fixes go in as Mixins (`src/main/java/com/tfmgtweaks/mixin`, see the README in that folder) so TFMG's own code never has to be touched or recompiled. New content (blocks/items/recipes) gets added the normal Registrate/NeoForge way in `TFMGTweaks.java`, just like TFMG does it in `TFMG.java`.
- Pure visual/data fixes (block/item models, textures, lang, etc.) go in as **resource overrides**: a file at the exact same path TFMG uses under `src/main/resources/assets/tfmg/...` in this project. Because the `tfmg` dependency in `neoforge.mods.toml` is declared with `ordering = "AFTER"`, this mod's resources load after TFMG's and win the merge — no mixin or Java code needed at all. Current example: `assets/tfmg/models/block/pumpjack_hammer/{block,block_wide,item}.json` gives the pump jack frame's connector gussets actual thickness (they were shipped as literal 0-thick planes in TFMG, which is why they can look like a gap or vanish depending on which way the block is facing).

### Setup

Copy your TFMG:CE jar (e.g. `tfmg-1.21.1-1.3.2-community.jar`) from your instance's mods folder into `/libs` before building — it isn't included in this repo. Only files matching `tfmg*.jar` are picked up, so make sure no original TFMG 1.2.x jar is sitting in `/libs` alongside it.

1. `./gradlew genEclipseRuns` / open in IntelliJ and let it sync — first sync downloads NeoForge, Create, Ponder, and Flywheel from their maven repos, so it needs network access.
2. `./gradlew runClient` to test.

If the build fails with "No TFMG:CE jar found in /libs", the jar either isn't in that folder or doesn't start with `tfmg`.

### Versioning

Bump `mod_version` in `gradle.properties` on every change you package up for actual use — same convention as the Sable/Flowing Fluids compat mod.

## License

[MIT](LICENSE)
