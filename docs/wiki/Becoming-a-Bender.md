# Becoming a Bender

[עברית](Becoming-a-Bender-HE)

A player bends one element, forever (only an [operator command](Commands) changes it). It is chosen **at birth**:
the first time you join a world, your fate is cast: one chance in six you are born **without bending**; otherwise
you are a **firebender**, an **airbender**, a **waterbender**, an **earthbender** or **the Avatar**, alike.

## The Avatar Cycle

* A world has **one Avatar** at a time by default. While it has all the Avatars it may, whoever would have been born
  the Avatar is born to one of the four elements instead. An operator changes the number:
  `/enlightened avatars max <count>`; `/enlightened avatars` lists the Avatars.
* **Each Avatar is born to an element**, and the cycle goes round as in the show: **Fire, Air, Water, Earth**. Each
  new Avatar is born to the element after the last one's (the first at random). He is strongest in it: he begins
  with **all its first arts already learned** (not its sub-bendings), they work **a level higher** (to a master's),
  cool down **20% faster**, and that element's harm strikes **25% harder**.
* When the Avatar is **banned**, the Avatar is **reborn in another player**, at random among those online (or the next
  one to join). If the banned player comes back, he is a bender of the element he was born to, no more.
* In a **hardcore** world, when the Avatar dies, the Avatar is reborn in another player the same way.

If you are born a bender you awaken at once: a title, a burst of your element's colour, a sound and a message to
everyone on the server. If you are born without bending you are told so, and you stay a non-bender. Your fate is cast
only once (it is remembered through death). The server can turn it off (`general.bornBending`).

## The scrolls (Creative only)

The four element scrolls and the Avatar Scroll are now **Creative items only**: they cannot be crafted or found in
chests. In Creative (or given by an operator) a scroll still awakens a non-bender to its element. A bender who reads
his own element's scroll opens the Bending Scroll screen; he cannot read another element's scroll.

## What you get

* Your element's **starter ability** at level 1: Rock Throw, Air Blast, Water Whip, or the Firebender's Skin (the
  Avatar gets the first art of each of the four).
* **Ability points** to spend (1 by default: `general.startingPoints`).
* The default **look** of your element (press **H** for the others: see [Looks](Looks)).
* A hint to press **K**: the [Bending Scroll](Points-and-the-Bending-Scroll), where you learn and arrange your abilities.
