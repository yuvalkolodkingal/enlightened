#!/usr/bin/env python3
"""Draws the Seven Havens brigade armor trim: the uniform of the four brigades painted over any armor.

    python3 tools/brigade.py          # writes src/main/resources/assets/enlightened/textures/trims/... and the template icon

The trim is one pattern (enlightened:brigade) whose look is chosen by the material it is set in, as in the show:
lapis for the water brigade (lavender blue, the moon and its crescent), quartz for the air brigade (gold-orange, the
blue triangle), redstone for the fire brigade (red-orange, the diamond), coal for the earth brigade (green, the
trapezoid). Those four are painted in full colour (the shoulder pads and the chest plate in the brigade's colour, the
sleeves grey, the belt, the apron with its white edge, the dark trousers and forearms, the headband); any other trim
material gets the same uniform in its own palette, from a grey version keyed to vanilla's trim palette. The armor
textures are the vanilla 64x32 armor layout: layer 1 (helmet, chestplate, boots) and layer 2 (leggings).
"""
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'enlightened', 'textures')
ARMOR = os.path.join(ROOT, 'trims', 'models', 'armor')
PALETTES = os.path.join(ROOT, 'trims', 'color_palettes')
ITEM = os.path.join(ROOT, 'item')

GREY = (168, 166, 176)
GREY_DARK = (128, 126, 138)
BELT = (104, 102, 112)
WHITE = (236, 234, 230)
SHOE = (70, 60, 54)
BAND_TOP = (232, 124, 152)
BAND_LOW = (136, 154, 232)

# The four brigades: main colour, its shade, the collar and forearms, the trousers, and the symbol's two colours.
BRIGADES = {
    # Water: lavender blue and navy, the moon with its golden crescent.
    'lapis': dict(main=(124, 130, 206), shade=(98, 104, 182), collar=(52, 54, 112), dark=(58, 58, 116),
                  symbol='moon', sym=(150, 174, 236), sym2=(242, 196, 82)),
    # Air: gold-orange, red collar and forearms, ochre trousers, the blue triangle.
    'quartz': dict(main=(238, 176, 64), shade=(212, 146, 48), collar=(190, 66, 46), dark=(176, 116, 44),
                   symbol='triangle', sym=(104, 176, 244), sym2=(214, 92, 52)),
    # Fire: red-orange, deep red collar, forearms and trousers, the orange diamond.
    'redstone': dict(main=(224, 96, 48), shade=(196, 74, 38), collar=(176, 44, 34), dark=(118, 32, 30),
                     symbol='diamond', sym=(246, 160, 72), sym2=(206, 80, 36)),
    # Earth: green, dark green collar, forearms and trousers, the golden trapezoid.
    'coal': dict(main=(104, 172, 84), shade=(82, 146, 66), collar=(34, 96, 54), dark=(38, 92, 56),
                 symbol='trapezoid', sym=(232, 224, 116), sym2=(236, 150, 66)),
}

# The palette of the coal trim on every other pattern: a sooty black-grey (vanilla has no coal trim).
COAL_PALETTE = [(92, 92, 96), (76, 76, 80), (62, 62, 66), (50, 50, 54), (40, 40, 44), (30, 30, 34), (22, 22, 25), (13, 13, 15)]
TRIM_KEY = [224, 192, 160, 128, 96, 64, 32, 0]


def put(img, x, y, c):
    img.putpixel((x, y), tuple(c) + (255,) if len(c) == 3 else c)


def rect(img, x0, y0, x1, y1, c):
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            put(img, x, y, c)


