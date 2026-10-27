#!/usr/bin/env python3
"""Draws every skin, shell, mob texture and item icon of Enlightened from palettes (no art files are kept outside
the repo).

    python3 tools/skins.py            # writes src/main/resources/assets/enlightened/textures/...

Each look is a palette and a few flags (the garment of its nation, the hair and its style, a beard, the airbender's
arrows, a scar, a mask, a headband, a crown...) painted onto the standard 64x64 skin layout, with shading, a little
grain and two-tone details (collars, belts and buckles, cuffs, fur trims, pauldrons, boot soles) so the clothes read
as clothes and not as flat colour. The flying lemur's 64x32 texture is drawn from its model's layout. Run it after
changing a palette; the result is deterministic.
"""
import os
import random

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'enlightened', 'textures')
SUIT = os.path.join(ROOT, 'entity', 'suit')
ENTITY = os.path.join(ROOT, 'entity')
ITEM = os.path.join(ROOT, 'item')


# ---- the skin layout: (x, y, w, h) of each face of each cube ----

def cube(x, y, w, h, d):
    """The six faces of a cube whose texture starts at (x, y): width w, height h, depth d."""
    return {
        'top': (x + d, y, w, d), 'bottom': (x + d + w, y, w, d),
        'right': (x, y + d, d, h), 'front': (x + d, y + d, w, h),
        'left': (x + d + w, y + d, d, h), 'back': (x + 2 * d + w, y + d, w, h),
    }


HEAD = cube(0, 0, 8, 8, 8)
HAT = cube(32, 0, 8, 8, 8)
BODY = cube(16, 16, 8, 12, 4)
JACKET = cube(16, 32, 8, 12, 4)
R_LEG = cube(0, 16, 4, 12, 4)
L_LEG = cube(16, 48, 4, 12, 4)


def arm(x, y, slim):
    return cube(x, y, 3 if slim else 4, 12, 4)


def shade(color, f):
    r, g, b = color[:3]
    return (max(0, min(255, int(r * f))), max(0, min(255, int(g * f))), max(0, min(255, int(b * f))), 255)


def mixc(a, b, t):
    return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3))


SIDE_SHADE = {'front': 1.0, 'left': 0.86, 'right': 0.86, 'back': 0.78, 'top': 1.06, 'bottom': 0.7}


