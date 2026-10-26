# Chakras and the Avatar State

[עברית](Chakras-and-the-Avatar-State-HE)

Inspired by the Bedrock *Avatar Legends* add-on and Guru Pathik's lesson: a bender opens the seven chakras by
**meditation**, and with all seven open may enter the **Avatar State**.

## Meditation

Press **Meditate** (default **M**) standing on the ground or in water: the bender sits in the lotus and breathes.
Moving, swinging, falling or dying ends it. Meditation earns one experience every `meditationXpSeconds` (15 s) and,
after `chakraMeditationSeconds` (60 s) of it, opens the next chakra, if you have spent enough points on your element:
the n-th chakra asks for n × `chakraPointsEach` (2) points. On a **Meditation Point** (a block: cyan dye, paper,
cyan dye over three smooth stone slabs; also any block in `#enlightened:meditation_point`) the time runs three times
as fast. The panel shows **Chakras n/7** and *meditating*.

| Chakra | | Gift |
|---|---|---|
| 1 | Earth (survival) | +1 heart |
| 2 | Water (pleasure) | cooldowns 5% shorter |
| 3 | Fire (willpower) | bending 5% stronger |
| 4 | Air (love) | +1 heart |
| 5 | Sound (truth) | cooldowns 5% shorter again |
| 6 | Light (insight) | bending 5% stronger again |
| 7 | Thought (cosmic energy) | the Avatar State |

## The Avatar State

With all seven open, **keep meditating a breath longer** and it takes you (or, if `avatarStateOnLowHealth`, it takes
you by itself when a blow would kill you). For `avatarStateSeconds` (12 s): nothing hurts you, your bending is
**twice as strong** and its cooldowns **half**, every cooldown is cleared at the start, you move 30% faster, never
drown, burn or fall, and glow with your element's light; everyone near is thrown back as you rise. Then it rests
`avatarStateCooldownSeconds` (5 min). The server announces who entered it. `/enlightened chakras <players> <0-7>`
sets the open chakras.
