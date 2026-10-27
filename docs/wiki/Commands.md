# Commands

[עברית](Commands-HE)

All need operator rights (permission level 2).

| Command | What it does |
|---|---|
| `/enlightened element get [player]` | Which element a player bends, and his look. |
| `/enlightened element set <players> <none\|earth\|air\|water\|fire> [look]` | Makes players benders of an element (with the awakening, and everything of the old element gone), or takes their bending away with `none`. |
| `/enlightened look <players> <look>` | Changes the look (the ids: `toph`, `bumi`, `lin`, `suyin`, `ghazan`, `bolin`; `tenzin`, `zaheer`, `jinora`, `gyatso`, `jae`, `kartik`; `katara`, `amon`, `ming_hua`, `pakku`; `zuko`, `iroh`, `azula`, `jeong_jeong`, `ozai`; `own`). |
| `/enlightened points add <players> <points>` | Gives (or, negative, takes) ability points. |
| `/enlightened learn <players> <ability> [level]` | Sets an ability's level (without a level: one more), ignoring points and gates; `0` forgets it. The ids are the ability names in lower case with underscores (`rock_throw`, `air_shield`, `water_whip`, `lightning_redirect`...). |
| `/enlightened loadout <player> <slot 1-10> <ability\|none>` | Puts an ability on a slot (slot 1 is R, 2 is Sneak + R, 3 is G, ... 10 is Sneak + N). |
| `/enlightened tetherless <players> <true\|false>` | Makes an airbender [tetherless](Airbending#the-tetherless-airbender), or no longer (flight and suffocation are forgotten). |
| `/enlightened reset <players>` | Takes back every level but the starter's and refunds the points. |
| `/enlightened avatar <players> <enter\|leave\|rest>` | Enters or leaves the [Avatar State](Chakras-and-the-Avatar-State) (whatever the chakras), or ends its rest. |
| `/enlightened chakras <players> <0-7>` | Sets how many [chakras](Chakras-and-the-Avatar-State) are open. |
| `/enlightened info [player]` | Element, points, experience, points spent, kills, and every learned ability with its level and slot. |
