<div dir="rtl">

# הגדרות שרת

[English](Server-Configuration)

`world/serverconfig/enlightened-server.toml` (נוצר כשהעולם נפתח בפעם הראשונה; מסונכרן לכל לקוח). בעולם של שחקן
יחיד, כפתור *Config* של Enlightened במסך המודים פותח אותו. כל המספרים של כל יכולת כאן; ברירות המחדל הן מה
ש[דפי היסודות](Home-HE) מתארים.

## `[general]`

| אפשרות | ברירת מחדל | מה היא עושה |
|---|---|---|
| `startingPoints` | 1 | נקודות יכולת שכשף חדש מתחיל איתן (יכולת הפתיחה חינם בנוסף). |
| `xpPerPoint` | 3 | הריגות עם כישוף לנקודה אחת (בוס נותן `bossXp`, שחקן `playerXp`). |
| `bossXp` | 10 | ניסיון שהוית'ר, דרקון הקצה, הוורדן ושומר בכיר נותנים. |
| `playerXp` | 2 | ניסיון ששחקן אחר נותן (עם PvP דלוק). |
| `hurtPassives` | true | האם הכישוף (סלע, שוט, להבה, התקפת שטח) פוגע גם בכפריים, חיות וגולמים ולא רק במפלצות. כבו כדי לשמור על החוות. |
| `scrollsInChests` | true | האם המגילות נמצאות בתיבות. |
| `raisedBlockSeconds` | 10 | כמה זמן עמוד עומד. |

## `[earth]`

`sandGate` 3, `lavaGate` 6, `metalGate` 9 (נקודות שהושקעו לפני שהכשפות נפתחת); `rockDamage` 4; `rockRange` 24;
`rockCooldown` 20 טיקים; `rockLiftTakesBlocks` true; `earthArmorSlow` 0.3; `wallSeconds` 10; `wallCooldown` 200
טיקים; `lavaSeconds` 8; `metalBladeDamage` 6; `metalPullRadius` 6; `wreckDrops` true.

## `[air]`

`airBlastPush` 1.6; `airBlastCooldown` 20 טיקים; `airShieldStrength` 10; `airShieldBurstDamage` 7;
`airBladeDamage` 5; `projectionSeconds` 20; `projectionRange` 32; `gliderNeeded` true; `petKillSeversTethers` true;
`tetherlessDamageBonus` 2.

## `[water]`

`iceGate` 3, `bloodGate` 9; `waterSourceRange` 6; `waterSkinCharges` 10; `waterWhipDamage` 3; `waterHeal` 4;
`iceKnifeDamage` 4; `bloodNeedsFullMoon` true; `bloodSeconds` 6; `suppressNoviceMinutes` 10;
`suppressSkilledSeconds` 90.

## `[fire]`

`lightningGate` 6; `fireBallDamage` 4; `fireStreamDps` 4; `fireWhipDamage` 3; `fireRingDamage` 4; `fireHeal` 3;
`lightningDamage` 9; `lightningTwoHandsDamage` 12; `lightningCooldown` 600 טיקים; `lightningStunTicks` 40.

</div>
