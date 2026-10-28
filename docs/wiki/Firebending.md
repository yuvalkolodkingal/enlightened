# Firebending

[עברית](Firebending-HE)

*"Fire is the element of power."* The firebender hardly burns, throws balls of fire, streams it from his fists (a
master breathes it), lashes with a whip of fire, spins a ring of it, heals with its warmth, and learns **lightning**
and its redirection. Read the [Fire Scroll](Becoming-a-Bender) to begin; you start with the **Firebender's Skin**.

Numbers are *novice / adept / master*. Names in `code` are [server options](Server-Configuration).

## Fire

| Ability | Kind | What it does |
|---|---|---|
| **Firebender's Skin** | passive | Fire (the world's and other benders') hurts you **50% / 75% / 100%** less. A master never burns; from level 2 the sky's lightning does not strike you. |
| **Fire Ball** | hold | **Tap** the key: a small ball (3 damage, 2 s of burning) for when you do not want to do much harm. **Hold** it (half a second or more): the big ball, size **1 / 1.5 / 2**, `fireBallDamage` (4) × **1 / 1.5 / 2** damage, burning **4 / 5 / 6 s**; a master's ball bursts and burns everyone round the one it hits, and a big ball lights a fire where it lands. Cooldown 0.6 s (small) / 1.5 s (big). |
| **Fire Fist** | hold | A continuous stream of flame from both hands while you hold the key, **6 / 8 / 10** blocks long: `fireStreamDps` (4) × **1 / 1.3 / 1.6** damage a second (no damage cooldown), burning 3 s. **A master who sneaks breathes fire from his mouth**: 3 blocks farther, 50% stronger, wider. |
| **Fire Whip** | hold | A whip of fire in your hand, **6 / 9 / 12** blocks long, lashes what you look at four times a second: `fireWhipDamage` (3) + half a point per level, and everyone it touches burns 4 s. |
| **Fire Ring** | press | A spin of fire from your hands burns everyone within **3 / 4 / 5**: `fireRingDamage` (4) × **1 / 1.5 / 2**, burning **3 / 4 / 5 s**, a push. An adept spins with his legs too: the ring runs along the ground, and a second ring above. Cooldown 8 s. |
| **Fire Healing** | press | Heal yourself or the friend you look at with the warmth of fire: `fireHeal` (3) × **1 / 1.4 / 1.8** health and regeneration for 2 s; poison is burned away. Weaker than water healing. Cooldown 10 s. |

## Lightning (6 points spent)

| Ability | Kind | What it does |
|---|---|---|
| **Lightning** | press | You gather the lightning for a second (sparks at your hand; a master's at both), then it leaves your hand as a **bolt** (drawn as Thor's lightning beam: a white-hot core, an electric-blue body and halo, strands crackling round it and forks branching off) and strikes what you look at within 24: `lightningDamage` (9, four and a half hearts) and a **stun** of `lightningStunTicks` (2 s: no moving, no jumping). **A master strikes with both hands**: `lightningTwoHandsDamage` (12, six hearts). At nothing in sight it strikes the ground and hurts everyone within 2 blocks for 60%. It takes long to recharge: `lightningCooldown` (30 s) × **1 / 0.85 / 0.67**. |
| **Lightning Redirection** | toggle | Take the stance (sparks at your hand). Lightning that would hit you, a firebender's or the sky's, is **caught**: a **novice one time in three**, an **adept half the time**, else it burns through you as it would have; a **master always**, and he sends it on with its full damage at what he looks at, or back at whoever cast it. |

## The colour of your fire: normal, blue or rainbow

**Sneak + Change Look (H)** changes the colour of your fire: the colour of fire, **blue** (Azula's), or **rainbow**
(bands of every hue running through the flames, and sparks of every colour). Every flame of yours takes it: the fire
ball, the fist, the whip, the ring, the shield, the jet, the wall. The numbers are the same. Until you choose, your
look decides (the look **Azula** burns blue). An operator can set it with `/enlightened firecolor <players> <normal|blue|rainbow|look>`.

## Default keys

R Fire Ball, ⇧R Fire Fist, G Fire Whip, ⇧G Fire Ring, V Lightning, ⇧V Lightning Redirection, B Fire Healing.

## Looks

Zuko after he joined Team Avatar (default), Uncle Iroh in the White Lotus robes, Azula, Jeong Jeong in the White
Lotus robes, Fire Lord Ozai, your own skin. See [Looks](Looks).

## New in 1.2

| Ability | Kind | What it does |
|---|---|---|
| **Fire Jet** | press | Flames from your feet throw you the way you look for **10 / 14 / 18** ticks; you land softly. Cooldown **5 / 4 / 3 s**. |
| **Fire Shield** | hold | A dome of fire round you (**2 / 2.4 / 2.8** blocks) that burns arrows and bending away and scorches whoever steps in. A broken dome rests 3 s. |
| **Wall of Fire** | press | A wall of fire rises where you look and burns `fireWallSeconds` (6 s) × **1 / 1.4 / 1.8**, burning and throwing back whoever crosses it. Cooldown **12 / 10 / 8 s**. |
| **Blaze** | press | An arc of fire runs **6 / 8 / 10** blocks over the ground ahead, setting it alight for a while and burning whoever stands in it for **3 / 4 / 5 s**. Cooldown **8 / 7 / 6 s**. |
| **Heat Control** | toggle | Your heat melts the snow and ice within **2 / 3 / 4** blocks, keeps you from freezing and burning, and (an adept) boils the water at your feet away. |
| **Illumination** | toggle | A flame on your hand lights your way (light **10 / 13 / 15**). |
| **Fire Step** | passive | A double tap of a direction dashes you in a flare of fire; a double tap of Jump hops. Cooldown **2 / 1.5 / 1 s**. |

## Combustion (`combustionGate`, 9 points spent)

| Ability | Kind | What it does |
|---|---|---|
| **Combustion** | press | After a breath of focus (**16 / 13 / 10** ticks, the third eye glowing) a beam leaves your forehead and explodes where it lands, up to **24 / 32 / 40** blocks away: `combustionDamage` (10) × **1 / 1.4 / 1.8** at the heart, less further out. `combustionBreaksBlocks` (false) lets it wreck the ground. Cooldown **30 / 24 / 18 s**. |

Every fire art is drawn as real fire: the fist's streams, the whip, the ball, the ring, the walls and domes are the
game's fire texture; Azula's fire is blue soul fire.

## New in 1.3

| Ability | Kind | What it does |
|---|---|---|
| **Palm Forge** | hold | The fire in your hand cooks what you hold: food, an ore, raw metal, sand, anything a furnace, a blast furnace, a smoker or a campfire would take, **one piece every 3 / 2.25 / 1.5 s**, with the furnace's experience. |

**Blue fire burns blue.** Azula's fire leaves **Blue Fire** on the ground where it lands (the blaze, a big fire
ball): a blue flame that stands wherever fire would, does not spread, and dies down by itself; whoever it burns, and
whoever her fist, whip, ring, wall, shield or combustion sets alight, **burns with blue flames**. The fire whip is the
show's rope of fire: thick, with a yellow heart, trailing the arm's swing in loops.
