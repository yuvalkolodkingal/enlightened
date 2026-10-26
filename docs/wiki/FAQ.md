# FAQ

[עברית](FAQ-HE)

**I pressed a key and nothing happened.** Look at the panel: a key with `-` has no ability on it. Open the Bending
Scroll (K) and put one there. A grey number after the ability is its cooldown.

**How do I get ability points?** Kill monsters **with your bending** (a rock, a whip, a flame, a blast), not with a
sword or a bow. Every three kills is a point (`general.xpPerPoint`). Bosses give ten.

**Why can't I learn sand / lava / metal / ice / blood / lightning?** They open after you have spent enough points on
your element (3, 6, 9; 3, 9; 6 by default). The scroll shows the gate in red with the points it asks for.

**Why can't I learn Flight?** Only an airbender who let go of his earthly tethers flies: see
[Airbending](Airbending#the-tetherless-airbender).

**My waterbending says "No water".** Stand near water (your carried water fills up), carry a filled
[Water Skin](Items-and-Crafting), or learn Plant Water and draw it from the plants round you.

**Bloodbending does nothing.** Below mastery it needs the **full moon** at night (the first night of a new world is
one; then every eighth). The target must be within four blocks. A stronger waterbender is never held.

**The earth wall / pillar disappeared.** It is meant to: bending raises blocks for a while and puts the old ones
back (`earth.wallSeconds`, `general.raisedBlockSeconds`). Nothing is placed in protected places.

**My glider does not glide.** You must be an airbender, hold the glider in either hand, and fall (not sneaking). A
server can let every airbender glide without it (`air.gliderNeeded`).

**Can I bend two elements? Be the Avatar?** No: one element per player, as in the show for everyone but the Avatar.
An operator can change it (`/enlightened element set`); the old element's levels are gone.

**Can I change my look?** Press **H**; see [Looks](Looks). Azula's look makes your fire blue.

**Is this the Enhanced mod?** It is built on the same frame as [Enhanced](https://github.com/yuvalkolodkingal/Enhanced)
(the superhero mod) and plays the same way, but it is a mod of its own: the two can be installed together, and a
player can be a hero in one and a bender in the other.

**Who made it?** The ideas are Eilon Kolodkin-Gal's; the mod is by Yuval Kolodkin-Gal. Not affiliated with
Nickelodeon or Avatar Studios.

**My bending doesn't hit animals.** It does, unless the server turned `general.hurtPassives` off (it is on by
default since 1.1). Pets, teammates, and Creative or Spectator players are never hit.

**Can I turn the animations off, or calm them down?** Yes: the mod's config screen (Mods, Enlightened, Config) has
**animations**, **reduced motion**, **effects** and **first-person hands**. They are yours alone; the server does not
see them.

**Where are the flying lemurs?** In jungles, bamboo jungles, cherry groves, meadows and flower forests. Feed one an
apple or berries to tame it: see [The Flying Lemur](Flying-Lemur).
