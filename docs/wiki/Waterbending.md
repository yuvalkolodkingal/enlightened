# Waterbending

[עברית](Waterbending-HE)

*"Water is the element of change."* The waterbender whips, heals, wraps himself in the octopus form, rides a spout
of water and wears arms of it, and learns **ice** and, under the full moon, **bloodbending**. Read the
[Water Scroll](Becoming-a-Bender) to begin; you start with **Water Whip**.

Numbers are *novice / adept / master*. Names in `code` are [server options](Server-Configuration).

## Water: where it comes from

Every water ability spends **one unit of water**. You **carry** water with you: whenever any water is within
`waterSourceRange` (6) blocks (a lake, a river, a bucket's pool), your carried water fills up (one unit every half
second) to **3 / 4 / 6** units, by the level of your whip; the panel shows them. Away from water:

1. your **[Water Skin](Items-and-Crafting)** gives a unit (one of its ten uses);
2. with **Plant Water** learned, the plants round you (within **3 / 4 / 6**) give it, and wither: grass to dead
   bushes, leaves and flowers gone, a grass block to dirt;
3. else: *No water*.

In water or right next to it, nothing is spent.

## Water

| Ability | Kind | What it does |
|---|---|---|
| **Water Whip** | press | A whip of water lashes what you look at within **8 / 10 / 12**: `waterWhipDamage` (3) + level damage, a push, and the target's fire goes out. The easiest art; it is also what keeps your carried water filled. Cooldown 0.8 s. |
| **Healing** | press | Heal yourself, or the player, pet or villager you look at (within 6): `waterHeal` (4) × **1 / 1.5 / 2** health at once and regeneration for 3 s; poison and wither are washed away, fire put out. Cooldown 8 s. |
| **Octopus Form** | toggle | Eight arms of water round you: you take **20% / 30% / 40%** less damage, every 1.5 s the arms strike the monsters within 3 blocks (1 + level damage), and whenever you hit someone the arms lash everyone else within 3.5. Spends a unit every 10 s. |
| **Water Spout** | toggle | Over water (within 3 blocks below you) a spout lifts you up to **6 / 12 / 20** blocks above it; you move on it, Sneak brings you down; whoever stands in the column is thrown and hurt **3 / 5 / 7** every second. It ends over dry ground (you float down). |
| **Water Arms** | toggle | Long arms of water on your hands: **+2 / +3 / +4** reach and **+1 / +2 / +3** attack damage. |
| **Plant Water** | passive | The water of plants (above). |

## Ice (3 points spent)

| Ability | Kind | What it does |
|---|---|---|
| **Ice Knives** | press | **1 / 2 / 3** knives of ice: `iceKnifeDamage` (4) each, and the target is **frozen** (70% slower, shivering, cannot jump) for **2 / 3 / 4 s**. Cooldown 1.2 s. |
| **Freeze** | press | The creature you look at (within 12) is frozen for **3 / 5 / 7 s** and takes 2; or the water you look at freezes to ice in a radius of **2 / 3 / 4** for 30 s. Cooldown 6 s. |
| **Snow Throw** | press | Three snowballs: 1 + level damage and slowness. Cooldown 1 s. |
| **Ice Wall** | press | A wall of ice ahead of you, **3 / 5 / 7** wide, 3 high, for `earth.wallSeconds` (10 s). Fire is strong against it: a fire ball melts the ice it hits. Water, in turn, puts fire out. |

## Bloodbending (9 points spent)

Three limits:

* below mastery (level 3) it answers only under the **full moon**, at night (`bloodNeedsFullMoon`);
* the grip is taken **up close**: the target must be within **4 blocks**;
* **a stronger or equal waterbender is never held** (your strength is the points you spent); other players only with PvP on.

| Ability | Kind | What it does |
|---|---|---|
| **Bloodbending** | press | The creature or player you look at is yours: it cannot move, jump or bend, and hangs three blocks in front of you wherever you look, for `bloodSeconds` (6) × **1 / 1.5 / 2 s** (or until it gets 12 blocks away). Press again to **throw** it (2 + level damage). Cooldown 20 s after a grip. |
| **Take Bending** | press | Only a **master** of bloodbending. While you hold a bender in your grip, press: his bending is taken away, for `suppressNoviceMinutes` (10) minutes if he is weak (fewer than 4 points spent), for `suppressSkilledSeconds` (90) if he is skilled. His abilities end and no key answers him; his panel counts the seconds. Cooldown 60 s. |

## Default keys

R Water Whip, ⇧R Ice Knives, G Healing, ⇧G Freeze, V Octopus Form, ⇧V Water Arms, B Water Spout, ⇧B Ice Wall,
N Bloodbending, ⇧N Take Bending. Snow Throw waits for a free slot.

## Looks

Katara (default), Amon (Noatak), Ming-Hua, Master Pakku in the White Lotus robes, your own skin. See [Looks](Looks).
