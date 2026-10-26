# Points and the Bending Scroll

[עברית](Points-and-the-Bending-Scroll-HE)

## Ability points

Every creature you kill **with your bending** (a rock, a blast, a whip, a flame: any damage of the mod, never a
sword or a bow) earns **experience**: one for a monster, ten for a boss (the Wither, the Ender Dragon, the Warden, an
Elder Guardian), two for another player; animals and villagers give none. Every `general.xpPerPoint` (3) experience
is one **ability point**, with a chime and a message. The panel shows your points and the experience toward the
next.

## Levels

Every ability has **three levels**: novice (★☆☆), adept (★★☆), master (★★★). Level 1 costs **1 point**, level 2
costs **2**, level 3 costs **3** (six in all to master an ability). Almost every number of an ability grows with the
level: how many rocks, how wide the wall, how far the blast throws, how long the grip holds, how much the healing
heals. The element pages list the numbers as *novice / adept / master*.

## Gates: the sub-bending arts

The sub-arts open when you have **spent** enough points on your element (your *strength*):

| Art | Points spent | Option |
|---|---|---|
| Sand (earth) | 3 | `earth.sandGate` |
| Lava (earth) | 6 | `earth.lavaGate` |
| Metal (earth) | 9 | `earth.metalGate` |
| Ice (water) | 3 | `water.iceGate` |
| Blood (water) | 9 | `water.bloodGate` |
| Lightning (fire) | 6 | `fire.lightningGate` |
| Flight and Suffocation (air) | never by points: only a [tetherless](Airbending#the-tetherless-airbender) airbender | |

Your strength also decides who wins when bending meets bending: quicksand never sinks a stronger or equal
earthbender, and bloodbending never holds a stronger or equal waterbender.

## The Bending Scroll (K)

Press **K** (or right-click with your element's scroll) to open it.

* **Left: your abilities.** Every ability of your element, with its stars, the cost of the next level, its gate (in
  red while closed, with the points it asks for) and a **+** button that learns the next level (greyed out when it
  cannot: not enough points, the gate closed, mastered already). *(passive)* marks the abilities that sit on no key.
  Scroll the mouse wheel for more. Click an ability to **select** it: its description appears under the keys.
* **Right: your keys.** Ten slots: each key plain and with Sneak (⇧). Select an ability on the left, then click a
  slot to put it there (an ability sits on one slot at a time; it leaves its old slot). **Right-click** a slot to
  empty it. **Default keys** lays your learned abilities out in the element's default order.

Every ability you learn takes its default slot if that is free, else the first free slot. The panel in the corner
always shows the keys as they are now.

Operators can do all of it with [commands](Commands): `/enlightened points add`, `/enlightened learn`,
`/enlightened loadout`, `/enlightened reset` (every level back, the points refunded).
