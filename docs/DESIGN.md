# Enlightened — Full Design

**Enlightened** is a Minecraft mod (NeoForge, Minecraft 1.21.1) that lets players become benders of the Avatar
world: an **earthbender**, an **airbender**, a **waterbender** or a **firebender**, with every sub-bending art the
shows gave them (sand, lava, metal; the airbender's flight; ice, blood; lightning), a progression of ability points
earned by fighting with the bending, and a loadout: the bender chooses which of his many abilities sit on his five
keys.

This document completes the original concepts by Eilon Kolodkin-Gal (the four elements and their abilities, the
points, the looks, the choice of abilities for battle). Everything the concepts said is kept. Where they left a gap
(where a scroll comes from, what key does what, exact numbers), the gap is filled in here, and every addition is
marked **(added)**. The player's guide, in English and Hebrew, is on the wiki; its pages are in
[docs/wiki](wiki/).

The mod is built on the frame of [Enhanced](https://github.com/yuvalkolodkingal/Enhanced), the superhero mod of
the same authors, and plays the same way: one power per player, saved and synced as a data attachment, ability keys
R G V B N with Sneak variants, a look key H, a HUD panel in the top-left corner, items that awaken the power, a
server config, operator commands and GameTests. The design of other Avatar mods and plugins was studied too:
ProjectKorra (Bukkit: its abilities per element, sub-elements and their unlocking) and the Bending mod (Forge: its
hotbar slot binding of abilities, which is where the loadout of section 2 comes from).

---

## 0. Rules for every bender

| Rule | Detail |
|---|---|
| One element per player | A player bends exactly one of: *none*, earth, air, water, fire. Elements never stack. Only an operator command changes it (`/enlightened element set`). |
| Permanent | Once a scroll is read, the bending survives death, logging out and changing dimension. |
| Ability keys **(added)** | Five keys, **R, G, V, B, N** by default (rebind under *Options → Controls → Enlightened Bending*). Each key has two slots: the plain key and **Sneak + key**. The slot's ability answers the key. **H** changes the look (Sneak + H goes back). **K** opens the Bending Scroll. |
| The loadout **(added)** | There are more abilities than slots (an earthbender has seventeen), so the bender chooses which sit on his ten slots, in the Bending Scroll (section 2). Every learned ability takes the first free slot by default. |
| Points | Every monster killed with the bender's own bending (any damage of the tag `enlightened:bending`) earns one experience; a boss ten; another player two; animals and villagers none. Every `general.xpPerPoint` (3) experience is one **ability point**. A new bender has his element's starter ability at level 1 and `general.startingPoints` (1) to spend. |
| Levels | Every ability has three levels: novice, adept, master. Level *n* costs *n* points. The numbers of an ability (count, size, range, damage, time) are given per level below as *novice / adept / master*. |
| Sub-bending gates | Sand, lava and metal; ice and blood; lightning: an ability of a sub-art can be learned only after `sandGate` (3), `lavaGate` (6), `metalGate` (9), `iceGate` (3), `bloodGate` (9) or `lightningGate` (6) points were spent on the element. The airbender's flight and suffocation open only to a **tetherless** airbender (section 4.9). |
| Strength | A bender's strength is the points spent on his element. Where bending meets bending (quicksand, bloodbending) the stronger or equal bender is never taken. |
| Hunger | Bending costs food (exhaustion per use; holds and toggles a trickle). A starving bender cannot hold a shield or a stream. Creative players bend for free. |
| HUD **(added)** | The top-left corner shows the element, the look and its key, each key with the ability on it and on its Sneak slot (level in stars, cooldown, ON/OFF, "holding", "charging"...), the points, the experience to the next, the water a waterbender carries, and whether bending was taken away. |
| Looks **(added)** | Every element has several looks (section 7); the look changes only how the bender looks and the colour of his energy, except Azula, whose fire is blue. Every element includes *Your own skin*. |
| Area attacks | A fire ring, an air blast, a tornado, a spout: never the caster, a Creative or Spectator player, a teammate or the caster's pets; a player only with PvP on; hostile creatures always; villagers, animals and golems only if `general.hurtPassives` is on (on by default since 1.1, so a bender's rock, whip or flame hits a cow as well as a zombie; a server that wants its animals safe turns it off). |
| Blocks bending raises | Walls, pillars, lava paths and frozen water are **temporary**: the block that was there comes back after the ability's time (`general.raisedBlockSeconds`, `earth.wallSeconds`, `earth.lavaSeconds`), at the server's stop, or when the bender loses his element. Nothing is placed where the player may not place (spawn protection, claims, Adventure mode, the world border), and nothing is torn out of a block he may not break. |

---

## 1. Becoming a bender: the scrolls **(added)**

Four scrolls, one per element, crafted in a 3x3 grid: paper in the middle, four of one thing on the sides and four of
another in the corners:

| Scroll | Sides | Corners |
|---|---|---|
| Earth Scroll | Emerald | Obsidian |
| Air Scroll | Breeze Rod | Feather |
| Water Scroll | Prismarine Crystals | Nautilus Shell |
| Fire Scroll | Blaze Rod | Magma Cream |

They are also found in chests (`general.scrollsInChests`): earth in stronghold libraries (8%) and mineshafts (4%);
air in End city treasure (10%), ancient cities (6%) and jungle temples (6%); water in shipwreck treasure (8%),
big underwater ruins (8%) and buried treasure (10%); fire in Nether fortresses (8%) and bastion treasure (10%).

Right-click with a scroll: a player with no bending becomes a bender of its element (the dramatic awakening: a
title, a burst of the element's colour, a sound, a message to the whole server, a hint to press K) and the scroll
is used up (not in Creative). A bender of the same element reads it to open the Bending Scroll screen instead; a
bender of another element cannot read it.

## 2. The Bending Scroll: points, levels and the loadout **(added)**

The **K** key (or the scroll item) opens the screen. On the left every ability of the element, with its level in
stars, the cost of the next level, its gate (and the points it asks for) and a **+** button that learns the next
level (greyed out when it cannot be learned: not enough points, the gate closed, mastered already). On the right
the ten slots, five keys plain and with Sneak, each with the ability on it: click an ability on the left to select
it, then a slot to put it there (an ability sits on one slot at a time; it leaves its old slot); right-click a slot
to clear it; *Default keys* lays the learned abilities out in the element's default order. Under the slots, the
selected ability's description. Passive abilities sit on no key. Everything the screen shows comes from the synced
bending data, so it always shows what the server knows; its buttons send `ScrollActionPayload` (learn, assign,
reset) to the server.

`/enlightened learn`, `/enlightened loadout` and `/enlightened points add` do the same for operators (section 8).

## 3. Earthbending

The first art of every earthbender is **Rock Throw**. The sub-arts: **sand** (3 points spent), **lava** (6),
**metal** (9).

| # | Ability | Kind | How it works |
|---|---|---|---|
| 1 | **Rock Throw** | press | Rocks tear out of the earth in front of him (the block under the ground there, drawn as that block) and fly at what he looks at: **1 / 2 / 3** rocks, each `earth.rockDamage` (4) + level damage, knocking the target back. Needs earth within 3 blocks under the point ahead; `earth.rockCooldown` (20 ticks). |
| 2 | **Rock Lift** | press | Look at a block of earth within `earth.rockRange` (24) and press: it is torn out (`earth.rockLiftTakesBlocks`; a block torn out drops its item when it shatters or is set down) and held in the air in front of him, orbiting with the others: **1 / 2 / 3** at once. Press while looking at no earth (or holding the most) to throw them all at what he looks at (damage 6 + level). The grip ends if he dies, logs out or loses the element (the rocks fall as items). |
| 3 | **Earth Armor** | toggle | A shell of rock over his whole body, the eyes open (a client layer draws it on an inflated player model with a stone texture transparent at the eyes): **+6 / +10 / +14 armour**, **+2 / +4 / +6 toughness**, 30% knockback resistance, but **`earth.earthArmorSlow` (30%) slower** (5% less per level). Needs earth under his feet to put on. |
| 4 | **Earth Pillar** | press | The ground rises under his feet as a pillar of its own block, **3 / 5 / 8** high, and carries him up; the pillar stands `general.raisedBlockSeconds` (10 s) and crumbles. Cooldown 6 s. |
| 5 | **Earth Wall** | press | A wall of the ground's block rises 2.5 blocks in front of him, **3 / 5 / 7 wide**, 3 high, for `earth.wallSeconds` (10 s): rocks, arrows and blasts stop at it. Cooldown `earth.wallCooldown` (200 ticks). |
| 6 | **Seismic Sense** | toggle | Standing on the ground (not flying), every creature within **12 / 20 / 32** blocks that touches the ground (or the water) is outlined through walls (the server names them every half second, `SeismicPayload`; the client outlines them as the glowing effect does). |
| 7 | **Sand Wall** | press, sand | As the earth wall, of sand; needs sand within 6 blocks. |
| 8 | **Sand Lift** | press, sand | Sand swirls up round him and flies at his enemy as **1 / 2 / 3** clouds: 2 + level damage and **blindness** for 3 s. Needs sand within 4 blocks. Cooldown 1.5 s. |
| 9 | **Quicksand** | press, sand | The sand under the creature he looks at (within 16) turns to quicksand: it sinks (pulled down, 80% slower, no jumping, a little hurt each second) for **4 / 6 / 8 s**. An adept turns earth under the target to sand for the while. **A stronger or equal earthbender is never sunk.** Cooldown 10 s. |
| 10 | **Lava Path** | press, lava | The ground ahead of him, toward where he looks, turns to lava for **4 / 7 / 10** blocks, for `earth.lavaSeconds` (8 s); then sets back to what it was. He gets fire resistance for the while (he does not burn in his own lava). Cooldown 15 s. |
| 11 | **Lava Shuriken** | press, lava | A spinning disc of lava (drawn as a magma block) at what he looks at: 5 + level damage and burning for **3 / 4 / 6 s**; where it lands it may light a fire. Cooldown 2.5 / 2 / 1.5 s. |
| 12 | **Lavabender's Skin** | passive, lava | Fire and lava hurt him **25% / 50% / 75%** less. |
| 13 | **Metal Wall** | press, metal | As the earth wall, of iron blocks. Needs metal: a metal block within 8, or metal in his inventory (`enlightened:metal_items`). |
| 14 | **Metal Blades** | press, metal | **1 / 2 / 3** blades of metal (drawn as spinning iron swords) at what he looks at: `earth.metalBladeDamage` (6) + level damage. Needs metal. Cooldown 1.5 s. |
| 15 | **Metal Armor** | toggle, metal | Full armour of metal over the whole body but the eyes: **+16 / +20 / +24 armour**, **+6 / +8 / +10 toughness**, 60% knockback resistance, **40% slower** (5% less per level). Replaces the earth armour. Needs metal. |
| 16 | **Metal Pull** | press, metal | The metal in the ground within `earth.metalPullRadius` (6) + 3 per level comes to him: every ore of `enlightened:metal_ores` turns to its stone and gives its metal (raw iron, raw gold, 2-5 raw copper, 2-5 gold nuggets, netherite scrap) straight into his inventory, and loose metal items on the ground come too; **8 / 16 / 32** things at most. Only where he may break. Cooldown 8 s. |
| 17 | **Wreck** | press, metal | The metal device he looks at (within 12; `enlightened:metal_devices`: furnaces, blast furnaces, smokers, pistons, hoppers, dispensers, droppers, observers, rails, iron doors and trapdoors, bars, chains, anvils, cauldrons, lightning rods, weighted plates, lanterns, bells, grindstones, smithing tables, stonecutters, copper bulbs and grates, crafters, iron and copper blocks) is torn apart, and the devices within 2 blocks of it with it: **1 / 3 / 6** at once (`earth.wreckDrops`: whether they drop). A minecart in his view breaks; an iron golem takes 6 + 2 per level damage. Cooldown 5 s. |

Default keys: R Rock Throw, Sneak+R Rock Lift, G Earth Armor, Sneak+G Metal Armor, V Earth Pillar, Sneak+V Seismic
Sense, B Earth Wall, Sneak+B Sand Wall, N Quicksand, Sneak+N Lava Shuriken; then Lava Path, Metal Blades, Metal Wall,
Sand Lift, Metal Pull and Wreck wait for a free slot.

**Looks:** Toph Beifong (default), King Bumi, Lin Beifong, Suyin Beifong, Ghazan (red-orange energy), Bolin, your
own skin.

## 4. Airbending

The first art is **Air Blast**. No sub-art has a point gate; **flight** and **suffocation** need the tetherless
state (4.9).

| # | Ability | Kind | How it works |
|---|---|---|---|
| 1 | **Air Blast** | press | A wave of air throws everyone in a 35° cone within **6 / 9 / 12** blocks back: `air.airBlastPush` (1.6) × **1 / 1.5 / 2.2**, 1 damage; items and arrows go with it. A tetherless airbender's blast throws 30% less and does `air.tetherlessDamageBonus` (2) more. Cooldown `air.airBlastCooldown` (20 ticks). |
| 2 | **Air Shield** | toggle | A sphere of air of radius **2.5 / 3 / 3.5** (drawn as a faint sphere of swirling bands) keeps creatures out and turns arrows back (at their shooter). It takes the blows meant for him (everything but falls and what bypasses armour): `air.airShieldStrength` (10) × **1 / 1.5 / 2** damage before it **breaks**; then it **bursts**: everyone within the radius + 2 is thrown back and takes `air.airShieldBurstDamage` (7, three and a half hearts), and the shield rests 15 s. Costs a trickle of food. |
| 3 | **Air Jump** | press | Air shot at the ground sends him up (**0.9 / 1.2 / 1.5** blocks a tick of lift) and the creatures round him with him, with slow falling for **3 / 4 / 5 s**. Cooldown 1.5 s. **The glide (added):** falling with the **Air Glider** in either hand (`air.gliderNeeded`; false lets every airbender glide), not sneaking, the air carries him forward (0.55 / 0.65 / 0.75 blocks a tick) and down slowly (faster when he looks down); no fall damage. |
| 4 | **Air Blades** | press | **1 / 2 / 3** cutting blades of air at what he looks at: `air.airBladeDamage` (5, two and a half hearts) each, no knockback. Cooldown 1 s. |
| 5 | **Spirit Projection** | press | Sitting on the ground, his spirit leaves the body: he becomes a spectator for `air.projectionSeconds` (20) × **1 / 2 / 3** seconds, within `air.projectionRange` (32) × **1 / 2 / 4** blocks of the body (farther, the tether pulls him back), free to look through walls and into caves. The body stays as a meditating figure with his skin; **if anything hurts it, the spirit returns at once and the blow lands on him**. Press again to return early; logging out or changing dimension returns him. Cooldown 20 s. |
| 6 | **Air Bubble** | passive | Under water a bubble of air round his head: a novice's air never drops below 120 ticks, an adept breathes freely, a master swims like a dolphin. |
| 7 | **Air Scooter** | toggle | Riding a ball of air: **60% / 90% / 120%** faster, steps up a block, never falls hard. |
| 8 | **Tornado** | press | A small tornado walks ahead of him for **5 / 7 / 9 s**, **1.5 / 2.5 / 3.5** wide and 2.5 times as high: it lifts, spins and throws everything it meets (creatures, items, arrows) and hurts **1 / 1.5 / 2** every half second (one more for a tetherless airbender); at a wall it turns. Cooldown 12 s. |
| 9 | **Flight** | toggle, tetherless | Creative-like flight (double-tap Space or press the key again to take off or land), flying speed 0.08. |
| 10 | **Suffocation** | hold, tetherless | The air leaves the lungs of the creature he looks at (within 8) while he holds the key: its air drains, then 2 damage a second (drowning); it is slowed. Ends if it gets more than 12 blocks away. Cooldown 6 s. |

**4.9 The tetherless airbender (added).** An airbender who **kills his own pet** (a creature tamed by him) lets go
of his earthly tethers (`air.petKillSeversTethers`): the flag is permanent, flight and suffocation can be learned,
his blasts and blades do `air.tetherlessDamageBonus` more and his blast throws less far (his airbending turns from
keeping distance to attack). `/enlightened tetherless` sets or clears it.

Default keys: R Air Blast, Sneak+R Air Blades, G Air Shield, Sneak+G Suffocation, V Air Jump, Sneak+V Flight, B Air
Scooter, Sneak+B Spirit Projection, N Tornado.

**Looks:** Tenzin (default), Zaheer, Jinora, Monk Gyatso, Jae and Kartik of the Seven Havens, your own skin.

## 5. Waterbending

The first art is **Water Whip**. The sub-arts: **ice** (3 points spent), **blood** (9).

**Water (added).** Every water ability spends one unit of water. The bender **carries** water: near any water (a
block within `water.waterSourceRange`, 6) his carried water fills up, one unit a half second, to **3 / 4 / 6** units
(by the level of the whip; the HUD shows them). Away from water the unit comes from his **Water Skin** (one use of
its ten) and, for a bender with **Plant Water**, from the plants round him (within 3 / 4 / 6), which wither (grass to
dead bushes, leaves and flowers gone, a grass block to dirt). In water, or next to it, nothing is spent.

| # | Ability | Kind | How it works |
|---|---|---|---|
| 1 | **Water Whip** | press | A whip of water lashes what he looks at within **8 / 10 / 12**: `water.waterWhipDamage` (3) + level damage, a push, and the target's fire goes out. Cooldown 0.8 s. The whip's tick keeps the carried water filled. |
| 2 | **Healing** | press | Heals himself, or the player, pet or villager he looks at within 6: `water.waterHeal` (4) × **1 / 1.5 / 2** health at once and regeneration for 3 s; washes poison and wither away and puts out fire. Cooldown 8 s. |
| 3 | **Octopus Form** | toggle | Eight arms of water round him: he takes **20% / 30% / 40%** less damage, every 1.5 s the arms strike the monsters within 3 blocks (1 + level damage) and whenever he hits someone the arms lash everyone else within 3.5. Spends a unit every 10 s. |
| 4 | **Water Spout** | toggle | Over water (within 3 blocks below) a spout lifts him up to **6 / 12 / 20** blocks above it; he moves on it, Sneak brings him down; whoever stands in the column is thrown and hurt **3 / 5 / 7** every second. Ends over dry ground (with slow falling). |
| 5 | **Water Arms** | toggle | Arms of water on his hands: **+2 / +3 / +4** reach and **+1 / +2 / +3** attack damage. |
| 6 | **Plant Water** | passive | The water of plants (above). |
| 7 | **Ice Knives** | press, ice | **1 / 2 / 3** knives of ice: `water.iceKnifeDamage` (4) each and **frozen** (70% slower, shivering, no jumping) for 2 / 3 / 4 s. Cooldown 1.2 s. |
| 8 | **Freeze** | press, ice | The creature he looks at (within 12) is frozen for **3 / 5 / 7 s** and takes 2; or the water he looks at freezes to ice in a radius of **2 / 3 / 4** for 30 s. Cooldown 6 s. |
| 9 | **Snow Throw** | press, ice | Three snowballs: 1 + level damage and slowness. Cooldown 1 s. |
| 10 | **Ice Wall** | press, ice | A wall of ice in front of him, **3 / 5 / 7** wide, 3 high, for `earth.wallSeconds`. A fire ball that hits ice melts it. |
| 11 | **Bloodbending** | press, blood | Limits: below mastery only under a **full moon** at night (`water.bloodNeedsFullMoon`); the grip is taken **up close** (the target within 4 blocks); **a stronger or equal waterbender is never held**; PvP for players. Held, the creature is a puppet: it cannot move, jump or bend, and hangs three blocks in front of him wherever he looks, for `water.bloodSeconds` (6) × **1 / 1.5 / 2 s** (or until it is 12 blocks away). Press again to **throw** it (2 + level damage). Cooldown 20 s after the grip. |
| 12 | **Take Bending** | press, blood | A **master** of bloodbending (level 3), holding a bender, takes his bending: for a weak bender (fewer than 4 points spent) `water.suppressNoviceMinutes` (10) minutes, for a skilled one `water.suppressSkilledSeconds` (90). The victim's abilities end and answer no key; the HUD counts the seconds. Cooldown 60 s. |

Default keys: R Water Whip, Sneak+R Ice Knives, G Healing, Sneak+G Freeze, V Octopus Form, Sneak+V Water Arms, B Water
Spout, Sneak+B Ice Wall, N Bloodbending, Sneak+N Take Bending; Snow Throw waits for a slot.

**Looks:** Katara (default), Amon (Noatak, the mask), Ming-Hua, Master Pakku in the White Lotus robes, your own skin.

## 6. Firebending

The first art is **Firebender's Skin** (passive). The sub-art: **lightning** (6 points spent).

| # | Ability | Kind | How it works |
|---|---|---|---|
| 1 | **Firebender's Skin** | passive | Fire (the world's and other benders') hurts him **50% / 75% / 100%** less; a master never burns, and from level 2 the sky's lightning does not strike him. |
| 2 | **Fire Ball** | hold | A tap throws a **small** ball (3 damage, 2 s of burning); a key held 8 ticks or more throws the **big** one: size **1 / 1.5 / 2**, `fire.fireBallDamage` (4) × **1 / 1.5 / 2**, burning **4 / 5 / 6 s**; a ball of size 2 bursts and burns everyone round the one it hits, and a big ball lights a fire where it lands. Cooldown 0.6 s (small) / 1.5 s (big). **Azula's fire is blue.** |
| 3 | **Fire Fist** | hold | A stream of flame from both hands while the key is held, **6 / 8 / 10** blocks long, `fire.fireStreamDps` (4) × **1 / 1.3 / 1.6** damage a second (a continuous flame: no damage cooldown), burning 3 s. **A master who sneaks breathes fire from his mouth**: 3 blocks farther, 50% stronger, wider. |
| 4 | **Fire Whip** | hold | A whip of fire in his hand, **6 / 9 / 12** long, lashes what he looks at four times a second: `fire.fireWhipDamage` (3) + half a point per level, burning 4 s. |
| 5 | **Fire Ring** | press | A spin of fire burns everyone within **3 / 4 / 5**: `fire.fireRingDamage` (4) × **1 / 1.5 / 2**, burning 3 / 4 / 5 s, a push. An adept spins with his legs too (the ring runs along the ground, and a second ring above). Cooldown 8 s. |
| 6 | **Fire Healing** | press | Heals himself or the friend he looks at `fire.fireHeal` (3) × **1 / 1.4 / 1.8** (weaker than water) and burns poison away. Cooldown 10 s. |
| 7 | **Lightning** | press, lightning | He gathers the lightning for a second (sparks at his hand; a master's at both), then it strikes what he looks at within 24: `fire.lightningDamage` (9, four and a half hearts) and **stun** for `fire.lightningStunTicks` (40, 2 s: no moving, no jumping); a **master strikes with both hands** for `fire.lightningTwoHandsDamage` (12, six hearts). At nothing, it strikes the ground and hurts everyone within 2 for 60%. Cooldown `fire.lightningCooldown` (600 ticks) × **1 / 0.85 / 0.67**. |
| 8 | **Lightning Redirection** | toggle, lightning | The stance (sparks at the hand). Lightning that would hit him (a firebender's, or the sky's) is **caught**: a **novice one time in three**, an **adept half the time**, else it burns through him as it would; a **master always**, and sends it on at what he looks at (or back at whoever cast it) with its full damage. |

Default keys: R Fire Ball, Sneak+R Fire Fist, G Fire Whip, Sneak+G Fire Ring, V Lightning, Sneak+V Lightning
Redirection, B Fire Healing.

**Looks:** Zuko after he joined Team Avatar (default), Uncle Iroh in the White Lotus robes, Azula (blue fire), Jeong
Jeong in the White Lotus robes, Fire Lord Ozai, your own skin.

## 7. Looks and colours **(added)**

Every look is a 64x64 skin drawn by `tools/skins.py` from a palette (skin tone, hair and its style, clothes, trims, a
beard, the airbender's arrows, Zuko's scar, Amon's mask, the White Lotus tile) and a pair of colours: the main
colour of the element's energy and its pale heart. Earth is `#6B8E23` / `#C9A96E`, air `#E8C36A` / `#DDF3FF`, water
`#2E86DE` / `#BFE8FF`, fire `#E2531A` / `#FFD65A`; Ghazan's is red-orange, Azula's blue. The skin replaces the
player's own wherever it is drawn (a mixin on `AbstractClientPlayer.getSkin`), unless the look is "your own skin".
The earth and metal armour are a second model inflated past the skin's outer layer with a stone or iron texture
transparent at the eyes.

## 8. Items **(added)**

| Item | Recipe | What it does |
|---|---|---|
| The four scrolls | Section 1 | Awaken a bender; open the Bending Scroll. |
| Water Skin | 4 leather in a pouch shape, a string at the top | Ten uses of water for a waterbender away from any lake. Right-click water (or use it in water) to fill it; in the rain under the open sky it fills one use every 15 s. Its bar is the water left. |
| Air Glider | 2 leather, a breeze rod, 2 sticks (a staff) | In an airbender's hand it opens as he falls and he glides (4.3). It is dyeable: any dye with it in a crafting table colours its wings. |

## 9. Operator commands **(added)**

```
/enlightened element get [player]
/enlightened element set <players> <none|earth|air|water|fire> [look]
/enlightened look <players> <look>
/enlightened points add <players> <points>
/enlightened learn <players> <ability> [level]      (no level: one more)
/enlightened loadout <player> <slot 1-10> <ability|none>
/enlightened tetherless <players> <true|false>
/enlightened reset <players>                         (every level back; the points refunded)
/enlightened info [player]
```

## 10. Server configuration **(added)**

`world/serverconfig/enlightened-server.toml`, synced to clients, with sections `[general]`, `[earth]`, `[air]`,
`[water]` and `[fire]`: every number named in sections 0 to 6 (the points, the gates, the damages, the ranges, the
cooldowns, the times, whether area attacks hurt passive creatures, whether the scrolls are in chests, whether rock
lift tears blocks out, whether the glider is needed, whether a pet's death severs the tethers, whether bloodbending
needs the moon). The *Config* button of Enlightened in the Mods screen opens them.

## 11. How it is built

* **Data.** `BendingData`, one attachment per player (saved, kept on death, synced to everyone who sees him): the
  element, the look, every ability's level, the points, the experience, the kills, the ten slots, the toggles that are
  on, the tetherless flag, the carried water; at runtime the abilities active right now, the cooldowns, the held
  creature and the time bending was taken until.
* **Abilities.** `Ability` is the enum of all 46 with their element, gate and kind (press, hold, toggle, passive);
  `AbilityLogic` is one ability's server code (`key`, `tick`, `end`); `Abilities` the registry each element fills
  (`EarthBending`, `AirBending` with `Projection`, `WaterBending` with `WaterSources`, `FireBending`); `Cast` what a
  call needs (the bender, his data, the level, cooldowns, messages, sounds, sparks). `BendingHandler` routes a key to
  its slot's ability; `BendingTicker` runs every learned ability's tick and the passives (flight, armour, arms,
  scooter).
* **Shared.** `Shots` (aim, area, cone, hurt, push, lines and rings of particles), `Raycasts`, `AreaTargets`,
  `BlockRights` (spawn protection, claims, Adventure, the border), `TempBlocks` (what bending raised and puts back),
  `Mastery` (points, gates, strength, kills), `Loadout`.
* **Entities.** `BendingShotEntity` (a rock, sand, a metal blade, an ice knife, a snowball, a lava shuriken, an
  air blade, a fire ball: held or launched), `TornadoEntity`, `MeditatingBodyEntity`, `FlyingLemurEntity` (section 13).
* **Network.** `AbilityKeyPayload`, `ChangeLookPayload`, `ScrollActionPayload` to the server; `SeismicPayload` to the
  client; protocol version 1.
* **Client.** `KeyBindings`, `ClientEvents` (keys with the Sneak of the press, the air shield spheres), `BendingHud`,
  `ScrollScreen`, `SuitSkins` and its mixin, `ShellLayer`, `ShotRenderer`, `TornadoRenderer`, `MeditatingBodyRenderer`
  with `MeditatingBodyModel`, `LemurRenderer` with `LemurModel`, `AirShieldRenderer`, `EnergyParticle`, `SeismicGlow`
  and its mixin; the animation layer of section 12 in `client.anim`, with the `PlayerModel`, `PlayerRenderer` and
  `ItemInHandRenderer` mixins; `EnlightenedClientConfig` (animations, reduced motion, effects, first-person hands).
* **Tests.** `src/gametest`: `TestKit` (real players on a fake connection, ticked by the test) and `EnlightenedTests`
  (the scrolls, the points, learning and the gates, the earth wall, rock throw, quicksand's strength rule, the air
  blast, spirit projection, the water whip's water, bloodbending's moon and closeness, the fire ball, lightning,
  the firebender's skin, a suppressed bender; bending hurting animals by default, the gesture tables being well
  formed, the animation maths settling, the lemur being tamed and sitting). `./gradlew runGameTestServer`.
* **Assets.** The skins, the shells, the lemur's texture and the item icons are generated by `tools/skins.py`; the 39
  sounds by `tools/sounds.py` (synthesised, no samples). Lang: `en_us`, `he_il`.

## 12. Animation **(added in 1.1)**

The benders act their bending, in the spirit of the show's martial arts and on the frame of Enhanced's animator:
nothing is assigned, everything is blended, so no pose ever snaps.

| | |
|---|---|
| The four styles | Earthbending is rooted and heavy (Hung Gar): stomps that sink the body, fists and palms that drive straight through. Airbending is circular and light (Ba Gua): spirals, spins and sweeps, the weight never planted. Waterbending flows (Tai Chi): curves that gather and release, the weight shifting foot to foot. Firebending is sharp (Northern Shaolin): straight punches and kicks snapping from the hips, the body pressing forward. Each nation also walks and sprints with its own character (the earthbender heavy, the airbender bouncy, the waterbender with a swing of the shoulders, the firebender leaning in). |
| Gestures | Every cast, hold, toggle and landing plays a short keyed timeline (`Gesture`, keys in `EarthGestures`, `AirGestures`, `WaterGestures`, `FireGestures`): both arms (absolute, additive, by world elevation, or **aimed** in three dimensions at the look or at the thing held), the legs, the head, the shoulders' twist, the body's lean, roll, dip and spin. A gesture's weight rises with an ease-out, holds and falls with an ease-in-out; one of each group plays at a time. The local player's gesture starts the moment the key goes down and is confirmed by the server's data rather than played twice; everyone else's starts on the edge in the synced data (a cooldown that begins, a hold that starts or ends, a toggle that flips, the ground after a fall). |
| Stances | A pose held while a state lasts, with an eased weight (`Stance`, rows in `Stances`): the fire fist's alternating punches, fire breathed, the whip's lashing arm, a fire ball gathering at the hip, lightning circling and coming to point, the open palm under a lifted rock, the clawed hands of bloodbending, the fist of suffocation, the air shield's circling arms, the scooter's seat, the glider's grip, the octopus form's sway, the spout's balance, water arms, the lightning stance, seismic sense, the armour's heaviness. Aimed stances point the hands at the target on a leaning body. |
| The body | Tetherless flight leans the body into its speed (hover, cruise, sprint poses blended by speed, with drift and flutter) and banks it into turns; the glider lies nearly flat on the wind; the scooter leans into the ride; a sprint leans with the nation's character. Lean and bank are springs on the render pose stack (`BodyLean`), so armour, items and layers follow and the name tag does not; a stomp or a landing dips the body on a damped spring. |
| First person | The local player's hands move with the stances and gestures (`FirstPersonArms`): each gesture's first-person row is read from its own arm keys, so every gesture of the tables moves the hands; the empty off hand is drawn when a pose really uses it. |
| Effects | Flames lick the fists of a fire fist, sparks crawl between the hands of a lightning charge, dust flies at an earthbender's stomp, mist comes off water arms, wisps round the air shield, motes gather into the hands before a wall rises (`GestureEffects`), all from where the model really has hands and feet this frame (`HandTracker`). |
| Everything else | The flying lemur blends its ground, flight, glide and perch poses and banks into its turns; the tornado is a drawn funnel of twisting bands; every shot pops into being and tumbles, with a trail; the meditating body sits in the lotus and breathes; the air shield breathes; the HUD flashes a cast and drains a bar under each cooldown. |
| Switches | The client config (`enlightened-client.toml`, also in the mod's config screen): `animations`, `reducedMotion` (no trembles or drift), `effects`, `firstPersonHands`. |

## 13. The flying lemur **(added in 1.1)**

Inspired by the *Flying Lemur* Avatar add-on: a small tameable winged lemur, like Momo.

| | |
|---|---|
| Where | Jungles, bamboo jungles, cherry groves, meadows and flower forests, in small groups (`neoforge:add_spawns`). A spawn egg in the creative tab. |
| Taming | Feed it apples, sweet berries, glow berries, melon or a golden apple (`#enlightened:lemur_food`); one feeding in three tames it. A tamed lemur follows its owner, flies to keep up, sits when right-clicked and stands again, can be leashed when standing, and breeds with the same food. Babies are small. |
| Life | It flies and glides (no fall damage), chirps, scolds when hurt, twitches its ears, and perches. |

## 14. The arts of 1.2 **(added in 1.2)**

Reverse-engineered from ProjectKorra (Bukkit), *Avatar: Legend Reforged* (NeoForge 1.21.1), *Avatar: The Four
Elements* (Forge 1.20.1) and the Bedrock *Avatar Legends* add-on, and rebuilt on Enlightened's frame (an
`AbilityLogic` per art in `EarthMore`, `AirMore`, `WaterMore`, `FireMore`; numbers *novice / adept / master*).

| Element | Ability | Kind | How it works |
|---|---|---|---|
| Earth | **Shockwave** | press | A stomp: everyone within 4.5 / 6 / 8 blocks hurt, thrown back and shaken; the ground ripples (`Fx.SHOCKWAVE`). Cooldown 12 / 10 / 8 s. |
| Earth | **Catapult** | press | The earth flings him the way he looks (1.3 / 1.7 / 2.1), the landing soft (`SoftLanding`). Cooldown 6 / 5 / 4 s. |
| Earth | **Earth Grab** | press | The ground closes round the legs of the target (held 3 / 5 / 7 s); aimed at nothing, the loose items come to him. Cooldown 8 s. |
| Earth | **Earth Tunnel** | hold | The earth and sand he looks at are dug away (wider for a master), only where he may break. |
| Earth | **Earth Step** | passive | The element's step (§16). |
| Earth | **Extraction** | press, metal | The raw metal torn out of the ore he aims at: 1 / 2 / 3 pieces, the ore left stone. Cooldown 4 / 3 / 2 s. |
| Air | **Air Suction** | press | Pulls what he looks at (10 / 14 / 18) to him: creatures, arrows, items. Cooldown 3 s (1.5 s empty). |
| Air | **Air Burst** | press | A sphere of air (5 / 7 / 9 blocks) throws everyone back. Cooldown 10 / 8 / 6 s. |
| Air | **Air Spout** | toggle | A column of wind holds him 6 / 10 / 15 blocks up; Sneak to come down. |
| Air | **Air Agility** | passive | Faster on foot; from level 2 higher jumps. |
| Water | **Water Shield** | hold | A dome of water (2 / 2.4 / 2.8) that turns arrows and takes most of each blow (`WaterMore.shieldAbsorb` in `CommonEvents`), drinking his water; broken, it rests 3 s. |
| Water | **Water Bubble** | toggle | Air under water for him and friends within 1.5 / 2.5 / 3.5. |
| Water | **Phase Change** | press, ice | Water he aims at freezes (2 / 3 / 5 blocks, 30 / 60 / 120 s, `TempBlocks`); ice and snow melt. Cooldown 6 / 5 / 4 s. |
| Water | **Frost Breath** | hold, ice | A cone of frost (5 / 7 / 9) that slows and then freezes (2 / 3 / 4 s, `FROZEN`). |
| Water | **Plant Tether** | press | A vine from the plants within 5 / 7 / 9 lashes the target, slows it and drags it to them; aimed at nothing, pulls him to the plant. Cooldown 8 / 6 / 5 s. |
| Water | **Fast Swim** | passive | Swift in water; an adept breathes longer, a master always. |
| Fire | **Fire Jet** | press | Flames from the feet throw him the way he looks for 10 / 14 / 18 ticks; soft landing. Cooldown 5 / 4 / 3 s. |
| Fire | **Fire Shield** | hold | A dome of fire (2 / 2.4 / 2.8) that burns arrows and bending away and scorches who steps in (`FireMore.shieldAbsorb`); broken, it rests 3 s. |
| Fire | **Wall of Fire** | press | A `FireWallEntity` where he looks, `fire.fireWallSeconds` (6) × 1 / 1.4 / 1.8: burns and throws back who crosses, deflects projectiles. Cooldown 12 / 10 / 8 s. |
| Fire | **Blaze** | press | An arc of fire over the ground ahead (6 / 8 / 10), lighting it for 40 / 60 / 80 ticks and burning who stands in it 3 / 4 / 5 s. Cooldown 8 / 7 / 6 s. |
| Fire | **Heat Control** | toggle | Melts snow and ice within 2 / 3 / 4, no freezing or burning, (adept) boils the water at his feet. |
| Fire | **Illumination** | toggle | A flame on the hand: light 10 / 13 / 15 (a light block that follows him). |
| Fire | **Combustion** | press, combustion (`fire.combustionGate` 9) | A breath of focus (16 / 13 / 10 ticks, third eye glowing), then a beam from the forehead explodes where it lands (24 / 32 / 40 away): `fire.combustionDamage` (10) × 1 / 1.4 / 1.8 at the heart; `combustionBreaksBlocks`. Cooldown 30 / 24 / 18 s. |

Combustion is a gate of its own (`Gate.COMBUSTION`, `Mastery`), like lightning.

## 15. Combos **(added in 1.2)**

ProjectKorra's `ComboManager`, rebuilt: `Combos` remembers each bender's last casts that went off (`BendingHandler`
records a cast after its logic ran) and, when the tail of the list matches a combo's sequence within
`avatar.comboWindowTicks` (60) of each other, performs it and clears the list. Twister (Air Shield, Tornado, Air
Blast), Air Sweep (Air Blades ×2, Air Burst), Earth Dome (Earth Wall, Shockwave), Earth Pillars (Shockwave, Earth
Pillar ×2), Ice Wave (Water Spout, Freeze), Ice Bullets (Ice Wall, Ice Knives ×2), Fire Kick (Fire Ball ×2, Fire
Ring), Fire Spin (Fire Shield, Fire Ring). The combo plays its own gesture (`Gesture.COMBO`) and a message.

## 16. Steps **(added in 1.2)**

*Legend Reforged*'s STEP form: the client notices a double tap of a movement key or Jump within 7 ticks
(`ClientEvents.handleSteps`, `StepPayload`); the server (`Steps.step`) dashes a bender who learned his element's step
(0.9 / 1.1 / 1.3 blocks a tick, or a hop) with a soft landing and the element's own `Fx.STEP`; cooldown 2 / 1.5 / 1 s;
`avatar.steps` turns them off.

## 17. Chakras and the Avatar State **(added in 1.2)**

The Bedrock add-on's seven-tier chakra system and temporary invincibility, with ProjectKorra's AvatarState
(cleared cooldowns, stronger bending, the low-health trigger). `Chakras`: the Meditate key (`MeditatePayload`,
default M) sits the bender in the lotus (`Stance.MEDITATE`, the meditating body); moving, swinging or falling ends it.
Every `avatar.chakraMeditationSeconds` (60; a third on a block of `#enlightened:meditation_point`, the **Meditation
Point** block) opens the next chakra if `spent() >= n × avatar.chakraPointsEach` (2); meditation also earns experience
(`avatar.meditationXpSeconds`). Gifts: chakras 1 and 4 a heart each (an attribute modifier), 2 and 5 cooldowns −5%
each (`Cast.cooldown`), 3 and 6 damage +5% each (`Shots.hurt`). With all seven open, holding the meditation a breath
longer (or, `avatar.avatarStateOnLowHealth`, a lethal hit in `CommonEvents`) enters the Avatar State for
`avatar.avatarStateSeconds` (12): invulnerable to damage, damage ×2, cooldowns ×0.5, cooldowns cleared, speed +30%,
absorption, no drowning, burning or falling, `Fx.AVATAR_RISE` and a push on everyone near, a broadcast; then
`avatar.avatarStateCooldownSeconds` (300). `BendingData` saves chakras, meditation progress and the cooldown;
`/enlightened chakras` sets the count; the HUD shows them.

## 18. World effects **(added in 1.2)**

Every element is drawn as the element itself (`client.fx`): water as ribbons, tubes, domes and discs of the game's
water textures (32 frames of `water_still`, `water_flow`, tinted), fire as `fire_0` / `soul_fire_0` (Azula) and
`lava_still`, ice and snow as their blocks, air and lightning as additive light (`RenderType.lightning()`), earth as
rippling rings of the ground's colour. The server sends one-shot effects as `FxPayload` (`Fx` kinds: a whip's lash,
a healing's glow, a freeze, a lightning bolt, a redirect, a blast, a shockwave, the steps, a vine, a blaze's arc, a
combustion beam, an ice wave, a fire spin, an extraction, the Avatar's rise, a chakra's opening); the client draws
sustained effects from the synced data (`WorldFxRenderer`: water arms, the octopus's tentacles, the spout's column,
fire streams and whip, the lightning gathering, bloodbending's strands, the air shield, the glider's wings, the
wind of flight, the scooter's ball, the water and fire domes, the frost breath, the fire jet, the combustion glow,
the Avatar's aura). Geometry is built in `FxGeometry` (tubes along splines, cones, spheres, discs, ring bands,
billboards, glow blobs, and camera-facing *puffs* along a path) into a buffer source of its own with a fixed buffer
per type, and drawn after the particles (`RenderLevelStageEvent.AFTER_PARTICLES`). Every stream carries puffs along
its length and the streams from the hands fan a little outward, because the bender's own third-person camera looks
straight along his look: a bare tube seen end-on is a ring, and a stream on the view axis hides behind his body. The air scooter is a rolling ball of air under a body lifted off the
ground (`Stances.drop`), rolled by a quaternion with the ride.

