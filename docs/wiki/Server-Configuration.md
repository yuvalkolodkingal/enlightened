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
`lightningDamage` 9; `lightningTwoHandsDamage` 12; `lightningCooldown` 600 ticks; `lightningStunTicks` 40.
