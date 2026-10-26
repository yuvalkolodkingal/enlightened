# Airbending

[עברית](Airbending-HE)

*"Air is the element of freedom."* The airbender throws everyone back with a wave of air, hides in a sphere of it,
jumps high and glides on his **Air Glider**, cuts with blades of air, leaves his body in meditation, breathes under
water, rides a ball of air and sends tornadoes; and the one who lets go of his earthly tethers flies, and takes
his enemy's breath. Read the [Air Scroll](Becoming-a-Bender) to begin; you start with **Air Blast**.

Numbers are *novice / adept / master*. Names in `code` are [server options](Server-Configuration).

## Air

| Ability | Kind | What it does |
|---|---|---|
| **Air Blast** | press | A wave of air throws everyone in a cone ahead of you (**6 / 9 / 12** blocks) back: `airBlastPush` (1.6) × **1 / 1.5 / 2.2** of push and 1 damage; items and arrows fly with it. Cooldown `airBlastCooldown` (1 s). |
| **Air Shield** | toggle | A sphere of air **2.5 / 3 / 3.5** blocks round you (you see it as a faint, swirling sphere): creatures are kept out and arrows turn back at whoever shot them. It takes the blows meant for you (everything but falls, starving, drowning and the like): `airShieldStrength` (10) × **1 / 1.5 / 2** damage, and then it **breaks and bursts**: everyone within the radius + 2 is thrown back and takes `airShieldBurstDamage` (7, three and a half hearts). The shield rests 15 s after it breaks, and costs a little food while it is up. |
| **Air Jump** | press | Shoot air at the ground and fly up: **0.9 / 1.2 / 1.5** of lift (about 4, 7 and 11 blocks), with slow falling for **3 / 4 / 5 s**; the creatures next to you go up too. Cooldown 1.5 s. **Gliding:** fall with the [Air Glider](Items-and-Crafting) in either hand (not sneaking) and the air carries you forward at **0.55 / 0.65 / 0.75** blocks a tick and down slowly; look down to dive. No fall damage. (`gliderNeeded` false lets every airbender glide without it.) |
| **Air Blades** | press | **1 / 2 / 3** cutting blades of air at what you look at: `airBladeDamage` (5, two and a half hearts) each, no knockback. Cooldown 1 s. |
| **Spirit Projection** | press | Sit still on the ground and press: your spirit leaves the body and you are a **spectator** for `projectionSeconds` (20) × **1 / 2 / 3** seconds, free to fly through walls and look into caves, within `projectionRange` (32) × **1 / 2 / 4** blocks of the body (farther, the tether pulls you back). The body stays behind as a meditating figure in your skin: **if anything hurts it, you return at once and the blow lands on you.** Press again to return early. Cooldown 20 s. |
| **Air Bubble** | passive | Under water, a bubble of air round your head: a novice's air never runs below six bubbles, an adept breathes freely, a master swims like a dolphin. |
| **Air Scooter** | toggle | Ride a ball of air: **60% / 90% / 120%** faster, you step up blocks, and you never fall hard. |
| **Tornado** | press | A small tornado walks ahead of you for **5 / 7 / 9 s**, **1.5 / 2.5 / 3.5** blocks wide: it lifts, spins and throws everything it meets (creatures, items, arrows) and hurts **1 / 1.5 / 2** every half second; at a wall it turns. Cooldown 12 s. |

## The tetherless airbender

An airbender who **kills his own pet** (a wolf, a cat, a parrot or a horse he tamed) lets go of his earthly
tethers, as Zaheer did (`petKillSeversTethers`). From then on:

* **Flight** and **Suffocation** can be learned (they are on the scroll, greyed out until then);
* his blasts and blades do `tetherlessDamageBonus` (2) more damage, his blast throws 30% less far and his tornado
  bites harder: his airbending turns from keeping distance to attack;
* the panel says *Tetherless*. An operator can set or clear it: `/enlightened tetherless`.

| Ability | Kind | What it does |
|---|---|---|
| **Flight** | toggle | Creative-like flight: press the key to switch it on and take off, again to land (double-tapping Space works too). Flying speed 0.08. |
| **Suffocation** | hold | Hold the key on the creature you look at (within 8): the air leaves its lungs; when it has none left it takes 2 damage a second, as drowning, and it is slowed. Ends if it gets more than 12 blocks from you. Cooldown 6 s. |

## Default keys

R Air Blast, ⇧R Air Blades, G Air Shield, ⇧G Suffocation, V Air Jump, ⇧V Flight, B Air Scooter, ⇧B Spirit Projection,
N Tornado.

## Looks

Tenzin (default), Zaheer, Jinora, Monk Gyatso, Jae and Kartik of the Seven Havens, your own skin. See [Looks](Looks).