def symbol(img, kind, b, x0, y0):
    """The brigade's symbol, 6 wide by 4 high, its top-left at (x0, y0)."""
    s, s2 = b['sym'], b['sym2']
    if kind == 'moon':
        for (x, y) in [(1, 0), (2, 0), (3, 0), (4, 0), (0, 1), (1, 1), (2, 1), (3, 1), (4, 1), (5, 1), (0, 2), (1, 2), (2, 2), (3, 2), (4, 2), (5, 2)]:
            put(img, x0 + x, y0 + y, s)
        for (x, y) in [(0, 2), (5, 2), (1, 3), (2, 3), (3, 3), (4, 3)]:
            put(img, x0 + x, y0 + y, s2)
    elif kind == 'triangle':
        for (x, y) in [(0, 0), (1, 0), (4, 0), (5, 0), (1, 1), (4, 1), (2, 2), (3, 2)]:
            put(img, x0 + x, y0 + y, s)
        for (x, y) in [(2, 0), (3, 0), (2, 1), (3, 1)]:
            put(img, x0 + x, y0 + y, s2)
        put(img, x0 + 2, y0 + 3, s)
        put(img, x0 + 3, y0 + 3, s)
    elif kind == 'diamond':
        for (x, y) in [(2, 0), (3, 0), (1, 1), (2, 1), (3, 1), (4, 1), (1, 2), (4, 2), (2, 3), (3, 3)]:
            put(img, x0 + x, y0 + y, s2)
        for (x, y) in [(2, 2), (3, 2)]:
            put(img, x0 + x, y0 + y, s)
    elif kind == 'trapezoid':
        for (x, y) in [(2, 0), (3, 0), (1, 1), (2, 1), (3, 1), (4, 1), (0, 2), (1, 2), (4, 2), (5, 2), (0, 3), (5, 3)]:
            put(img, x0 + x, y0 + y, s)
        for (x, y) in [(2, 2), (3, 2), (1, 3), (2, 3), (3, 3), (4, 3)]:
            put(img, x0 + x, y0 + y, s2)


def layer1(b):
    """Helmet (the headband), chestplate (shoulder pads, plate, sleeves, belt, apron) and boots."""
    img = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    main, shade, collar, dark = b['main'], b['shade'], b['collar'], b['dark']
    # The headband round the brow, two stripes, tied at the back with its ends hanging.
    for x in range(0, 32):
        put(img, x, 10, BAND_TOP)
        put(img, x, 11, BAND_LOW)
    for (x, y) in [(27, 12), (28, 12), (27, 13), (28, 14)]:
        put(img, x, y, BAND_LOW)
    # The body: top and bottom of the torso.
    rect(img, 20, 16, 27, 19, main)
    rect(img, 23, 16, 24, 19, collar)
    rect(img, 28, 16, 35, 19, dark)
    # Front: the high collar, the chest plate and its symbol, the belt and buckle, the apron with its white edge.
    rect(img, 20, 20, 27, 27, main)
    rect(img, 22, 20, 25, 20, collar)
    rect(img, 23, 21, 24, 21, collar)
    for y in range(22, 28):
        put(img, 20, y, WHITE)
        put(img, 27, y, WHITE)
    symbol(img, b['symbol'], b, 21, 22)
    rect(img, 20, 28, 27, 29, BELT)
    rect(img, 25, 28, 25, 29, shade)
    rect(img, 20, 30, 27, 31, main)
    put(img, 20, 30, WHITE)
    put(img, 20, 31, WHITE)
    put(img, 27, 30, WHITE)
    put(img, 27, 31, WHITE)
    # Back: the plate, plain; the belt.
    rect(img, 32, 20, 39, 27, shade)
    rect(img, 34, 20, 37, 20, collar)
    rect(img, 32, 28, 39, 29, BELT)
    rect(img, 32, 30, 39, 31, shade)
    # Sides: grey cloth under the arms, the belt round.
    for x0 in (16, 28):
        rect(img, x0, 20, x0 + 3, 27, GREY)
        rect(img, x0, 20, x0 + 3, 21, shade)
        rect(img, x0, 28, x0 + 3, 29, BELT)
        rect(img, x0, 30, x0 + 3, 31, dark)
    # Arms: the shoulder pad over the top, grey sleeves, dark forearms.
    rect(img, 44, 16, 47, 19, main)
    rect(img, 48, 16, 51, 19, dark)
    rect(img, 40, 20, 55, 22, main)
    rect(img, 40, 23, 55, 23, shade)
    rect(img, 40, 24, 55, 27, GREY)
    rect(img, 40, 27, 55, 27, GREY_DARK)
    rect(img, 40, 28, 55, 31, dark)
    # Boots: the trousers gathered at the ankle, the soft shoe.
    rect(img, 0, 28, 15, 29, dark)
    rect(img, 0, 30, 15, 31, SHOE)
    rect(img, 8, 16, 11, 19, SHOE)
    return img


