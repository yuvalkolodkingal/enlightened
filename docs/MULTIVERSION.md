# One jar for every NeoForge version above 1.21.1: the plan

This is a plan, not a description of something built. It is the same plan as Enhanced's
(`docs/MULTIVERSION.md` there has the full reasoning); this page has Enlightened's own numbers and the points where
the two mods differ. Enlightened is the smaller mod, so it should go first and serve as the pilot.

## 1. The versions

NeoForge versions with a stable build above 1.21.1 (checked on NeoForge's Maven on 2026-11-11). Versions that only
ever had beta builds (1.21.2, 1.21.6, 1.21.7, 1.21.9, 26.1, 26.1.1) are left out.

| Minecraft | NeoForge | Java | Audit: errors | Audit: files |
|---|---|---|---|---|
| 1.21.1 (today) | 21.1.x | 21 | 0 | 0 |
| 1.21.3 | 21.3.100 | 21 | 628 | 206 |
| 1.21.4 | 21.4.161 | 21 | 635 | 210 |
| 1.21.5 | 21.5.101 | 21 | 904 | 236 |
| 1.21.8 | 21.8.57 | 21 | 1,106 | 256 |
| 1.21.10 | 21.10.64 | 21 | 1,297 | 278 |
| 1.21.11 | 21.11.48 | 21 | 1,691 | 296 |
| 26.1.2 | 26.1.2.126 | 25 | not audited | |
| 26.2 | 26.2.0.93 | 25 | not audited | |
| 26.3 | 26.3.0.102 | 25 | not audited | |

**The audit.** The mod's main sources (234 Java files) were compiled, unchanged, against each NeoForge version and
javac's errors counted; "files" is how many files have at least one error. The numbers are a lower bound (fixing a
missing class uncovers uses of its changed methods), and they measure the distance from 1.21.1 to each version, not
from one version to the next.

What the numbers say:

* **1.21.3 already breaks 206 of the 234 files**, and 1.21.4 adds almost nothing. The main causes: the player model
  lost its type parameter and its parts moved (the render-state change: `PlayerModel`, `rightArm`, `leftArm`, the
  overrides that no longer match, about 250 errors), entity types are created with a registry key (20),
  `MobSpawnType` and `InteractionResultHolder` are gone, `teleportTo` and `sidedSuccess` changed (about 20 each).
  62 % of the errors are in the client code.
* **1.21.5** adds the NBT change (a tag's getters return an `Optional`) and `moveTo`.
* **1.21.10** adds two that hit the bending code itself: `level.isClientSide` is no longer a public field (60) and
  `player.serverLevel()` is gone (90). Both are one-line seams.
* **1.21.11** adds the rename of `ResourceLocation` to `Identifier`, which is mechanical.
* **26.1.2 could not be audited** with today's build: NeoForge 26.x only offers itself to a Java 25 build, and the
  project is set up for Java 21. Setting that up is part of phase 0.

## 2. How one jar serves many versions

In short (Enhanced's page explains each point):

* One `@Mod` entry class in a `boot` package that uses nothing version-specific. It reads the running Minecraft
  version and creates that version's copy of the mod by name.
* One complete copy of the mod per Minecraft version, each moved by the build into its own package
  (`com.kolodkingal.enlightened.v1_21_4`). A copy that is not used is never loaded.
* No copy may be found by FML's scan, so the copies have no `@Mod` and no `@EventBusSubscriber`.
* One mixin config per version, switched by a mixin plugin in `boot`.
* Shared textures, sounds and language files stored once; files whose format changed between versions in pack
  overlays.
* If a single jar cannot be made reliable (phase 0 finds out), the same build produces one jar per version instead.

What Enlightened has to change first, that Enhanced already did in its loader-independent port:

* **Four `@EventBusSubscriber` classes** (`ClientEvents` and the others, 35 `@SubscribeEvent` methods) become
  registrations made by the mod's entry (`NeoForge.EVENT_BUS.addListener(...)`), so that nothing is found by scanning.
* **43 files use NeoForge classes directly.** That is fine for a NeoForge-only mod and does not need Enhanced's
  whole platform layer. It only needs the version differences gathered in one `compat` package (see below).

Jar size is no concern here: the code is small and the assets are 2.7 MB.

## 3. One source tree

* **Stonecutter**: version conditions as comments in one source tree; the build makes each version's sources.
* **A `compat` package** with one small helper per common difference, so that conditions stay out of the abilities.
  The audit shows which: a handful of causes make up most of the errors (the notes above list them).
* **Rendering**: the gesture animations (`client/anim`) work on the player model's parts and are hit by the
  render-state change of 1.21.2 in almost every file; `WorldFx` and the renderers are hit again by 1.21.5 and
  1.21.10. One base class for "pose the player model" and one drawing helper for the world effects, introduced on
  1.21.1 first, keep the per-version part small.

## 4. The order

The gate of every phase: it compiles on every version done so far, the GameTests pass on each, and the **tour**
(`-Denlightened.tour=true`, which already exists) produces the same pictures as on 1.21.1.

| Phase | What | Done when |
|---|---|---|
| 0. Spike | A two-version jar of a toy mod (one mixin, one overlay) for 1.21.1 and 26.3. | It loads on both, client and dedicated server. If not: one jar per version. |
| 1. The build | Stonecutter, per-version projects, relocation, the `boot` entry, the mixin plugin; the four subscriber classes registered by hand. Only 1.21.1. | The 1.21.1 copy in the new jar passes the GameTests and the tour. |
| 2. The seams | `compat` helpers and the two rendering bases, on 1.21.1. | No behaviour change; the 1.21.3 audit drops sharply. |
| 3. 1.21.3 and 1.21.4 | Render states, item definitions, recipes. | Gate. |
| 4. 1.21.5 | Render pipelines, data component and NBT changes. | Gate. |
| 5. 1.21.8 | The HUD and the Scroll and Talk screens (GUI rendering changed in 1.21.6); entity saving. | Gate. |
| 6. 1.21.10 and 1.21.11 | The submit-based renderers (the bison, the dragons, the spirits); the 1.21.11 renames. | Gate. |
| 7. 26.1.2, 26.2, 26.3 | Java 25 and the new toolchain. | Gate. |

### Phase 0, first result (2026-11-11)

A toy mod built the way section 2 describes was tried: one jar holding a `boot` entry class and one `Entry` class per
version (`spike.v1_21_1.Entry` using `ResourceLocation`, `spike.v1_21_11.Entry` using its 1.21.11 name `Identifier`),
each compiled against its own NeoForge. **The same jar loaded on a dedicated server of NeoForge 21.1.265 (1.21.1) and
of 21.11.48 (1.21.11)**, and on each the entry of that version ran. So the core of the approach holds across the whole
1.21 range.

Still to prove before phase 1: a mixin config per version switched by a plugin, a pack overlay, a client (only
servers were started), and 26.x with its Java 25 classes. The two small projects are in Enhanced's `tools/multiversion-spike/`.

What is uncertain: the notes for 1.21.5 and later are from NeoForge's release notes and from memory, and 26.2 and
26.3 have not been audited. Re-run the audit at the start of each phase.
