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

## Progress (1.0.29): 1.21.1 and 1.21.4 in one jar

Phases 0 to 3 are done for 1.21.4 (1.21.3 is left out: nobody plays it, and 1.21.4 has the same changes).

* **The jar**: `boot.EnlightenedBoot` is the only `@Mod`; `boot.Versions` lists the copies; `boot.MixinGate` lets a
  copy's mixins apply only on its version (each copy has its own mixin config, made by the build). The four scanned
  subscriber classes are registered by hand.
* **The sources**: one tree. `versions/1.21.4` makes the 1.21.4 copy from it with the `Preprocess` task (buildSrc):
  version blocks (`//#if >=1.21.4` ... `//#else` ... `//#endif`, the other version's lines written as `//$$ code`),
  whole-file replacements in `versions/1.21.4/src/main/java`, plain renames in `versions/1.21.4/renames.txt`, and the
  move to the package `com.kolodkingal.enlightened.v1_21_4`. The calls that differ are in `compat.Compat`.
* **1.21.4's own code** (`versions/1.21.4/src/main/java`): the render-state bridge and the `Legacy*` base classes, so
  the renderers and models written against 1.21.1 draw unchanged from a render state; the attachment sync NeoForge 21.4
  lacks (`compat.AttachSync`); the client mixins whose targets changed.
* **Resources**: pack overlays made by `tools/overlays.py` (recipes and biomes in 1.21.4's format, the item model
  definitions, the spawn eggs' colours and the glider's dye).
* **Checked**: both copies compile; the jar starts a dedicated server and a client on 1.21.1 and on 1.21.4, and the
  quick tour (`-Denlightened.tour=true -Denlightened.tour.quick=true`) plays on both with the same pictures; the
  GameTests pass on 1.21.1. The GameTests are not built for 1.21.4 yet.

## Progress (1.0.31): 1.21.5 joins the jar

Phase 4 is done for the server: the jar holds a third copy, `com.kolodkingal.enlightened.v1_21_5`, made by
`versions/1.21.5`.

* **The versions are chained.** `Preprocess` (the same as Enhanced's) takes ordered lists: a copy's whole-file
  replacements are the folders of every version up to its own, the later winning (1.21.5: `versions/1.21.4/src/main/java`,
  then `versions/1.21.5/src/main/java`), and its renames are the `renames.txt` of each, applied in order. So
  `versions/1.21.5` holds only what changed since 1.21.4. The mixin configs are chained the same way in the root
  `build.gradle`: each copy lists its `chain` of version folders, whose `mixins.json` (`mixins`, `client`, `server`,
  and a `remove` list to drop an earlier version's) are merged over `enlightened.mixins.json`.
* **1.21.5's renames** (`versions/1.21.5/renames.txt`, 52 rules): `moveTo` to `snapTo`, the renamed effects, the NBT
  getters that return an `Optional` (the `...Or` forms with 1.21.1's defaults, `getCompoundOrEmpty`, `getListOrEmpty`),
  UUIDs and block positions through their codecs, `SavedData.Factory` to `SavedDataType` (a `CompoundTag.CODEC` bridge
  to the existing load and save), owners as `EntityReference`s, `Inventory.items`, villager types as registry keys,
  the click and hover event records, `entityInside`, tooltips to a consumer, `causeFallDamage`, the client input
  vector, the block entity renderer's camera position.
* **Version blocks** (`//#if >=1.21.5`) in the shared sources for the rest: the lion turtles' chunk ticket and the
  moved respawn point, the sky bison without `Saddleable`, the meditating body's armour slots, the items' server-only
  `inventoryTick`, the saved data's `save` (no longer an override), coal as a trim material (now an item component),
  and the two places that drew with `BufferUploader` (`CometSky`, `BlueScreenFire`), which draw through a render type on
  1.21.5. No 1.21.5 overlay files were needed: 1.21.4's (the render-state bridge, `AttachSync`, the client mixins)
  compile and apply unchanged.
* **Resources**: `tools/overlays.py` widens the 1.21.4 overlays to 1.21.5's formats (data 71, resources 55: their
  contents are still valid there) and adds two 1.21.5 overlays: the trim recipe with its `pattern`, and a texture and
  model of its own for each spawn egg (1.21.5 removed the template egg they were tinted from).
* **Checked**: all three copies compile (`./gradlew :compileJava :v1_21_4:compileJava :v1_21_5:compileJava`);
  `./gradlew build` makes the jar; the jar starts a dedicated server of NeoForge 21.5.101 (a new world, then again on
  it, no errors, the copy's four common mixins applied) and still of 21.4.161; the dev server starts on 1.21.1 and the
  119 GameTests pass there. **Not checked on 1.21.5**: a client (nothing client-side has been run there: the client
  mixins' targets were compared by signature only), the tour, and GameTests (they are not built for the later copies).

### Known differences on 1.21.5

* **The Eternal Flame's flicker** is sent from the server (1.21.5 ticks a held item on the server only), so it reaches
  the client a little later and is seen by everyone near, as before.
* **The sky bison** keeps its own saddle flag: 1.21.5 dropped `Saddleable` (saddles are equipment there), so a
  dispenser no longer saddles a bison. Saddling it by hand is unchanged.
* **The spawn eggs** have their own textures (an egg in the two colours, drawn by `tools/overlays.py`) instead of the
  game's tinted template egg.
* **The water spout's steering** reads 1.21.5's movement vector, which is normalised: moving diagonally is a little
  slower than on 1.21.1 (where the two keys add up).
* **Sozin's Comet in the sky and the blue fire over the screen** draw through a render pipeline (the comet's own, from
  the game's position-colour one, without depth test; the fire through the game's fire-overlay render type). They
  compile and follow the 1.21.4 drawing, but have not been seen on a 1.21.5 client yet.

## Progress (1.0.32): 1.21.11 joins the jar

Phases 5 and 6 are done for the server on 1.21.11 (1.21.8 and 1.21.10 are left out: 1.21.11 has all their changes):
the jar holds a fourth copy, `com.kolodkingal.enlightened.v1_21_11`, made by `versions/1.21.11` (NeoForge 21.11.48).
Java stays 21: 1.21.11's `version.json` says `"java_version": 21`.

* **The chain**: `versions/1.21.11` builds on 1.21.4's and 1.21.5's overlays and renames (in that order), so it holds
  only what changed since 1.21.5. The first compile of the copy had 1,135 errors in 138 files; then 567, 524, 214,
  130, 85, 43, 1 and 0 as the rules, overlays and version blocks below went in.
* **The renames** (`versions/1.21.11/renames.txt`, 104 rules): `ResourceLocation` is `Identifier` (and
  `ResourceKey.location()` is `identifier()`); the classes that moved (villagers, golems, minecarts, fireballs,
  `GameRules`, `Util`, `PlayerSkin`); `level.isClientSide()`; a server player's `level()` is its `ServerLevel` and the
  server comes through it; entities and block entities save to a `ValueOutput` and load from a `ValueInput`; typed game
  rules; the command permission check; a mob's restriction is its home; chunk ticket flags; explosion block particles;
  respawn points as `RespawnData`; `iron_chain`; `entityInside`'s extra argument; `canBeCollidedWith(Entity)`; the
  moon phase as an environment attribute; `GameProfile` as a record and bans by `NameAndId`;
  `ClientPacketDistributor`; the tempt range attribute of the tempted mobs (TemptGoal reads it from 1.21.6). On the
  client: the player model and `AvatarRenderState`, render types in `RenderTypes`, the render layers' `submit`, the
  overlay parts of the player model as children of their parts (no `copyFrom`), skins, sprites through the atlas
  manager, the level's render stage events, `GuiLayer`, the GUI's 2D pose and its pipelines, the input events of the
  screens and buttons, item rendering through `ItemStackRenderState`, the first-person hand renderer's collector.
* **Preprocess** learned file-scoped rules: `@in(<regex>) rule` applies a rename only to the files whose path the regex
  finds (the GUI's `pose.pushPose()` is not the world's).
* **1.21.11's own code** (`versions/1.21.11/src/main/java`): the render bridge for the submit-based renderers of 1.21.9
  (`compat.RenderBridge`, the `Legacy*Renderer` classes, `LegacyLiving`): a renderer or layer written against 1.21.1
  draws straight into the level's buffers while it is submitted (the level flushes them after the entities), and its
  `super.render(...)` submits vanilla's part to the collector; `compat.ItemSubmit` (items submitted);
  `compat.AttachCodecs` (attachments saved through a map codec in the same format as before, so a world keeps its
  bending); the anomaly cube's block entity renderer with a render state; the energy particle as a `SingleQuadParticle`;
  an empty `CosmicEffects` (1.21.11 has no dimension special effects); the client mixins whose targets changed
  (`HumanoidArmorLayerMixin`, `ItemInHandLayerMixin`, and `FlameFeatureRendererMixin` in place of
  `EntityRenderDispatcherMixin`, which `versions/1.21.11/mixins.json` removes).
* **Version blocks** (`//#if >=1.21.11`, 12 in 7 files): the shot's block state and kind saved through codecs, the
  attachments' serializers, the spawn eggs' entity type as an item property, the key category record and its
  registration, the render stage events, the back slot drawn on the screen's background event, the suit skins'
  textures, no dimension special effects.
* **Resources**: `tools/overlays.py` widens the 1.21.4 and 1.21.5 overlays to 1.21.11 (data 94.1, resources 75.0:
  recipes, item model definitions and the spawn eggs are still in those formats) and adds `overlay_1_21_11_data`: the
  noise settings with a preliminary surface level, the biomes with their sky, fog, particles, sounds and music as
  environment attributes, the dimension types in 1.21.11's form (a fixed time is a timeline of the day's values at that
  time, `data/enlightened/timeline/`; the cosmic plane's purple haze and its lack of clouds are values in it), and the
  tags with `minecraft:iron_chain`. Every overlay entry now carries `min_format` and `max_format` beside `formats`:
  1.21.11 reads the same entries for data and for resources and refuses an entry that reaches past format 64
  (resources) or 81 (data) without them.
* **Checked**: all four copies compile (`./gradlew :compileJava :v1_21_4:compileJava :v1_21_5:compileJava
  :v1_21_11:compileJava`); `./gradlew build` makes the jar; the jar starts a dedicated server of NeoForge 21.11.48 (a
  new world, then again on it), every summonable creature of the mod was summoned there, saved and found again after a
  restart, and six of the mod's places were raised with `/place structure` (the other five refused the spot they were
  given, as several do on 1.21.5) and loaded again, with no errors (the one error in the log, netty's kqueue probe, comes from the game
  itself, without the mod too); it still starts servers of 21.5.101 and 21.4.161; the 119 GameTests pass on 1.21.1.
  **Not checked on 1.21.11**: a client (nothing client-side has been run: the client mixins' targets were compared by
  signature only), the tour and the GameTests.

### Known differences on 1.21.11

* **A flying lemur on a shoulder is not drawn.** From 1.21.9 the game keeps what sits on a player's shoulder on the
  server and tells the clients only a parrot's variant, so the client cannot know a lemur is there. It still sits,
  and comes down, as before.
* **The anomaly is drawn in the frame's fog.** 1.21.6 made the fog a buffer of the game renderer's own, which the mod
  cannot swap for "no fog" while it draws, so far away the anomaly fades into the fog like everything else.
* **The back slot's frame** is drawn on the screen's background event (1.21.11 has no container background event);
  it is the same picture.
* **The renderers written for 1.21.1 draw straight into the level's buffers** (see the render bridge above) instead of
  submitting: they come out in the same frame, but outside 1.21.9's feature ordering (an outline or a translucent
  part may sort differently against vanilla's).
* **The sleeves, the hat and the trousers** of the player-shaped creatures follow their parts by being their children
  (the model's own way since 1.21.2), not by copying their pose.
* **The cosmic plane's sky** is the overworld's sky at midnight with the plane's purple haze and no clouds, made from
  1.21.11's environment attributes, not from 1.21.1's special effects; the spirit world's fixed morning likewise.
* **Not seen on a client yet**: everything client-side of 1.21.11 (the bridge, the HUD and the screens with their 2D
  pose, the comet sky's render setup, the hands in first person).

What is uncertain: the notes for 1.21.5 and later are from NeoForge's release notes and from memory, and 26.2 and
26.3 have not been audited. Re-run the audit at the start of each phase.