def layer2(b):
    """Leggings: the dark trousers, the apron hanging in front to the knee, grey plates on the thighs, the belt."""
    img = Image.new('RGBA', (64, 32), (0, 0, 0, 0))
    main, shade, dark = b['main'], b['shade'], b['dark']
    rect(img, 4, 16, 7, 19, BELT)
    rect(img, 0, 20, 15, 31, dark)
    # The apron, its outer edge white (the right leg; the left mirrors it).
    rect(img, 4, 20, 7, 27, main)
    for y in range(20, 28):
        put(img, 4, y, WHITE)
    rect(img, 4, 27, 7, 27, WHITE)
    # The grey thigh plate on the outside of the leg.
    rect(img, 0, 20, 3, 25, GREY)
    rect(img, 0, 25, 3, 25, GREY_DARK)
    # The back flap.
    rect(img, 12, 20, 15, 25, shade)
    # The waist: the belt and the apron's top.
    rect(img, 20, 28, 27, 29, BELT)
    rect(img, 20, 30, 27, 31, main)
    put(img, 20, 30, WHITE)
    put(img, 27, 30, WHITE)
    rect(img, 32, 28, 39, 29, BELT)
    rect(img, 32, 30, 39, 31, shade)
    rect(img, 16, 28, 19, 31, BELT)
    rect(img, 28, 28, 31, 31, BELT)
    return img


def grey(img):
    """The uniform keyed to vanilla's trim palette: each colour to the key grey nearest its brightness."""
    out = Image.new('RGBA', img.size, (0, 0, 0, 0))
    for x in range(img.width):
        for y in range(img.height):
            r, g, bl, a = img.getpixel((x, y))
            if a == 0:
                continue
            lum = 0.299 * r + 0.587 * g + 0.114 * bl
            k = min(TRIM_KEY, key=lambda v: abs(v - lum))
            out.putpixel((x, y), (k, k, k, 255))
    return out


def icon():
    """The template: a sandstone tablet of Ellora with the four brigades' symbols in its corners, a golden rim."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    gold, gold_dark, stone, stone_dark = (250, 206, 74), (196, 140, 40), (226, 150, 104), (180, 104, 72)
    rect(img, 2, 1, 13, 14, gold_dark)
    rect(img, 3, 2, 12, 13, stone)
    for x in range(3, 13):
        put(img, x, 2, gold)
    for y in range(2, 14):
        put(img, 3, y, gold)
    rect(img, 4, 13, 12, 13, stone_dark)
    rect(img, 12, 3, 12, 13, stone_dark)
    # The golden spire of the city up the middle.
    rect(img, 7, 4, 8, 11, gold)
    put(img, 7, 3, (255, 244, 170))
    for (x, y, c) in [(4, 4, BRIGADES['lapis']['sym']), (5, 4, BRIGADES['lapis']['sym2']),
                      (10, 4, BRIGADES['quartz']['sym']), (11, 4, BRIGADES['quartz']['sym2']),
                      (4, 11, BRIGADES['redstone']['sym2']), (5, 11, BRIGADES['redstone']['sym']),
                      (10, 11, BRIGADES['coal']['main']), (11, 11, BRIGADES['coal']['sym'])]:
        put(img, x, y, c)
    return img


def main():
    os.makedirs(ARMOR, exist_ok=True)
    os.makedirs(PALETTES, exist_ok=True)
    os.makedirs(ITEM, exist_ok=True)
    for name, b in BRIGADES.items():
        layer1(b).save(os.path.join(ARMOR, 'brigade_%s.png' % name))
        layer2(b).save(os.path.join(ARMOR, 'brigade_leggings_%s.png' % name))
    grey(layer1(BRIGADES['lapis'])).save(os.path.join(ARMOR, 'brigade.png'))
    grey(layer2(BRIGADES['lapis'])).save(os.path.join(ARMOR, 'brigade_leggings.png'))
    pal = Image.new('RGBA', (8, 1))
    for i, c in enumerate(COAL_PALETTE):
        pal.putpixel((i, 0), c + (255,))
    pal.save(os.path.join(PALETTES, 'coal.png'))
    icon().save(os.path.join(ITEM, 'brigade_armor_trim_smithing_template.png'))


if __name__ == '__main__':
    main()
