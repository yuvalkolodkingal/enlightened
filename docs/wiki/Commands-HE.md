<div dir="rtl">

# פקודות

[English](Commands)

כולן דורשות הרשאות מפעיל (רמה 2).

| פקודה | מה היא עושה |
|---|---|
| `/enlightened element get [player]` | איזה יסוד שחקן מכשף, והנראות שלו. |
| `/enlightened element set <players> <none\|earth\|air\|water\|fire> [look]` | הופך שחקנים לכשפים של יסוד (עם ההתעוררות, וכל מה שהיה ביסוד הישן נעלם), או לוקח את הכישוף עם `none`. |
| `/enlightened look <players> <look>` | מחליף נראות (המזהים: `toph`, `bumi`, `lin`, `suyin`, `ghazan`, `bolin`; `tenzin`, `zaheer`, `jinora`, `gyatso`, `jae`, `kartik`; `katara`, `amon`, `ming_hua`, `pakku`; `zuko`, `iroh`, `azula`, `jeong_jeong`, `ozai`; `own`). |
| `/enlightened points add <players> <points>` | נותן (או, שלילי, לוקח) נקודות יכולת. |
| `/enlightened learn <players> <ability> [level]` | קובע רמה של יכולת (בלי רמה: אחת יותר), בלי קשר לנקודות ולשערים; `0` שוכח אותה. המזהים הם שמות היכולות באנגלית באותיות קטנות עם קו תחתון (`rock_throw`, `air_shield`, `water_whip`, `lightning_redirect`...). |
| `/enlightened loadout <player> <slot 1-10> <ability\|none>` | שם יכולת על משבצת (משבצת 1 היא R, 2 היא התכופפות + R, 3 היא G, ... 10 היא התכופפות + N). |
| `/enlightened tetherless <players> <true\|false>` | הופך כשף אוויר ל[משוחרר מכבלים](Airbending-HE#כשף-האוויר-המשוחרר-מכבלים), או מבטל (תעופה וחנק נשכחים). |
| `/enlightened reset <players>` | לוקח בחזרה כל רמה מלבד של יכולת הפתיחה ומחזיר את הנקודות. |
| `/enlightened avatar <players> <enter\|leave\|rest>` | נכנס או יוצא מ[מצב האווטאר](Chakras-and-the-Avatar-State-HE) (בלי קשר לצ'אקרות), או מסיים את המנוחה שלו. אווטארים בלבד. |
| `/enlightened avatars` / `/enlightened avatars max <count>` | מציג את האווטארים (והיסוד שכל אחד נולד אליו, ושל הבא); קובע כמה אווטארים יכולים להיות בעולם בבת אחת (ברירת מחדל 1). |
| `/enlightened guru <players> <taught\|untaught>` | מסמן שחקנים כתלמידים (או לא) של גורו פאתיק. |
| `/enlightened lionturtle [pos [size]]` | מעלה [צב אריה](Lion-Turtle-HE) ואת האי שלו (הוא עולה תוך כמה שניות): בגובה פני הים היכן שאתה עומד, או עם פני הים במיקום הנתון; הוא פונה לאן שאתה פונה. גודל 1 עד 10 (ברירת מחדל `lionTurtleSize`). |
| `/enlightened firecolor <players> <normal\|blue\|rainbow\|look>` | קובע את צבע האש של השחקנים (`look`: לפי הנראות שלהם). |
| `/enlightened temple [pos]` | בונה את [מקדש האוויר הדרומי](Southern-Air-Temple-HE) עם רצפת הטרסה במיקום (ברירת מחדל: היכן שאתה עומד). |
| `/enlightened chakras <players> <0-7>` | קובע כמה [צ'אקרות](Chakras-and-the-Avatar-State-HE) פתוחות. |
| `/enlightened info [player]` | יסוד, נקודות, ניסיון, נקודות שהושקעו, הריגות, וכל יכולת שנלמדה עם הרמה והמשבצת שלה. |

</div>
