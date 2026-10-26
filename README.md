# Enlightened

A Minecraft mod (NeoForge, Minecraft 1.21.1) where you become a **bender of the Avatar world**:
an **earthbender** who throws rock, wears earth armour, raises walls and pillars and feels the ground, and learns
**sand** (a wall, sand in the air, quicksand), **lava** (a path, a shuriken, a lavabender's skin) and **metal**
(a wall, blades, full armour, pulling metal out of the ground, wrecking machines);
an **airbender** who blasts, shields, jumps and glides on the **Air Glider**, cuts with air blades, leaves his body in
**spirit projection**, breathes under water, rides the air scooter and sends tornadoes, and, if he lets go of his
earthly tethers, **flies** and takes his enemy's breath;
a **waterbender** who whips, heals, grows the **octopus form**, rides a **water spout** and wears water arms, with
the **Water Skin** far from any lake, and learns **ice** (knives, freezing, snow, a wall) and **bloodbending**
(under the full moon, up close, never on a stronger bender; a master takes another bender's bending away);
or a **firebender** whom fire hardly burns, with the fire ball, the fire fist (a master breathes fire), the fire
whip, the fire ring and fire healing, and **lightning** (and its redirection, which a novice fails as often as not).

Every monster you kill with your bending earns experience; every few kills is an **ability point**; a point buys
the next level of an ability (three levels each: novice, adept, master, and the numbers grow with every level).
Sub-bending opens once enough points are spent. There are more abilities than keys, so the **Bending Scroll**
(**K**) is where you choose the ones you take into battle: five keys, each plain and with Sneak.

Every bender wears the look of a character: Toph, King Bumi, Lin and Suyin Beifong, Ghazan and Bolin; Tenzin,
Zaheer, Jinora, Monk Gyatso, Jae and Kartik; Katara, Amon, Ming-Hua and Master Pakku; Zuko, Uncle Iroh, Azula
(whose fire is blue), Jeong Jeong and Fire Lord Ozai, or your own skin. **H** changes the look.

Every bender **acts** their bending: a stomp and a punch for a rock, both palms thrust for an air blast, water drawn
from the side and lashed, a fist chambered at the hip for a fire ball, the arms circling for lightning, in the spirit
of the show's four martial arts; tetherless flight, the glider and the scooter lean the body into the wind, and your
own hands move the same way in first person. Bending hits animals as well as monsters (a server can turn that off).
And **flying lemurs**, like Momo, live in the jungles and meadows: feed one apples or berries to tame it, and it follows
you, glides down to you and perches when told.

Enlightened is built on the same frame as [Enhanced](https://github.com/yuvalkolodkingal/Enhanced), the
superhero mod, and plays the same way: one power per player, the ability keys **R G V B N**, the look key **H**, the
panel in the top-left corner, items that awaken the power, a server config, operator commands and GameTests.

**The ideas are Eilon Kolodkin-Gal's; the mod is by Yuval Kolodkin-Gal.**

## How to play

1. **Become a bender.** Craft a scroll (or find one in a chest) and right-click with it:
   * **Earth Scroll**: paper in the middle, 4 emeralds on the sides, 4 obsidian in the corners.
   * **Air Scroll**: paper, 4 breeze rods on the sides, 4 feathers in the corners.
   * **Water Scroll**: paper, 4 prismarine crystals on the sides, 4 nautilus shells in the corners.
   * **Fire Scroll**: paper, 4 blaze rods on the sides, 4 magma cream in the corners.
2. **Use your keys.** **R G V B N** are abilities 1 to 5; **Sneak + key** is the ability on the key's second slot.
   The panel in the top-left corner shows what sits on every key, its level in stars and its state.
3. **Grow.** Kill monsters with your bending (not with a sword) and watch the points come. Press **K** (or use your
   scroll again) to open the **Bending Scroll**: learn abilities with **+**, click an ability and then a key to put
   it there, right-click a key to clear it.
4. **Sub-bending.** Sand, lava and metal; ice and blood; lightning: each opens when you have spent enough points on
   your element (3, 6, 9 by default). An airbender who kills his own pet lets go of his earthly tethers: flight and
   suffocation open to him, and his air strikes harder.
5. **Items.** The **Water Skin** (4 leather, 1 string) carries ten uses of water; right-click water to fill it. The
   **Air Glider** (2 leather, a breeze rod, 2 sticks) opens as an airbender falls; dye it in a crafting table.
6. **Combos and steps.** Cast the right abilities within three seconds of each other for a combo (Earth Dome, Ice
   Wave, Fire Spin, Twister...); double-tap a direction to dash with your element's step.
7. **Chakras.** Press **M** to meditate (faster on a **Meditation Point**: cyan dye, paper, cyan dye over three
   smooth stone slabs). Each minute opens a chakra; with all seven open, a breath more takes you into the
   **Avatar State**: invincible, twice as strong, for twelve seconds.

Every effect is drawn after the series itself (see `docs/SHOW-REFERENCE.md`), and bending hits every creature in
its way, pets and villagers included (`general.hurtEverything`).

The full guide (every ability and its numbers, the looks, the commands, every server option) is on the
[wiki](../../wiki), in English and Hebrew.

## Building from source

```
./gradlew build              # the jar in build/libs
./gradlew runGameTestServer  # the GameTests (the server exits with an error if one fails)
./gradlew runClient          # a development client
```

Java 21. The skins and the sounds are generated: `python3 tools/skins.py` and `python3 tools/sounds.py`
(numpy, Pillow and ffmpeg).

---

<div dir="rtl">

# Enlightened (עברית)

מוד למיינקראפט (NeoForge, מיינקראפט 1.21.1) שבו אתם הופכים ל**כשפים מעולם האווטאר**:
**כשף אדמה** שזורק סלעים, לובש שריון אדמה, מרים קירות ועמודים ומרגיש את האדמה, ולומד **חול** (קיר, חול באוויר,
חול טובעני), **לבה** (שביל, שוריקן, עור של כשף לבה) ו**פלדה** (קיר, סכינים, שריון מלא, משיכת מתכת מהאדמה, הריסת
מכשירים);
**כשף אוויר** שמעיף, מגן, קופץ וגולש על **דאון האוויר**, חותך בלהבי אוויר, עוזב את גופו ב**הקרנה רוחנית**, נושם
מתחת למים, רוכב על כדור אוויר ושולח טורנדו, ואם הוא מנתק את כבליו הארציים, **עף** ולוקח את הנשימה של האויב;
**כשף מים** שמצליף, מרפא, מגדל **תמנון מים**, רוכב על **מערבולת מים** ולובש זרועות מים, עם **נאד המים** רחוק מכל
אגם, ולומד **קרח** (סכינים, הקפאה, שלג, קיר) ו**כשפות דם** (בירח מלא, מקרוב, אף פעם לא על כשף חזק יותר; מומחה
לוקח לכשף אחר את הכישוף);
או **כשף אש** שאש כמעט לא שורפת, עם כדור אש, אגרוף אש (מומחה נושף אש מהפה), שוט אש, מעגל אש וריפוי אש, ו**ברק**
(והחזרת ברק, שמתחיל נכשל בה לעיתים קרובות).

כל מפלצת שהורגים עם הכישוף נותנת ניסיון; כל כמה הריגות הן **נקודת יכולת**; נקודה קונה את הרמה הבאה של יכולת (שלוש
רמות לכל יכולת: מתחיל, מתקדם, מומחה, והמספרים גדלים עם כל רמה). כשפות המשנה נפתחות כשמשקיעים מספיק נקודות. יש
יותר יכולות ממקשים, ולכן **מגילת הכשפות** (**K**) היא המקום לבחור את אלה שלוקחים לקרב: חמישה מקשים, כל אחד רגיל
ועם התכופפות.

כל כשף לובש נראות של דמות: טוף, המלך בומי, לין וסוּיין בייפונג, גזאן ובולין; טנזין, זאהיר, ג'ינורה, הנזיר ג'יאטסו,
ג'יי וקארתיק; קטרה, אמון, מינג-הואה ומאסטר פאקו; זוקו, דוד אירו, אזולה (שהאש שלה כחולה), ג'אונג ג'אונג ושליט האש
אוזאי, או הסקין שלכם. **H** מחליף נראות.

כל כשף **משחק** את הכישוף שלו: רקיעה ואגרוף לסלע, שתי כפות ידיים להדף אוויר, מים שנמשכים מהצד ומוצלפים, אגרוף
נטען במותן לכדור אש, זרועות במעגלים לברק, ברוח ארבע אומנויות הלחימה של הסדרה; טיסה חופשית, הדאון וכדור האוויר מטים
את הגוף לתוך הרוח, והידיים שלכם זזות באותה צורה בגוף ראשון. הכישוף פוגע בחיות כמו במפלצות (שרת יכול לכבות את זה).
ו**למורים מעופפים**, כמו מומו, חיים בג'ונגלים ובאחו: האכילו אחד בתפוחים או בפירות יער כדי לאלף אותו, והוא עוקב
אחריכם, גולש אליכם ונח כשאומרים לו.

Enlightened בנוי על אותה מסגרת כמו [Enhanced](https://github.com/yuvalkolodkingal/Enhanced), מוד גיבורי העל,
ומשחקים בו באותה צורה: כוח אחד לשחקן, מקשי היכולות **R G V B N**, מקש הנראות **H**, הפאנל בפינה השמאלית העליונה,
פריטים שמעירים את הכוח, הגדרות שרת, פקודות מפעיל ו-GameTests.

**הרעיונות של אילון קולודקין-גל; המוד של יובל קולודקין-גל.**

## איך משחקים

1. **הופכים לכשף.** מכינים מגילה (או מוצאים בתיבה) ולוחצים איתה קליק ימני:
   * **מגילת אדמה**: נייר באמצע, 4 אמרלדים בצדדים, 4 אובסידיאן בפינות.
   * **מגילת אוויר**: נייר, 4 מוטות בריז בצדדים, 4 נוצות בפינות.
   * **מגילת מים**: נייר, 4 גבישי פריזמרין בצדדים, 4 קונכיות נאוטילוס בפינות.
   * **מגילת אש**: נייר, 4 מוטות בלייז בצדדים, 4 קרם מאגמה בפינות.
2. **משתמשים במקשים.** **R G V B N** הם יכולות 1 עד 5; **התכופפות + מקש** היא היכולת במשבצת השנייה של המקש.
   הפאנל בפינה מראה מה יושב על כל מקש, את הרמה בכוכבים ואת המצב.
3. **מתקדמים.** הורגים מפלצות עם הכישוף (לא עם חרב) והנקודות מגיעות. **K** (או המגילה שוב) פותח את **מגילת
   הכשפות**: לומדים יכולות עם **+**, לוחצים על יכולת ואז על מקש כדי לשים אותה שם, קליק ימני על מקש מנקה אותו.
4. **כשפות משנה.** חול, לבה ופלדה; קרח ודם; ברק: כל אחת נפתחת כשהשקעתם מספיק נקודות ביסוד שלכם (3, 6, 9 כברירת
   מחדל). כשף אוויר שהורג את חיית המחמד שלו מנתק את כבליו הארציים: תעופה וחנק נפתחים לו, והאוויר שלו מכה חזק יותר.
5. **פריטים.** **נאד המים** (4 עור, חוט) נושא עשר מנות מים; קליק ימני על מים ממלא אותו. **דאון האוויר** (2 עור,
   מוט בריז, 2 מקלות) נפתח כשכשף אוויר נופל; צובעים אותו בשולחן עבודה.
6. **קומבואים וצעדים.** מטילים את היכולות הנכונות בתוך שלוש שניות זו מזו לקומבו (כיפת אדמה, גל קרח, סחרור אש,
   טוויסטר...); לחיצה כפולה על כיוון מזניקה אתכם בצעד של היסוד שלכם.
7. **צ'אקרות.** **M** למדיטציה (מהר יותר על **נקודת מדיטציה**: צבע טורקיז, נייר, צבע טורקיז מעל שלושה לוחות אבן
   חלקה). כל דקה פותחת צ'אקרה; כשכל השבע פתוחות, עוד נשימה מכניסה אתכם ל**מצב האווטאר**: בלתי פגיעים, חזקים פי
   שניים, לשתים עשרה שניות.

המדריך המלא (כל יכולת והמספרים שלה, הנראויות, הפקודות, כל אפשרות שרת) נמצא ב[ויקי](../../wiki), באנגלית ובעברית.

</div>
