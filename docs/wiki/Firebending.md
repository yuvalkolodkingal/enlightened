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
| **Lightning** | press | You gather the lightning for a second (sparks at your hand; a master's at both), then it strikes what you look at within 24: `lightningDamage` (9, four and a half hearts) and a **stun** of `lightningStunTicks` (2 s: no moving, no jumping). **A master strikes with both hands**: `lightningTwoHandsDamage` (12, six hearts). At nothing in sight it strikes the ground and hurts everyone within 2 blocks for 60%. It takes long to recharge: `lightningCooldown` (30 s) × **1 / 0.85 / 0.67**. |
| **Lightning Redirection** | toggle | Take the stance (sparks at your hand). Lightning that would hit you, a firebender's or the sky's, is **caught**: a **novice one time in three**, an **adept half the time**, else it burns through you as it would have; a **master always**, and he sends it on with its full damage at what he looks at, or back at whoever cast it. |

## Azula's blue fire

Choose the look **Azula** and every flame of yours is blue: the fire ball, the fist, the whip, the ring. The numbers
are the same.

## Default keys

R Fire Ball, ⇧R Fire Fist, G Fire Whip, ⇧G Fire Ring, V Lightning, ⇧V Lightning Redirection, B Fire Healing.

## Looks

Zuko after he joined Team Avatar (default), Uncle Iroh in the White Lotus robes, Azula, Jeong Jeong in the White
Lotus robes, Fire Lord Ozai, your own skin. See [Looks](Looks).