class Painter:
    def __init__(self, seed, size=(64, 64)):
        self.img = Image.new('RGBA', size, (0, 0, 0, 0))
        self.px = self.img.load()
        self.rnd = random.Random(seed)

    def fill(self, rect, color, grain=0.04, dark=1.0, vgrad=0.0):
        """Fills a rect with grain; vgrad > 0 makes the top lighter and the bottom darker (a lit garment)."""
        x, y, w, h = rect
        for i in range(x, x + w):
            for j in range(y, y + h):
                g = 1.0 + vgrad * (0.5 - (j - y) / max(1, h - 1)) if h > 1 else 1.0
                f = dark * g * (1.0 + self.rnd.uniform(-grain, grain))
                self.px[i, j] = shade(color, f)

    def faces(self, c, color, grain=0.04, names=None, vgrad=0.12):
        for name, rect in c.items():
            if names and name not in names:
                continue
            self.fill(rect, color, grain, SIDE_SHADE[name], vgrad if name in ('front', 'back', 'left', 'right') else 0)

    def rows(self, c, color, rows, grain=0.03, names=('front', 'left', 'right', 'back')):
        """Paints horizontal rows (relative to the face's top) of the side faces: belts, trims, boots."""
        for name in names:
            x, y, w, h = c[name]
            for r in rows:
                if 0 <= r < h:
                    self.fill((x, y + r, w, 1), color, grain, SIDE_SHADE[name])

    def cols(self, c, color, cols, grain=0.03, names=('front',)):
        for name in names:
            x, y, w, h = c[name]
            for k in cols:
                if 0 <= k < w:
                    self.fill((x + k, y, 1, h), color, grain, SIDE_SHADE[name])

    def dot(self, x, y, color, f=1.0):
        self.px[x, y] = shade(color, f)

    def speckle(self, rect, color, chance, f=1.1):
        x, y, w, h = rect
        for i in range(x, x + w):
            for j in range(y, y + h):
                if self.rnd.random() < chance:
                    self.px[i, j] = shade(color, f)

    def save(self, path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        self.img.save(path)


# ---- the face and the hair ----

def paint_head(p, skin, eyes, mouth=(150, 70, 70), brows=(60, 50, 40)):
    p.faces(HEAD, skin, grain=0.025, vgrad=0.06)
    fx, fy = HEAD['front'][0], HEAD['front'][1]
    # Cheeks a touch warmer, the chin a touch darker, the brow lit.
    p.dot(fx + 1, fy + 5, mixc(skin, (230, 150, 140), 0.25))
    p.dot(fx + 6, fy + 5, mixc(skin, (230, 150, 140), 0.25))
    for i in range(8):
        p.dot(fx + i, fy + 7, skin, 0.92)
    # Eyes: a white and an iris each, with a glint; brows above; a nose; a mouth.
    p.dot(fx + 1, fy + 4, (248, 248, 248))
    p.dot(fx + 2, fy + 4, eyes)
    p.dot(fx + 5, fy + 4, eyes)
    p.dot(fx + 6, fy + 4, (248, 248, 248))
    p.dot(fx + 2, fy + 3, mixc(skin, brows, 0.7))
    p.dot(fx + 1, fy + 3, mixc(skin, brows, 0.55))
    p.dot(fx + 5, fy + 3, mixc(skin, brows, 0.7))
    p.dot(fx + 6, fy + 3, mixc(skin, brows, 0.55))
    p.dot(fx + 3, fy + 5, skin, 0.9)
    p.dot(fx + 4, fy + 5, skin, 0.86)
    p.dot(fx + 3, fy + 6, mouth)
    p.dot(fx + 4, fy + 6, mouth)


def hair_tone(p, rect, color, dark=1.0, streak=0.18):
    """Hair: the base with lighter streaks, so it reads as strands."""
    p.fill(rect, color, 0.05, dark)
    p.speckle(rect, color, streak, 1.25 * dark)


def paint_hair(p, color, style):
    """Styles: bald, short, long, bob, topknot, bun, wild, loops, ponytail, fringe, braid, slick."""
    if style == 'bald':
        return
    top, back, left, right, front = HEAD['top'], HEAD['back'], HEAD['left'], HEAD['right'], HEAD['front']
    fx, fy = front[0], front[1]
    hair_tone(p, top, color, 1.05)
    if style in ('short', 'wild', 'topknot', 'bun', 'ponytail', 'loops', 'braid', 'slick'):
        hair_tone(p, (back[0], back[1], 8, 3), color, 0.85)
        hair_tone(p, (left[0], left[1], 8, 3), color, 0.92)
        hair_tone(p, (right[0], right[1], 8, 3), color, 0.92)
        hair_tone(p, (fx, fy, 8, 2), color)
        # A hairline with a parting and two points at the temples.
        p.dot(fx, fy + 2, color, 0.95)
        p.dot(fx + 7, fy + 2, color, 0.95)
    if style in ('long', 'bob', 'fringe'):
        rows = 8 if style == 'long' else 6
        hair_tone(p, (back[0], back[1], 8, rows), color, 0.82)
        hair_tone(p, (left[0], left[1], 8, rows - 1), color, 0.9)
        hair_tone(p, (right[0], right[1], 8, rows - 1), color, 0.9)
        hair_tone(p, (fx, fy, 8, 2), color)
        p.dot(fx, fy + 2, color, 0.95)
        p.dot(fx + 7, fy + 2, color, 0.95)
        # Locks framing the face on the hat layer.
        hair_tone(p, (HAT['front'][0], HAT['front'][1] + 2, 1, 4), color, 0.95)
        hair_tone(p, (HAT['front'][0] + 7, HAT['front'][1] + 2, 1, 4), color, 0.95)
        hair_tone(p, (HAT['back'][0], HAT['back'][1], 8, rows), color, 0.8)
    if style == 'fringe':
        hair_tone(p, (fx, fy, 8, 3), color)
        p.dot(fx + 2, fy + 3, color)
        p.dot(fx + 5, fy + 3, color)
        p.dot(fx + 3, fy + 2, color, 0.9)
    if style == 'slick':
        hair_tone(p, (fx, fy, 8, 1), color)
        hair_tone(p, (HAT['back'][0], HAT['back'][1], 8, 4), color, 0.8)
    if style == 'wild':
        hair_tone(p, HAT['top'], color, 1.1, 0.3)
        for i in range(0, 8, 2):
            p.dot(HAT['front'][0] + i, HAT['front'][1], color, 1.1)
            p.dot(HAT['back'][0] + i + 1, HAT['back'][1], color, 0.9)
            p.dot(HAT['left'][0] + i, HAT['left'][1], color, 0.95)
            p.dot(HAT['right'][0] + i + 1, HAT['right'][1], color, 0.95)
        hair_tone(p, (HAT['back'][0], HAT['back'][1], 8, 3), color, 0.85, 0.3)
    if style == 'topknot':
        hair_tone(p, (HAT['top'][0] + 3, HAT['top'][1] + 2, 2, 3), color, 1.1)
        hair_tone(p, (HAT['back'][0] + 3, HAT['back'][1], 2, 1), color)
        hair_tone(p, (HAT['front'][0] + 3, HAT['front'][1], 2, 1), color)
    if style == 'bun':
        hair_tone(p, (HAT['back'][0] + 2, HAT['back'][1] + 1, 4, 3), color, 0.9)
        hair_tone(p, (HAT['top'][0] + 2, HAT['top'][1] + 5, 4, 3), color, 1.05)
    if style == 'ponytail':
        hair_tone(p, (HAT['back'][0] + 3, HAT['back'][1] + 1, 2, 7), color, 0.85)
    if style == 'braid':
        for j in range(1, 8):
            p.dot(HAT['back'][0] + 3 + (j % 2), HAT['back'][1] + j, color, 0.8 + 0.15 * (j % 2))
            p.dot(HAT['back'][0] + 4 - (j % 2), HAT['back'][1] + j, color, 0.95 - 0.15 * (j % 2))
    if style == 'loops':
        for side in (HAT['left'], HAT['right']):
            hair_tone(p, (side[0] + 1, side[1] + 2, 1, 4), color, 0.9)
            hair_tone(p, (side[0] + 6, side[1] + 2, 1, 4), color, 0.9)
            p.dot(side[0] + 1, side[1] + 6, color, 0.85)
            p.dot(side[0] + 6, side[1] + 6, color, 0.85)
        hair_tone(p, (HAT['back'][0] + 2, HAT['back'][1] + 3, 4, 5), color, 0.85)
        for j in range(1, 8):
            p.dot(HAT['back'][0] + 3 + (j % 2), HAT['back'][1] + j, color, 0.75 + 0.2 * (j % 2))


def paint_beard(p, color, big=False, full=False):
    fx, fy = HEAD['front'][0], HEAD['front'][1]
    for i in (0, 1, 2, 5, 6, 7):
        p.dot(fx + i, fy + 7, color, 0.95)
    if full:
        for i in range(8):
            p.dot(fx + i, fy + 7, color)
        p.dot(fx, fy + 6, color)
        p.dot(fx + 7, fy + 6, color)
    if big:
        hair_tone(p, (HAT['front'][0] + 1, HAT['front'][1] + 6, 6, 2), color, 1.0)
        hair_tone(p, (HAT['front'][0] + 2, HAT['front'][1] + 5, 4, 1), color, 1.0)
        hair_tone(p, (HAT['bottom'][0] + 1, HAT['bottom'][1] + 1, 6, 4), color, 0.85)


def paint_mustache(p, color):
    fx, fy = HEAD['front'][0], HEAD['front'][1]
    for ex in (1, 2, 5, 6):
        p.dot(fx + ex, fy + 6, color)
    p.dot(fx + 1, fy + 7, color, 0.9)
    p.dot(fx + 6, fy + 7, color, 0.9)


def paint_arrows(p, color, slim):
    """The airbender's tattoos: an arrow from the nape over the crown to the brow, and down the backs of the arms."""
    fx, fy = HEAD['front'][0], HEAD['front'][1]
    tx, ty = HEAD['top'][0], HEAD['top'][1]
    p.fill((tx + 3, ty, 2, 8), color, 0.0)
    p.fill((HEAD['back'][0] + 3, HEAD['back'][1], 2, 4), color, 0.0)
    p.fill((fx + 3, fy, 2, 2), color, 0.0)
    for (dx, dy) in ((2, 2), (5, 2), (3, 2), (4, 2), (1, 1), (6, 1)):
        p.dot(fx + dx, fy + dy, color)
    for sleeve in (cube(40, 32, 3 if slim else 4, 12, 4), cube(48, 48, 3 if slim else 4, 12, 4)):
        x, y, w, h = sleeve['back']
        p.fill((x + w // 2 - (0 if slim else 1), y + 1, 1 if slim else 2, 7), color, 0.0)
        p.dot(x + 0, y + 7, color)
        p.dot(x + w - 1, y + 7, color)


def paint_scar(p):
    fx, fy = HEAD['front'][0], HEAD['front'][1]
    scar = (150, 78, 58)
    for (dx, dy) in ((4, 2), (5, 2), (6, 2), (4, 3), (6, 3), (7, 3), (5, 5), (6, 5), (7, 4), (7, 5)):
        p.dot(fx + dx, fy + dy, scar)
    p.dot(fx + 6, fy + 4, (200, 120, 90))
    p.dot(fx + 5, fy + 4, (120, 60, 50))


def paint_mask(p, base, mark):
    fx, fy = HEAD['front'][0], HEAD['front'][1]
    p.fill((fx, fy, 8, 8), base, 0.015)
    for i in range(8):
        p.dot(fx + i, fy, base, 0.85)
        p.dot(fx + i, fy + 7, base, 0.85)
    for ex in (1, 2, 5, 6):
        p.dot(fx + ex, fy + 4, (35, 35, 40))
    p.fill((fx + 3, fy + 1, 2, 2), mark, 0.0)
    p.dot(fx + 3, fy + 6, (170, 60, 60))
    p.dot(fx + 4, fy + 6, (170, 60, 60))
    p.dot(fx + 3, fy + 5, base, 0.9)
    p.dot(fx + 4, fy + 5, base, 0.9)


def paint_headband(p, color, row=2):
    for name in ('front', 'left', 'right', 'back'):
        x, y, w, h = HEAD[name]
        p.fill((x, y + row, w, 1), color, 0.03)


def paint_crown(p, color):
    p.fill((HAT['front'][0] + 3, HAT['front'][1], 2, 2), color, 0.05)
    p.dot(HAT['top'][0] + 3, HAT['top'][1] + 1, color)
    p.dot(HAT['top'][0] + 4, HAT['top'][1] + 1, color)
    p.dot(HAT['top'][0] + 3, HAT['top'][1] + 2, color, 0.9)
    p.dot(HAT['top'][0] + 4, HAT['top'][1] + 2, color, 0.9)


# ---- the garments of the four nations ----

def paint_body(p, s, slim):
    skin = s['skin']
    shirt = s['shirt']
    pants = s['pants']
    boots = s['boots']
    trim = s.get('trim', shade(shirt, 0.7))
    style = s.get('garment', 'robe')
    sleeves = s.get('sleeves', shirt)
    bare = s.get('bare', False)
    arms = (arm(40, 16, slim), arm(32, 48, slim))

    p.faces(BODY, shirt)
    for a in arms:
        p.faces(a, skin if bare else sleeves, grain=0.03 if bare else 0.04)
    for leg in (R_LEG, L_LEG):
        p.faces(leg, pants)
        # A seam down the outside of each leg, and boots with a top edge and a sole.
        p.cols(leg, shade(pants, 0.85), (0,), names=('front',))
        p.rows(leg, boots, (9, 10, 11))
        p.rows(leg, shade(boots, 1.25), (9,))
        p.rows(leg, shade(boots, 0.6), (11,))

    fx, fy, fw, fh = BODY['front']
    if style == 'robe':
        # Earth Kingdom: a crossed tunic (two diagonal lapels), a wide belt with a buckle, a darker skirt.
        for i in range(4):
            p.dot(fx + 3 - i, fy + i + 1, trim)
            p.dot(fx + 4 + i, fy + i + 1, trim)
        p.rows(BODY, s.get('belt', trim), (7, 8))
        p.dot(fx + 3, fy + 7, shade(s.get('belt', trim), 1.4))
        p.dot(fx + 4, fy + 7, shade(s.get('belt', trim), 1.4))
        p.rows(BODY, shade(shirt, 0.88), (9, 10, 11))
        p.rows(BODY, trim, (0,), names=('front', 'back'))
        if not bare:
            for a in arms:
                p.rows(a, trim, (8,))
    elif style == 'nomad':
        # Air Nomads: a saffron tunic under a maroon shoulder cape, a sash from the right shoulder, the belt.
        cape = s.get('cape', (150, 60, 40))
        p.rows(BODY, cape, (0, 1, 2, 3))
        p.rows(BODY, shade(cape, 0.8), (3,))
        for a in arms:
            if not bare:
                p.rows(a, cape, (0, 1, 2))
            else:
                p.rows(a, cape, (0, 1))
        for i in range(8):
            j = 4 + i // 2
            if j < 8:
                p.dot(fx + i, fy + j, cape, 0.9)
        p.rows(BODY, s.get('belt', shade(cape, 0.9)), (8,))
        p.rows(BODY, shade(shirt, 0.9), (9, 10, 11))
    elif style == 'tribe':
        # Water Tribe: a parka with fur at the collar, the hem and the cuffs, a pale belt, a waist line.
        fur = s.get('fur', (232, 232, 240))
        p.rows(BODY, fur, (0, 1), grain=0.08)
        p.rows(BODY, fur, (10, 11), grain=0.08)
        p.rows(BODY, s.get('belt', fur), (7,))
        p.dot(fx + 3, fy + 7, shade(s.get('belt', fur), 0.7))
        p.dot(fx + 4, fy + 7, shade(s.get('belt', fur), 0.7))
        for i in range(2, 8):
            p.dot(fx + 3, fy + i, shade(shirt, 0.85))
        if not bare:
            for a in arms:
                p.rows(a, fur, (8, 9), grain=0.08)
        for leg in (R_LEG, L_LEG):
            p.rows(leg, fur, (8,), grain=0.08)
    elif style == 'nation':
        # Fire Nation: dark armour with pauldrons, a chest plate edged in gold, a gold belt, black boots.
        gold = s.get('trim', (200, 160, 60))
        plate = shade(shirt, 1.12)
        p.fill((fx + 1, fy + 2, 6, 5), plate, 0.04)
        p.cols(BODY, gold, (0, 7), names=('front', 'back'))
        p.rows(BODY, gold, (1,), names=('front', 'back'))
        p.dot(fx + 3, fy + 3, gold, 0.9)
        p.dot(fx + 4, fy + 3, gold, 0.9)
        p.rows(BODY, gold, (8,))
        p.rows(BODY, shade(shirt, 0.75), (9, 10, 11))
        for a in arms:
            p.rows(a, shade(shirt, 0.7), (0, 1, 2))
            p.rows(a, gold, (2,))
            if not bare:
                p.rows(a, gold, (9,))
    elif style == 'lotus':
        # The White Lotus: blue robes with wide white edges and the lotus tile on the chest.
        white = (235, 235, 240)
        p.cols(BODY, white, (0, 7), names=('front', 'back'))
        p.rows(BODY, white, (0,), names=('front', 'back'))
        for i in range(3):
            p.dot(fx + 3 - i, fy + i + 1, white)
            p.dot(fx + 4 + i, fy + i + 1, white)
        p.rows(BODY, white, (8,))
        p.rows(BODY, shade(shirt, 0.9), (9, 10, 11))
        p.fill((fx + 3, fy + 4, 2, 2), white, 0.0)
        p.dot(fx + 3, fy + 4, (205, 70, 70))
        for a in arms:
            p.rows(a, white, (9,))
    elif style == 'armor':
        # Lin's police armour: grey metal plates with seams, pauldrons, a dark undersuit.
        metal = s.get('metal', (135, 140, 148))
        p.fill((fx, fy, 8, 8), metal, 0.05)
        p.rows(BODY, shade(metal, 0.7), (2, 5), names=('front', 'back'))
        p.cols(BODY, shade(metal, 0.75), (3,), names=('front',))
        p.rows(BODY, metal, (0, 1, 2, 3, 4, 5, 6, 7), names=('back', 'left', 'right'))
        p.rows(BODY, shade(metal, 0.7), (2, 5), names=('back',))
        for a in arms:
            p.rows(a, metal, (0, 1, 2))
            p.rows(a, shade(metal, 0.7), (2,))
            p.rows(a, metal, (8, 9))
        for leg in (R_LEG, L_LEG):
            p.rows(leg, metal, (3, 4))
    # Hands: skin, with the thumb side a touch darker.
    if not s.get('noarms'):
        for a in arms:
            p.rows(a, skin, (10, 11), grain=0.025)
            x, y, w, h = a['front']
            p.dot(x, y + 10, skin, 0.9)


LOOKS = {
    # name: (slim, spec)
    'toph': (True, dict(garment='robe', skin=(236, 208, 176), eyes=(190, 205, 195), hair=(28, 26, 28), style='bun', shirt=(96, 136, 72), pants=(226, 212, 168),
                        boots=(236, 208, 176), sleeves=(70, 108, 58), belt=(60, 92, 50), trim=(224, 204, 124), headband=(60, 92, 50), headband_row=1)),
    'bumi': (False, dict(garment='robe', skin=(226, 190, 152), eyes=(70, 60, 50), hair=(236, 236, 236), style='wild', shirt=(74, 112, 62), pants=(58, 88, 50),
                         boots=(92, 70, 48), bare=True, belt=(204, 170, 80), trim=(204, 170, 80), beard=True, bigbeard=True)),
    'lin': (True, dict(garment='armor', skin=(230, 200, 170), eyes=(84, 102, 92), hair=(176, 176, 182), style='short', shirt=(60, 64, 60), pants=(52, 56, 52),
                       boots=(40, 40, 40), metal=(138, 142, 150))),
    'suyin': (True, dict(garment='robe', skin=(230, 200, 170), eyes=(84, 102, 92), hair=(206, 206, 212), style='long', shirt=(62, 112, 84), pants=(44, 82, 62),
                         boots=(58, 58, 58), belt=(186, 186, 192), trim=(186, 186, 192))),
    'ghazan': (False, dict(garment='robe', skin=(214, 178, 142), eyes=(60, 50, 40), hair=(36, 30, 30), style='long', shirt=(214, 178, 142), pants=(110, 90, 60),
                           boots=(70, 50, 40), bare=True, belt=(92, 72, 50), trim=(110, 90, 60), beard=True, full=True)),
    'bolin': (False, dict(garment='robe', skin=(232, 202, 172), eyes=(84, 136, 94), hair=(40, 35, 35), style='short', shirt=(62, 104, 62), pants=(50, 60, 50),
                          boots=(62, 50, 40), belt=(92, 72, 50), trim=(204, 184, 124))),

    'tenzin': (False, dict(garment='nomad', skin=(230, 200, 170), eyes=(72, 72, 84), hair=(60, 50, 45), style='bald', shirt=(214, 130, 50), pants=(214, 130, 50),
                           boots=(120, 80, 50), bare=True, cape=(150, 60, 40), belt=(120, 50, 36), arrows=(70, 110, 210), beard=True)),
    'zaheer': (False, dict(garment='robe', skin=(224, 194, 164), eyes=(60, 60, 60), hair=(44, 44, 44), style='bald', shirt=(150, 122, 72), pants=(84, 72, 60),
                           boots=(60, 50, 40), bare=True, belt=(60, 50, 40), trim=(120, 96, 56), beard=True, full=True)),
    'jinora': (True, dict(garment='nomad', skin=(236, 206, 176), eyes=(84, 72, 60), hair=(72, 52, 40), style='bob', shirt=(224, 172, 82), pants=(224, 172, 82),
                          boots=(120, 80, 50), bare=True, cape=(164, 72, 52), belt=(140, 60, 44), arrows=(70, 110, 210))),
    'gyatso': (False, dict(garment='nomad', skin=(230, 200, 170), eyes=(72, 72, 84), hair=(222, 222, 222), style='bald', shirt=(216, 152, 62), pants=(216, 152, 62),
                           boots=(120, 80, 50), bare=True, cape=(150, 60, 40), belt=(120, 50, 36), arrows=(70, 110, 210), mustache=True)),
    'jae': (False, dict(garment='nomad', skin=(200, 160, 120), eyes=(60, 50, 40), hair=(40, 35, 30), style='short', shirt=(222, 152, 60), pants=(120, 90, 60),
                        boots=(80, 60, 40), cape=(150, 70, 50), belt=(150, 70, 50), arrows=(70, 110, 210))),
    'kartik': (False, dict(garment='nomad', skin=(170, 125, 90), eyes=(50, 40, 35), hair=(30, 25, 25), style='topknot', shirt=(204, 132, 50), pants=(100, 80, 60),
                           boots=(70, 50, 40), bare=True, cape=(150, 60, 40), belt=(150, 60, 40), arrows=(70, 110, 210))),

    'katara': (True, dict(garment='tribe', skin=(190, 140, 100), eyes=(70, 130, 200), hair=(72, 46, 30), style='loops', shirt=(62, 112, 172), pants=(50, 90, 150),
                          boots=(110, 85, 60), fur=(232, 232, 240), belt=(232, 232, 240))),
    'amon': (False, dict(garment='tribe', skin=(210, 180, 150), eyes=(40, 40, 40), hair=(40, 40, 45), style='slick', shirt=(46, 52, 62), pants=(36, 40, 50),
                         boots=(30, 30, 35), fur=(96, 96, 108), belt=(96, 96, 108), mask=((236, 226, 212), (190, 40, 40)), hood=(40, 40, 45))),
    'ming_hua': (True, dict(garment='tribe', skin=(200, 165, 130), eyes=(60, 90, 130), hair=(30, 30, 35), style='long', shirt=(72, 82, 102), pants=(46, 50, 66),
                            boots=(40, 40, 50), fur=(120, 130, 150), belt=(120, 130, 150), noarms=True)),
    'pakku': (False, dict(garment='lotus', skin=(216, 186, 156), eyes=(80, 90, 110), hair=(226, 226, 232), style='topknot', shirt=(62, 112, 172), pants=(50, 90, 150),
                          boots=(100, 80, 60), beard=True, mustache=True)),

    'zuko': (False, dict(garment='nation', skin=(236, 206, 176), eyes=(190, 150, 60), hair=(36, 30, 30), style='short', shirt=(132, 30, 30), pants=(70, 30, 30),
                         boots=(40, 30, 30), trim=(204, 162, 60), scar=True)),
    'iroh': (False, dict(garment='lotus', skin=(230, 200, 170), eyes=(190, 150, 60), hair=(222, 222, 222), style='topknot', shirt=(62, 112, 172), pants=(50, 90, 150),
                         boots=(60, 50, 40), beard=True, bigbeard=True)),
    'azula': (True, dict(garment='nation', skin=(236, 212, 182), eyes=(190, 150, 60), hair=(36, 30, 30), style='fringe', shirt=(150, 30, 30), pants=(80, 30, 30),
                         boots=(40, 30, 30), trim=(204, 162, 60), topknot=True)),
    'jeong_jeong': (False, dict(garment='lotus', skin=(224, 194, 164), eyes=(80, 70, 60), hair=(226, 226, 232), style='wild', shirt=(62, 112, 172), pants=(50, 90, 150),
                                boots=(60, 50, 40), beard=True, full=True)),
    # The Avatars.
    'aang': (False, dict(garment='nomad', skin=(236, 206, 176), eyes=(96, 96, 112), hair=(60, 50, 45), style='bald', shirt=(222, 150, 50), pants=(214, 130, 50),
                         boots=(120, 80, 50), bare=True, cape=(170, 60, 40), belt=(140, 60, 44), arrows=(70, 110, 210))),
    'korra': (True, dict(garment='tribe', skin=(196, 146, 104), eyes=(70, 150, 220), hair=(60, 36, 24), style='long', shirt=(62, 112, 172), pants=(50, 90, 150),
                         boots=(100, 70, 50), belt=(200, 180, 120), trim=(226, 226, 232))),
    'roku': (False, dict(garment='nation', skin=(230, 200, 170), eyes=(190, 150, 60), hair=(226, 226, 232), style='long', shirt=(140, 30, 30), pants=(80, 30, 30),
                         boots=(40, 30, 30), trim=(204, 162, 60), beard=True, full=True, topknot=True)),
    'kyoshi': (True, dict(garment='robe', skin=(240, 236, 232), eyes=(60, 120, 90), hair=(28, 26, 28), style='long', shirt=(70, 120, 70), pants=(50, 90, 60),
                          boots=(40, 40, 40), belt=(204, 170, 80), trim=(204, 170, 80), headband=(204, 170, 80), headband_row=1)),
    'ozai': (False, dict(garment='nation', skin=(236, 206, 176), eyes=(190, 150, 60), hair=(36, 30, 30), style='long', shirt=(120, 20, 20), pants=(60, 20, 20),
                         boots=(40, 30, 30), trim=(204, 162, 60), beard=True, full=True, crown=(232, 192, 70), topknot=True)),
}


def paint_look(name, slim, s):
    p = Painter(name)
    paint_head(p, s['skin'], s['eyes'], brows=s['hair'])
    if s.get('mask'):
        paint_mask(p, s['mask'][0], s['mask'][1])
        paint_hair(p, s['hood'], 'long')
    else:
        paint_hair(p, s['hair'], s['style'])
    if s.get('topknot'):
        paint_hair(p, s['hair'], 'topknot')
    paint_body(p, s, slim)
    if s.get('arrows'):
        paint_arrows(p, s['arrows'], slim)
    if s.get('scar'):
        paint_scar(p)
    if s.get('beard'):
        paint_beard(p, s['hair'], big=s.get('bigbeard', False), full=s.get('full', False))
    if s.get('mustache'):
        paint_mustache(p, s['hair'])
    if s.get('headband'):
        paint_headband(p, s['headband'], s.get('headband_row', 2))
    if s.get('crown'):
        paint_crown(p, s['crown'])
    if s.get('noarms'):
        # Ming-Hua has no arms of flesh: her sleeves are water.
        for a in (arm(40, 16, slim), arm(32, 48, slim)):
            p.faces(a, (150, 200, 240), grain=0.12)
            p.speckle(a['front'], (210, 235, 255), 0.2)
    p.save(os.path.join(SUIT, name + '.png'))


def paint_shell(name, base, crack):
    """A full-body shell (earth or metal) on the base layer, with transparent eyes."""
    p = Painter(name)
    for c in (HEAD, BODY, R_LEG, L_LEG, arm(40, 16, False), arm(32, 48, False)):
        p.faces(c, base, grain=0.12, vgrad=0.0)
        for face, (x, y, w, h) in c.items():
            for i in range(x, x + w):
                for j in range(y, y + h):
                    if p.rnd.random() < 0.12:
                        p.px[i, j] = shade(crack, 1.0 + p.rnd.uniform(-0.1, 0.1))
            if h >= 8:
                for j in range(y + 3, y + h, 4):
                    for i in range(x, x + w):
                        if p.rnd.random() < 0.6:
                            p.px[i, j] = shade(crack, 0.9)
    fx, fy = HEAD['front'][0], HEAD['front'][1]
    for ex in (1, 2, 5, 6):
        p.px[fx + ex, fy + 4] = (0, 0, 0, 0)
    p.save(os.path.join(ENTITY, name + '.png'))


def paint_lemur():
    """Momo, the flying lemur: his 64x32 texture over his model's layout (LemurModel.createBodyLayer). White fur, huge
    bat ears brown outside with a cream band inside and dark tips, big green eyes ringed in black, tan wing membranes,
    a long white tail ringed in brown."""
    p = Painter('lemur', (64, 32))
    white = (240, 238, 232)
    brown = (128, 96, 70)
    tan = (176, 144, 112)
    dark = (40, 34, 30)
    pink = (204, 150, 140)
    body = cube(0, 0, 6, 4, 8)
    head = cube(0, 13, 6, 5, 4)
    ears = (cube(28, 0, 4, 6, 1), cube(40, 0, 4, 6, 1))
    wings = (cube(0, 22, 8, 1, 6), cube(28, 22, 8, 1, 6))
    tail = cube(34, 13, 1, 1, 6)
    legs = [cube(48 + 4 * i, 13, 1, 3, 1) for i in range(4)]
    # Body: white all over, a faint grey down the spine.
    p.faces(body, white, grain=0.04, vgrad=0.0)
    x, y, w, h = body['top']
    p.fill((x + 2, y, 2, h), shade(white, 0.9), 0.04)
    # Head: white, the big green eyes ringed in black, a small dark nose.
    p.faces(head, white, grain=0.04, vgrad=0.0)
    fx, fy = head['front'][0], head['front'][1]
    green = (110, 190, 90)
    for ex in (0, 4):
        p.fill((fx + ex, fy, 2, 1), dark, 0.0)
        p.fill((fx + ex, fy + 1, 2, 2), green, 0.03)
        p.dot(fx + ex + (1 if ex == 0 else 0), fy + 2, dark)
        p.fill((fx + ex, fy + 3, 2, 1), shade(white, 0.85), 0.0)
    p.dot(fx + 2, fy + 3, dark)
    p.dot(fx + 3, fy + 3, dark)
    p.dot(fx + 2, fy + 4, pink)
    p.dot(fx + 3, fy + 4, pink)
    # Ears: brown outside with a cream band inside, dark tips.
    for ear in ears:
        p.faces(ear, brown, grain=0.06, vgrad=0.0)
        for face in ('front', 'back'):
            x, y, w, h = ear[face]
            p.fill((x + 1, y + 1, 2, h - 1), (226, 206, 170), 0.05)
            p.fill((x, y, w, 1), dark, 0.03)
    # Wings: tan membranes with brown ribs, paler below.
    for wing in wings:
        p.faces(wing, tan, grain=0.06, vgrad=0.0)
        x, y, w, h = wing['top']
        for i in range(0, w, 2):
            p.fill((x + i, y, 1, h), brown, 0.06)
        p.fill(wing['bottom'], shade(tan, 1.15), 0.05)
    # Tail: white with brown rings.
    for name, rect in tail.items():
        x, y, w, h = rect
        for k in range(max(w, h)):
            ring = brown if k % 2 == 0 else white
            if w >= h:
                p.fill((x + k, y, 1, h), ring, 0.05)
            else:
                p.fill((x, y + k, w, 1), ring, 0.05)
    for leg in legs:
        p.faces(leg, white, grain=0.04, vgrad=0.0)
        p.rows(leg, shade(white, 0.8), (2,))
    p.save(os.path.join(ENTITY, 'lemur.png'))

def paint_pathik():
    """Guru Pathik: an old, thin, bald hermit, brown from the sun, a long white beard down his chest, bushy white brows,
    a saffron-orange loincloth and sash, bare legs and feet (64x64 on the wide player model, entity/guru_pathik.png)."""
    p = Painter('guru_pathik')
    skin = (128, 78, 44)
    white = (238, 232, 216)
    cloth = (214, 170, 60)
    paint_head(p, skin, (60, 50, 44), brows=white)
    paint_hair(p, white, 'bald')
    # Deep lines of age on the brow and the cheeks.
    fx, fy = HEAD['front'][0], HEAD['front'][1]
    for i in range(1, 7):
        p.dot(fx + i, fy + 1, skin, 0.86)
    p.dot(fx + 1, fy + 5, skin, 0.85)
    p.dot(fx + 6, fy + 5, skin, 0.85)
    # Bushy white brows over the eyes.
    for i in (0, 1, 2, 5, 6, 7):
        p.dot(fx + i, fy + 2, white)
    paint_beard(p, white, big=True, full=True)
    paint_mustache(p, white)
    for side in ('left', 'right'):
        sx, sy = HEAD[side][0], HEAD[side][1]
        for j in (5, 6, 7):
            for i in range(2, 8) if side == 'right' else range(0, 6):
                p.dot(sx + i, sy + j, white, 0.95)
    # The bare body, thin: the ribs shaded.
    p.faces(BODY, skin, grain=0.03)
    bx, by = BODY['front'][0], BODY['front'][1]
    for j in (4, 6):
        for i in (1, 2, 5, 6):
            p.dot(bx + i, by + j, skin, 0.88)
    # The loincloth and the sash from the left shoulder.
    p.rows(BODY, cloth, (8, 9, 10, 11), names=('front', 'left', 'right', 'back'))
    p.rows(BODY, shade(cloth, 0.75), (8,), names=('front', 'left', 'right', 'back'))
    for k in range(8):
        p.dot(bx + 7 - k, by + k, shade(cloth, 0.9))
    # The beard falls down the chest, narrowing to a point.
    for j, (a, b) in enumerate([(0, 8), (1, 7), (1, 7), (2, 6), (2, 6), (3, 5), (3, 5), (3, 5), (3, 5)]):
        for i in range(a, b):
            p.dot(bx + i, by + j, white, 0.92 + 0.08 * ((i + j) % 2))
    arms = (arm(40, 16, False), arm(32, 48, False))
    for a in arms:
        p.faces(a, skin, grain=0.03)
    for leg in (R_LEG, L_LEG):
        p.faces(leg, skin, grain=0.03)
        p.rows(leg, cloth, (0, 1, 2), names=('front', 'left', 'right', 'back'))
        p.rows(leg, shade(skin, 0.85), (11,))
    p.save(os.path.join(ENTITY, 'guru_pathik.png'))


def paint_bison():
    """The sky bison (Appa): 256x256 over SkyBisonModel.createBodyLayer. White fur, the brown arrow from the tail over
    the back to the brow, tan horns, a dark nose, small brown eyes, grey toes; the saddle brown leather with a red
    blanket edge and a wooden rim."""
    p = Painter('sky_bison', (256, 256))
    fur = (238, 234, 222)
    brown = (122, 82, 52)
    body = cube(0, 0, 40, 26, 56)
    head = cube(0, 82, 26, 20, 16)
    bangs = cube(84, 82, 24, 5, 6)
    nose = cube(84, 93, 12, 6, 3)
    horn = cube(144, 82, 4, 4, 4)
    tip = cube(160, 82, 3, 5, 3)
    leg = cube(192, 0, 10, 12, 10)
    tail = cube(114, 93, 28, 4, 22)
    pad = cube(0, 128, 32, 4, 28)
    rim_end = cube(120, 128, 32, 5, 2)
    rim_side = cube(120, 136, 2, 5, 24)
    for c in (body, head, bangs, leg, tail):
        p.faces(c, fur, grain=0.06, vgrad=0.1)
    # The tufts of a shaggy coat.
    for c in (body, head, bangs, tail):
        for face, rect in c.items():
            p.speckle(rect, shade(fur, 0.88), 0.12)
    # The belly a little grey.
    p.fill(body['bottom'], shade(fur, 0.86), 0.06)
    # The arrow down the back: the body's top face runs 40 across and 56 from front (row 0) to back.
    x, y, w, h = body['top']
    p.fill((x + 14, y + 10, 12, h - 10), brown, 0.07)
    for k in range(10):
        p.fill((x + 14 - (10 - k), y + k, 12 + 2 * (10 - k), 1), brown, 0.07)
    # ...and over the brow: the head's top and the upper front, pointing down at the nose.
    x, y, w, h = head['top']
    p.fill((x + 8, y, 10, h), brown, 0.07)
    x, y, w, h = head['front']
    for k in range(7):
        p.fill((x + 6 + k, y + k, w - 12 - 2 * k, 1), brown, 0.07)
    x, y, w, h = bangs['top']
    p.fill((x + 7, y, 10, h), brown, 0.07)
    x, y, w, h = bangs['front']
    p.fill((x + 7, y, 10, 2), brown, 0.07)
    # The eyes: small and kind, low on the broad face.
    x, y, w, h = head['front']
    for ex in (5, 19):
        p.fill((x + ex, y + 9, 3, 3), (60, 40, 28), 0.0)
        p.dot(x + ex + 1, y + 9, (230, 230, 230))
        p.fill((x + ex - 1, y + 8, 5, 1), shade(fur, 0.8), 0.0)
    # The mouth.
    p.fill((x + 8, y + 17, 10, 1), shade(fur, 0.6), 0.0)
    # The nose: dark, with two nostrils.
    p.faces(nose, (96, 86, 80), grain=0.05, vgrad=0.0)
    x, y, w, h = nose['front']
    p.fill((x + 2, y + 2, 2, 2), (40, 34, 30), 0.0)
    p.fill((x + 8, y + 2, 2, 2), (40, 34, 30), 0.0)
    # Horns: tan, darker at the tip.
    p.faces(horn, (204, 176, 128), grain=0.05)
    p.faces(tip, (176, 146, 100), grain=0.05)
    # Legs: grey toes.
    p.rows(leg, (120, 112, 104), (10, 11))
    p.fill(leg['bottom'], (110, 100, 94), 0.05)
    x, y, w, h = leg['front']
    for k in (2, 5, 8):
        p.fill((x + k, y + h - 2, 1, 2), (80, 74, 70), 0.0)
    # The tail: white, the arrow's end in brown.
    x, y, w, h = tail['top']
    p.fill((x + 9, y, 10, 6), brown, 0.07)
    # The saddle: leather, a red blanket edge, a wooden rim.
    p.faces(pad, (128, 74, 42), grain=0.06, vgrad=0.0)
    for face in ('front', 'back', 'left', 'right'):
        x, y, w, h = pad[face]
        p.fill((x, y, w, h), (176, 56, 40), 0.05)
    x, y, w, h = pad['top']
    p.fill((x, y, w, 2), (176, 56, 40), 0.05)
    p.fill((x, y + h - 2, w, 2), (176, 56, 40), 0.05)
    for c in (rim_end, rim_side):
        p.faces(c, (196, 156, 92), grain=0.06, vgrad=0.0)
        for face in ('front', 'back', 'left', 'right'):
            x, y, w, h = c[face]
            p.fill((x, y + h - 1, w, 1), (150, 110, 60), 0.05)
    p.save(os.path.join(ENTITY, 'sky_bison.png'))


def paint_items():
    os.makedirs(ITEM, exist_ok=True)
    scrolls = {'earth_scroll': (90, 140, 70), 'air_scroll': (220, 170, 60), 'water_scroll': (50, 120, 200), 'fire_scroll': (200, 60, 30), 'avatar_scroll': (127, 214, 255)}
    for name, ribbon in scrolls.items():
        img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        paper = (235, 220, 180, 255)
        d.rectangle((3, 2, 12, 13), fill=paper, outline=(170, 150, 100, 255))
        d.rectangle((2, 1, 13, 3), fill=(215, 195, 150, 255), outline=(150, 130, 90, 255))
        d.rectangle((2, 12, 13, 14), fill=(215, 195, 150, 255), outline=(150, 130, 90, 255))
        d.rectangle((6, 4, 9, 11), fill=ribbon + (255,))
        d.line((5, 6, 10, 6), fill=(120, 100, 60, 255))
        d.line((5, 9, 10, 9), fill=(120, 100, 60, 255))
        img.save(os.path.join(ITEM, name + '.png'))
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse((3, 5, 12, 14), fill=(140, 95, 55, 255), outline=(90, 60, 35, 255))
    d.rectangle((6, 2, 9, 5), fill=(110, 75, 45, 255), outline=(80, 55, 30, 255))
    d.rectangle((6, 1, 9, 2), fill=(60, 130, 200, 255))
    d.line((4, 8, 11, 8), fill=(110, 75, 45, 255))
    img.save(os.path.join(ITEM, 'water_skin.png'))
    staff = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(staff)
    d.line((2, 13, 13, 2), fill=(120, 80, 40, 255), width=2)
    d.rectangle((12, 1, 14, 3), fill=(90, 60, 30, 255))
    staff.save(os.path.join(ITEM, 'air_glider.png'))
    wings = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(wings)
    d.polygon([(3, 3), (9, 6), (6, 9)], fill=(235, 235, 235, 255), outline=(200, 200, 200, 255))
    d.polygon([(9, 6), (13, 9), (10, 12), (6, 9)], fill=(225, 225, 225, 255), outline=(200, 200, 200, 255))
    wings.save(os.path.join(ITEM, 'air_glider_wings.png'))


def main():
    for name, (slim, spec) in LOOKS.items():
        paint_look(name, slim, spec)
    paint_shell('earth_armor', (120, 105, 85), (80, 70, 55))
    paint_shell('metal_armor', (170, 175, 180), (110, 115, 120))
    paint_lemur()
    paint_pathik()
    paint_bison()
    paint_items()
    print('wrote', len(LOOKS), 'skins, 2 shells, the lemur, 7 item icons')


if __name__ == '__main__':
    main()
