<div dir="rtl">

# בנייה מקוד המקור

[English](Building-from-Source)

Java 21 ו-Git; הבנייה הראשונה מורידה את NeoForge ואת מיינקראפט (כמה דקות).

<div dir="ltr">

```
git clone https://github.com/yuvalkolodkingal/enlightened.git
cd enlightened
./gradlew build              # the jar: build/libs/enlightened-<version>.jar
./gradlew runGameTestServer  # the GameTests; the server exits with an error if one fails
./gradlew runClient          # a development client
./gradlew runServer          # a development server
```

</div>

הסקינים, קליפות השריון והאייקונים של הפריטים מצוירים על ידי `python3 tools/skins.py` (Pillow), 37 הצלילים על ידי
`python3 tools/sounds.py` (numpy ו-ffmpeg): שניהם כותבים ישר לתוך `src/main/resources`, ושניהם דטרמיניסטיים, כך
שהרצה חוזרת לא משנה כלום אלא אם פלטה או מתכון השתנו.

GitHub Actions בונה את המוד בכל דחיפה, מריץ את ה-GameTests ומפרסם את ה-jar כ-release
(`.github/workflows/build.yml`), ומפרסם את הוויקי הזה מ-`docs/wiki` (`wiki.yml`).

העיצוב המלא (כל מספר, איך זה בנוי) נמצא ב-[docs/DESIGN.md](../blob/main/docs/DESIGN.md) (באנגלית).

</div>
