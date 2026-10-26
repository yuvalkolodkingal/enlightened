# Earthbending

[עברית](Earthbending-HE)

*"You're going to have to face it head-on!"* The earthbender throws rock torn from the ground, lifts and holds it,
wears a shell of earth, raises pillars and walls and feels every step through the ground; then he learns **sand**,
**lava** and **metal**. Read the [Earth Scroll](Becoming-a-Bender) to begin; you start with **Rock Throw**.

Numbers are *novice / adept / master*. Names in `code` are [server options](Server-Configuration).

## Earth

| Ability | Kind | What it does |
|---|---|---|
| **Rock Throw** | press | Rocks tear out of the earth in front of you (the ground there, drawn as that block) and fly at what you look at: **1 / 2 / 3** at once, `rockDamage` (4) + level damage each, knocking the target back. Needs earth within 3 blocks under the spot ahead of you. Cooldown `rockCooldown` (1 s). |
| **Rock Lift** | press | Look at a block of earth up to `rockRange` (24) away and press: it is torn out and floats in front of you, orbiting with the others you hold: **1 / 2 / 3** at once. Press while looking at no earth (or holding the most you can) to throw them all at what you look at: 6 + level damage. A block torn out drops its item when it shatters or is set down (`rockLiftTakesBlocks` false conjures the rock and leaves the ground). The hold ends if you die, log out or lose the element. |
| **Earth Armor** | toggle | A shell of rock over your whole body with only the eyes open: **+6 / +10 / +14 armour**, **+2 / +4 / +6 toughness**, 30% knockback resistance; but **`earthArmorSlow` (30%) slower**, 5% less per level. Needs earth under your feet to put on. |
| **Earth Pillar** | press | The ground rises under your feet as a pillar **3 / 5 / 8** blocks high and carries you up to high places; it stands `general.raisedBlockSeconds` (10 s) and crumbles. Cooldown 6 s. |
| **Earth Wall** | press | A wall of the ground's block rises 2.5 blocks ahead of you, **3 / 5 / 7 wide** and 3 high, for `wallSeconds` (10 s): rocks, arrows, blasts and whips stop at it. Cooldown `wallCooldown` (10 s). |
| **Seismic Sense** | toggle | Standing on the ground (not flying), every creature within **12 / 20 / 32** blocks that touches the ground or the water is outlined for you through walls. |

## Sand (3 points spent)

| Ability | Kind | What it does |
|---|---|---|
| **Sand Wall** | press | As the earth wall, of sand. Needs sand (sand, red sand, sandstone, gravel, soul sand) within 6 blocks. |
| **Sand Lift** | press | Sand swirls up round you and flies at your enemy as **1 / 2 / 3** clouds: 2 + level damage and **blindness** for 3 s. Needs sand within 4 blocks. Cooldown 1.5 s. |
| **Quicksand** | press | The sand under the creature you look at (within 16) turns to quicksand: it sinks (pulled down, 80% slower, cannot jump, a little hurt each second) for **4 / 6 / 8 s**. An adept turns the earth under the target into sand for the while, so it works anywhere earth is. **A stronger or equal earthbender is never sunk** (your strength is the points you spent). Cooldown 10 s. |

## Lava (6 points spent)

| Ability | Kind | What it does |
|---|---|---|
| **Lava Path** | press | The ground ahead of you, toward where you look, melts into a path of lava **4 / 7 / 10** blocks long for `lavaSeconds` (8 s), then sets back to what it was. You get fire resistance for the while: you do not burn in your own lava. Cooldown 15 s. |
| **Lava Shuriken** | press | A spinning disc of lava at what you look at: 5 + level damage and burning for **3 / 4 / 6 s**; where it lands it may light a fire. Cooldown 2.5 / 2 / 1.5 s. |
| **Lavabender's Skin** | passive | Fire and lava hurt you **25% / 50% / 75%** less. |

## Metal (9 points spent)

Metalbending needs **metal**: a metal block within 8 blocks (iron, gold, copper, netherite blocks, ores, anvils,
chains, bars, iron doors, rails, cauldrons, lanterns...) or metal in your inventory (ingots, nuggets, raw metal, iron
tools and armour, a bucket, rails...).

| Ability | Kind | What it does |
|---|---|---|
| **Metal Wall** | press | As the earth wall, of iron blocks. |
| **Metal Blades** | press | **1 / 2 / 3** blades of metal fly at what you look at: `metalBladeDamage` (6) + level damage each. Cooldown 1.5 s. |
| **Metal Armor** | toggle | Full armour of metal over the whole body but the eyes: **+16 / +20 / +24 armour**, **+6 / +8 / +10 toughness**, 60% knockback resistance, but **40% slower**, 5% less per level. It replaces the earth armour. |
| **Metal Pull** | press | The metal in the ground round you comes out of the rock into your pack: every iron, gold and copper ore and ancient debris within `metalPullRadius` (6) + 3 per level turns to stone and gives its metal (raw iron, raw gold, 2-5 raw copper, 2-5 gold nuggets, netherite scrap), and loose metal items on the ground come too; **8 / 16 / 32** things at most, only where you may break. Cooldown 8 s. |
| **Wreck** | press | Tear metal devices apart: look at one (within 12) and press, and it and the devices within 2 blocks of it break, **1 / 3 / 6** at once: furnaces, blast furnaces, smokers, pistons, hoppers, dispensers, droppers, observers, rails, iron doors and trapdoors, bars, chains, anvils, cauldrons, lightning rods, weighted plates, lanterns, bells, grindstones, smithing tables, stonecutters, copper bulbs and grates, crafters, iron and copper blocks (`wreckDrops`: whether they drop their item). A minecart in your view breaks; an iron golem takes 6 + 2 per level damage. Cooldown 5 s. |

## Default keys

R Rock Throw, ⇧R Rock Lift, G Earth Armor, ⇧G Metal Armor, V Earth Pillar, ⇧V Seismic Sense, B Earth Wall, ⇧B Sand
Wall, N Quicksand, ⇧N Lava Shuriken. Lava Path, Metal Blades, Metal Wall, Sand Lift, Metal Pull and Wreck wait for a
free slot: see the [Bending Scroll](Points-and-the-Bending-Scroll).

## Looks

Toph Beifong (default), King Bumi, Lin Beifong, Suyin Beifong, Ghazan, Bolin, your own skin. See [Looks](Looks).
