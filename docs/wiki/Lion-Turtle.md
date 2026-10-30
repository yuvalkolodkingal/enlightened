# The Lion Turtle

[עברית](Lion-Turtle-HE)

![The lion turtle's head over the sea](images/lion-turtle.jpg)
*The lion turtle's head over the sea*

![The gift of energybending](images/lion-turtle-gift.jpg)
*The gift of energybending*


The lion turtles are the oldest creatures of the Avatar world: as big as islands, the first homes of people, who
gave the first benders their bending. In Enlightened a lion turtle swims in the **deep ocean**: an island of real
blocks some **440 blocks long and 360 wide**, a dome of grass and forest rising 40 blocks above the sea on a shell
rimmed in brown plates, with a great lion's head at its front: a pale face with big amber eyes under heavy brows, a
broad snout and long whiskers, a crown of spined fins and a beard of them, flippers either side. (The size is the
server's choice: `lionTurtleSize`, 1 to 10.)

When the world makes the spot, the island rises from the sea over some seconds, a few rows at a time.

## Walking on it

You may land on it and walk about. While a player is near it, it **swims**: every `lionTurtleMoveSeconds` (40) it
moves one block the way its head faces, carrying everything on it (blocks, chests and their contents, beds, creatures,
you). The swim runs as a wave from its front to its back over some seconds, a row of blocks at a time, so it never
stops the game, and you do not see it go; you only find, after a while, that you are far from where you started.
Where land, ice or shallow water bars its way, it swims to a side, or back. An island nobody is near stays where it
was left.

So that all of it can swim, a lion turtle keeps its own chunks loaded while a player is near it (and lets them go a
little after everyone has left).

The lion turtle cannot be harmed and never leaves (only the void or a Creative player can kill it).

## Its gift

Stand at the front of its shell, beside its neck, and right-click its head. To most it says nothing. To a **Hero of
the Village** (the advancement for saving a village from a raid) it speaks, and gives two arts, placed on your free
keys. As it gave Wan his bending, it raises a flipper high beside its face and reaches out two great claws, tan banded
with black, to your **forehead** and your **chest**; light blooms where they touch, broken rings of it circling the two
points and long rays crossing through them; then the claws draw back.

* **Energybending**: touch a bender within reach and take his bending, for
  * **2 hours** if you are the Avatar (no chakra need be open);
  * **6 minutes** if you are an airbender;
  * **3 minutes** for anyone else (any other bender, or someone who bends nothing).
* **Energy Restore**: touch one whose bending was taken and give it back before its time ends.

The danger: if the energy of the one you touch is **stronger than yours** (more points spent and chakras opened; the
Avatar's counts for more), the energy turns on you. You lose your own bending for as long, and you are stunned. In the
Avatar State no energy is stronger than yours.

## A home

A Hero of the Village may also **build on its shell and live there**, as people lived on the lion turtles before the
Avatar. Nobody else can break or place a block on it (Creative players aside). A bed on the shell swims with it, and
you wake there still.

## Finding one

Lion turtles live in deep oceans, far apart (about one in every 64 by 64 chunks, where the ocean is deep under the whole
island). An operator can raise one with `/enlightened lionturtle` (at sea level where you stand, facing as you face),
`/enlightened lionturtle <pos>` (the sea's surface at that height) or `/enlightened lionturtle <pos> <size>`.
