# Server Configuration

[עברית](Server-Configuration-HE)

`world/serverconfig/enlightened-server.toml` (created when the world is first opened; synced to every client). On a
single-player world, the *Config* button of Enlightened in the Mods screen opens it. Every ability's numbers are
here; the defaults are what the [element pages](Home) describe.

## `[general]`

| Option | Default | What it does |
|---|---|---|
| `startingPoints` | 1 | Ability points a new bender starts with (the starter ability is free on top). |
| `xpPerPoint` | 3 | Kills with bending needed for one point (a boss gives `bossXp`, a player `playerXp`). |
| `bossXp` | 10 | Experience the Wither, the Ender Dragon, the Warden and an Elder Guardian give. |
| `playerXp` | 2 | Experience another player gives (with PvP on). |
| `hurtEverything` | true | Whether bending hurts every creature in its way: the bender's own pets, teammates, villagers, golems, everything alive but himself (players still by the server's PvP rule). On, `hurtPassives` does not matter. |
| `hurtPassives` | true | Whether bending (a rock, a whip, a flame, an area attack) hurts villagers, animals and golems as well as monsters. Turn it off to keep your farms safe. |
| `scrollsInChests` | true | Whether the scrolls are found in chests. |
| `raisedBlockSeconds` | 10 | How long a pillar stands. |

## `[earth]`

`sandGate` 3, `lavaGate` 6, `metalGate` 9 (points spent before the art opens); `rockDamage` 4; `rockRange` 24;
`rockCooldown` 20 ticks; `rockLiftTakesBlocks` true; `earthArmorSlow` 0.3; `wallSeconds` 10; `wallCooldown` 200
ticks; `lavaSeconds` 8; `metalBladeDamage` 6; `metalPullRadius` 6; `wreckDrops` true.

## `[air]`

`airBlastPush` 1.6; `airBlastCooldown` 20 ticks; `airShieldStrength` 10; `airShieldBurstDamage` 7;
`airBladeDamage` 5; `projectionSeconds` 20; `projectionRange` 32; `gliderNeeded` true; `petKillSeversTethers` true;
`tetherlessDamageBonus` 2.

## `[water]`

`iceGate` 3, `bloodGate` 9; `waterSourceRange` 6; `waterSkinCharges` 10; `waterWhipDamage` 3; `waterHeal` 4;
`iceKnifeDamage` 4; `bloodNeedsFullMoon` true; `bloodSeconds` 6; `suppressNoviceMinutes` 10;
`suppressSkilledSeconds` 90.

## `[fire]`

`lightningGate` 6; `fireBallDamage` 4; `fireStreamDps` 4; `fireWhipDamage` 3; `fireRingDamage` 4; `fireHeal` 3;
`lightningDamage` 9; `lightningTwoHandsDamage` 12; `lightningCooldown` 600 ticks; `lightningStunTicks` 40;
`combustionGate` 9; `combustionDamage` 10; `combustionBreaksBlocks` false; `fireWallSeconds` 6.

## `[avatar]`

| Option | Default | What it does |
|---|---|---|
| `chakraMeditationSeconds` | 60 | Seconds of meditation that open one chakra (a third of it on a Meditation Point). |
| `chakraPointsEach` | 2 | Points spent on the element each chakra asks for (the n-th needs n times this). |
| `meditationXpSeconds` | 15 | Seconds of meditation that earn one experience (0: none). |
| `avatarStateLengthSeconds` | 120 | How long the Avatar State lasts (two minutes). |
| `avatarStateCooldownSeconds` | 300 | Seconds before it can be entered again. |
| `avatarStateOnLowHealth` | true | Whether the Avatar enters it by himself when near death (before Guru Pathik's lessons, the only way in). |
| `guruRequired` | true | Whether the chakras open only for a bender Guru Pathik has taught. |
| `combos` | true | Whether sequences of casts trigger the combos. |
| `comboWindowTicks` | 60 | Ticks between the casts of a combo. |
| `energybendingAvatarMinutes` | 120 | Minutes the Avatar's energybending (all seven chakras open) takes a bending for. |
| `energybendingAirSeconds` | 360 | Seconds an airbender's energybending (the lion turtle's gift) takes a bending for. |
| `energybendingOtherSeconds` | 180 | Seconds anyone else's energybending (bender or not) takes a bending for. |
| `lionTurtleMoveSeconds` | 40 | Seconds a [lion turtle](Lion-Turtle) rests between one block of swim and the next, while a player is near it. |
| `avatarStateDamageTaken` | 0.3 | The share of every blow the Avatar takes in the Avatar State (0 makes him invincible). |
| `lionTurtleSize` | 10 | How big new lion turtles are: 1 is an island some 45 blocks long, 10 some 440 long. A great island keeps its own chunks loaded while a player is near it; lower this on a busy server. |
| `steps` | true | Whether a double tap of a direction dashes a bender who learned his element's step. |
