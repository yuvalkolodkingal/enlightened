# Chakras and the Avatar State

[עברית](Chakras-and-the-Avatar-State-HE)

After Guru Pathik's lessons in "The Guru": **Guru Pathik**, who meditates at the
[Southern Air Temple](Southern-Air-Temple), teaches a bender to open the seven chakras. Each open chakra strengthens
any bender a little. For **the Avatar** they are the door to the **Avatar State** at will. The Avatar State is the
Avatar's alone: no other bender ever enters it.

## Guru Pathik

Right-click the guru to speak to him. He teaches you the way of the chakras; from then on they open for you by
meditation. Before him (within 8 blocks) your meditation opens them **six times as fast**, and he asks no points.
As each chakra opens before him he tells you what blocked it. Spoken to again, he tells you which chakra is next and
what blocks it.

Untaught, meditation calms you and earns experience, but your chakras stay closed. A server can drop the guru with
`avatar.guruRequired = false`. `/enlightened guru <players> taught|untaught` marks players as taught.

## Meditation

Press **Meditate** (default **M**) standing on the ground or in water: the bender sits in the lotus and breathes.
Moving, swinging, falling or dying ends it. Meditation earns one experience every `meditationXpSeconds` (15 s) and,
after `chakraMeditationSeconds` (60 s) of it, opens the next chakra, if you have spent enough points on your element:
the n-th chakra asks for n × `chakraPointsEach` (2) points (none before the guru). On a **Meditation Point** (a block:
cyan dye, paper, cyan dye over three smooth stone slabs; also any block in `#enlightened:meditation_point`) the time
runs three times as fast. The panel shows **Chakras n/7** and *meditating*.

| Chakra | | Blocked by | Gift |
|---|---|---|---|
| 1 | Earth (survival) | fear | +1 heart |
| 2 | Water (pleasure) | guilt | cooldowns 5% shorter |
| 3 | Fire (willpower) | shame | bending 5% stronger |
| 4 | Air (love) | grief | +1 heart |
| 5 | Sound (truth) | lies | cooldowns 5% shorter again |
| 6 | Light (insight) | illusion | bending 5% stronger again |
| 7 | Thought (cosmic energy) | attachment | for the Avatar: the Avatar State at will |

## The cosmic plane

When the seventh chakra is about to open, your spirit leaves your body for a sea of stars in purple. A path of light
runs out ahead of you, and **your own self sits vast at its end**, dark as the night sky, with the arrow and the eyes
alight. Walk the path to its end: there the Thought chakra opens, and you come back where you sat. Step off the
path, and it catches you again at its start.

## The Avatar State

Only the Avatar has it. For `avatarStateLengthSeconds` (**two minutes**): nothing hurts you, your bending is **twice as strong** and
its cooldowns **half**, every cooldown is cleared at the start, you move 30% faster, never drown, burn or fall, and
glow with your element's light; everyone near is thrown back as you rise. The server announces who entered it.

* **Untaught**, it takes the Avatar only by itself, when a blow would kill him (`avatarStateOnLowHealth`), and then
  rests `avatarStateCooldownSeconds` (5 min).
* **Taught by the guru, with all seven chakras open**, the Avatar enters it **at will**: the Avatar State art, or
  keeping on meditating a breath longer. Pressed again inside it, the art **brings him out**. He rests three times
  as long as he stayed in it (at least 10 s).

`/enlightened chakras <players> <0-7>` sets the open chakras; `/enlightened avatar <players> enter|leave|rest` works
for Avatars only.

## As the show draws it

In the Avatar State the eyes glow solid white and an air nomad's arrow tattoos glow blue-white with them (Zaheer has
none). Each chakra opens in the colour of its vision in "The Guru": earth red, water orange, fire yellow, air green,
sound blue, light indigo, thought violet.
