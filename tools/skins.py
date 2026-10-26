#!/usr/bin/env python3
"""Draws every skin, shell and item icon of Enlightened from palettes (no art files are kept outside the repo).

    python3 tools/skins.py            # writes src/main/resources/assets/enlightened/textures/...

Each look is a palette and a few flags (hair style, beard, tattoos, a scar, a mask...) painted onto the standard
64x64 skin layout, with a little shading and grain so the cloth does not look flat. Run it after changing a palette;
the result is deterministic.
"""
import os
import random
import sys

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
R_PANTS = cube(0, 32, 4, 12, 4)
L_LEG = cube(16, 48, 4, 12, 4)
L_PANTS = cube(0, 48, 4, 12, 4)


def arm(x, y, slim):
    return cube(x, y, 3 if slim else 4, 12, 4)


def shade(color, f):
    r, g, b = color[:3]
    return (max(0, min(255, int(r * f))), max(0, min(255, int(g * f))), max(0, min(255, int(b * f))), 255)


class Painter:
    def __init__(self, seed):
        self.img = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
        self.px = self.img.load()
        self.rnd = random.Random(seed)

    def fill(self, rect, color, grain=0.06, dark=1.0):
        x, y, w, h = rect
        for i in range(x, x + w):
            for j in range(y, y + h):
                f = dark * (1.0 + self.rnd.uniform(-grain, grain))
                self.px[i, j] = shade(color, f)

    def faces(self, c, color, grain=0.06, sides=0.85, back=0.75, top=1.05, bottom=0.7, names=None):
        for name, rect in c.items():
            if names and name not in names:
                continue
            f = {'front': 1.0, 'left': sides, 'right': sides, 'back': back, 'top': top, 'bottom': bottom}[name]
            self.fill(rect, color, grain, f)

    def rows(self, c, color, rows, grain=0.05, names=('front', 'left', 'right', 'back')):
        """Paints horizontal rows (relative to the face's top) of the side faces: belts, trims, boots."""
        for name in names:
            x, y, w, h = c[name]
            f = {'front': 1.0, 'left': 0.85, 'right': 0.85, 'back': 0.75}[name]
            for r in rows:
                if 0 <= r < h:
                    self.fill((x, y + r, w, 1), color, grain, f)

    def dot(self, x, y, color):
        self.px[x, y] = shade(color, 1.0)

    def save(self, path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        self.img.save(path)


# ---- the parts of a character ----

def paint_head(p, skin, eyes, mouth=(150, 70, 70)):
    p.faces(HEAD, skin, grain=0.04)
    fx, fy = HEAD['front'][0], HEAD['front'][1]
    # Eyes: whites with an iris, on the fourth row; brows above; a mouth below.
    for ex in (1, 5):
        p.dot(fx + ex, fy + 4, (245, 245, 245))
        p.dot(fx + ex + 1, fy + 4, eyes)
    brow = shade(skin, 0.6)
    for ex in (1, 2, 5, 6):
        p.dot(fx + ex, fy + 3, brow)
    p.dot(fx + 3, fy + 6, mouth)
    p.dot(fx + 4, fy + 6, mouth)
    # Nose: a slightly darker pixel.
    p.dot(fx + 3, fy + 5, shade(skin, 0.88))


def paint_hair(p, color, style, bald=False):
    """Hair on the head and its hat layer. Styles: short, long, bob, topknot, bun, wild, loops, bald, ponytail, fringe."""
    if bald or style == 'bald':
        return
    top, back, left, right, front = HEAD['top'], HEAD['back'], HEAD['left'], HEAD['right'], HEAD['front']
    p.fill(top, color, 0.08)
    fx, fy = front[0], front[1]
    if style in ('short', 'wild', 'topknot', 'bun', 'ponytail', 'loops'):
        p.fill((back[0], back[1], 8, 3), color, 0.08, 0.8)
        p.fill((left[0], left[1], 8, 3), color, 0.08, 0.9)
        p.fill((right[0], right[1], 8, 3), color, 0.08, 0.9)
        p.fill((fx, fy, 8, 2), color, 0.08)
    if style in ('long', 'bob', 'fringe'):
        rows = 8 if style == 'long' else 6
        p.fill((back[0], back[1], 8, rows), color, 0.08, 0.8)
        p.fill((left[0], left[1], 8, rows - 1), color, 0.08, 0.9)
        p.fill((right[0], right[1], 8, rows - 1), color, 0.08, 0.9)
        p.fill((fx, fy, 8, 2), color, 0.08)
        p.dot(fx, fy + 2, shade(color, 0.95))
        p.dot(fx + 7, fy + 2, shade(color, 0.95))
    if style == 'fringe':
        p.fill((fx, fy, 8, 3), color, 0.08)
        p.dot(fx + 2, fy + 3, shade(color, 1.0))
        p.dot(fx + 5, fy + 3, shade(color, 1.0))
    if style == 'wild':
        # Spiky volume on the hat layer.
        p.fill((HAT['top'][0], HAT['top'][1], 8, 8), color, 0.15, 1.1)
        for i in range(0, 8, 2):
            p.dot(HAT['front'][0] + i, HAT['front'][1], shade(color, 1.1))
            p.dot(HAT['back'][0] + i + 1, HAT['back'][1], shade(color, 0.9))
    if style == 'topknot':
        p.fill((HAT['top'][0] + 3, HAT['top'][1] + 2, 2, 3), color, 0.1, 1.1)
        p.fill((HAT['back'][0] + 3, HAT['back'][1], 2, 1), color, 0.1)
    if style == 'bun':
        p.fill((HAT['back'][0] + 2, HAT['back'][1] + 1, 4, 3), color, 0.1, 0.9)
    if style == 'ponytail':
        p.fill((HAT['back'][0] + 3, HAT['back'][1] + 2, 2, 6), color, 0.1, 0.85)
    if style == 'loops':
        for side in (HAT['left'], HAT['right']):
            p.fill((side[0] + 1, side[1] + 2, 1, 4), color, 0.1, 0.9)
            p.fill((side[0] + 6, side[1] + 2, 1, 4), color, 0.1, 0.9)
        p.fill((HAT['back'][0] + 2, HAT['back'][1] + 3, 4, 5), color, 0.1, 0.85)


def paint_beard(p, color, big=False):
    fx, fy = HEAD['front'][0], HEAD['front'][1]
    p.fill((fx, fy + 7, 8, 1), color, 0.1)
    if big:
        p.fill((HAT['front'][0] + 1, HAT['front'][1] + 6, 6, 2), color, 0.1)
        p.fill((HAT['front'][0] + 2, HAT['front'][1] + 5, 4, 1), color, 0.1)
        p.fill((HAT['bottom'][0] + 1, HAT['bottom'][1] + 1, 6, 3), color, 0.1, 0.85)


def paint_mustache(p, color):
    fx, fy = HEAD['front'][0], HEAD['front'][1]
    for ex in (1, 2, 5, 6):
        p.dot(fx + ex, fy + 6, color)


def paint_arrows(p, color):
    """The airbender's tattoos: an arrow down the forehead to the brow, and along the backs of the arms."""
    fx, fy = HEAD['front'][0], HEAD['front'][1]
    tx, ty = HEAD['top'][0], HEAD['top'][1]
    p.fill((tx + 3, ty, 2, 8), color, 0.0)
    p.fill((HEAD['back'][0] + 3, HEAD['back'][1], 2, 3), color, 0.0)
    p.fill((fx + 3, fy, 2, 2), color, 0.0)
    p.dot(fx + 2, fy + 2, color)
    p.dot(fx + 5, fy + 2, color)
    p.dot(fx + 3, fy + 2, color)
    p.dot(fx + 4, fy + 2, color)


def paint_scar(p):
    """Zuko's scar over the left eye (the viewer's right)."""
    fx, fy = HEAD['front'][0], HEAD['front'][1]
    scar = (150, 80, 60, 255)
    for (dx, dy) in ((4, 2), (5, 2), (6, 2), (4, 3), (6, 3), (7, 3), (5, 5), (6, 5), (7, 4)):
        p.dot(fx + dx, fy + dy, scar)
    p.dot(fx + 6, fy + 4, (200, 120, 90, 255))


def paint_mask(p, base, mark):
    """Amon's mask: a pale face with a red mark on the brow and a dark hood."""
    fx, fy = HEAD['front'][0], HEAD['front'][1]
    p.fill((fx, fy, 8, 8), base, 0.02)
    for ex in (1, 2, 5, 6):
        p.dot(fx + ex, fy + 4, (40, 40, 40))
    p.fill((fx + 3, fy + 1, 2, 2), mark, 0.0)
    p.dot(fx + 3, fy + 6, (160, 60, 60))
    p.dot(fx + 4, fy + 6, (160, 60, 60))


def paint_headband(p, color, row=2):
    for name in ('front', 'left', 'right', 'back'):
        x, y, w, h = HEAD[name]
        p.fill((x, y + row, w, 1), color, 0.05)


def paint_crown(p, color):
    """A Fire Lord's flame crown on the hat layer."""
    p.fill((HAT['front'][0] + 3, HAT['front'][1], 2, 2), color, 0.05)
    p.dot(HAT['top'][0] + 3, HAT['top'][1] + 1, color)
    p.dot(HAT['top'][0] + 4, HAT['top'][1] + 1, color)


def paint_body(p, shirt, pants, boots, skin, sleeves=None, hands=True, belt=None, trim=None, collar=None,
               slim=False, robe=False, sash=None):
    sleeves = sleeves or shirt
    p.faces(BODY, shirt)
    if trim:
        # A vertical trim down the middle of the front, as a robe's edge.
        fx, fy, fw, fh = BODY['front']
        p.fill((fx + 3, fy, 2, fh), trim, 0.04)
    if collar:
        p.rows(BODY, collar, (0, 1))
    if belt:
        p.rows(BODY, belt, (8, 9))
    if sash:
        fx, fy, fw, fh = BODY['front']
        for i in range(8):
            p.dot(fx + i, fy + 2 + i // 2, sash)
    for arm_cube in (arm(40, 16, slim), arm(32, 48, slim)):
        p.faces(arm_cube, sleeves)
        if hands:
            p.rows(arm_cube, skin, (10, 11), grain=0.03)
    for leg in (R_LEG, L_LEG):
        p.faces(leg, pants)
        p.rows(leg, boots, (9, 10, 11))
        if robe:
            p.rows(leg, shirt, (0, 1, 2, 3, 4, 5))


def paint_armor_pads(p, color):
    """Shoulder pads and a chest plate on the jacket and sleeve overlays (Lin's police armour)."""
    p.faces(JACKET, color, 0.08, names=('front', 'back'))
    p.rows(JACKET, shade(color, 0.8), (0, 4, 8), names=('front', 'back'))
    for sleeve in (cube(40, 32, 4, 12, 4), cube(48, 48, 4, 12, 4)):
        p.rows(sleeve, color, (0, 1, 2), grain=0.08)


LOOKS = {
    # name: (slim, spec)
    'toph': (True, dict(skin=(235, 205, 170), eyes=(190, 200, 190), hair=(30, 30, 30), style='bun', shirt=(90, 130, 70), pants=(230, 215, 170),
                        boots=(235, 205, 170), sleeves=(235, 205, 170), belt=(60, 90, 50), collar=(220, 200, 120), headband=(60, 90, 50), headband_row=1)),
    'bumi': (False, dict(skin=(225, 190, 155), eyes=(60, 60, 60), hair=(235, 235, 235), style='wild', shirt=(70, 110, 60), pants=(60, 90, 50),
                         boots=(90, 70, 50), sleeves=(225, 190, 155), belt=(200, 170, 80), collar=(200, 170, 80), beard=True)),
    'lin': (True, dict(skin=(230, 200, 170), eyes=(80, 100, 90), hair=(170, 170, 175), style='short', shirt=(70, 75, 70), pants=(55, 60, 55),
                       boots=(40, 40, 40), sleeves=(70, 75, 70), belt=(120, 120, 125), collar=(120, 120, 125), pads=(130, 135, 140), scar=False)),
    'suyin': (True, dict(skin=(230, 200, 170), eyes=(80, 100, 90), hair=(200, 200, 205), style='long', shirt=(60, 110, 80), pants=(45, 80, 60),
                         boots=(60, 60, 60), sleeves=(60, 110, 80), belt=(180, 180, 185), collar=(180, 180, 185), trim=(180, 180, 185))),
    'ghazan': (False, dict(skin=(215, 180, 145), eyes=(60, 50, 40), hair=(35, 30, 30), style='long', shirt=(215, 180, 145), pants=(110, 90, 60),
                           boots=(70, 50, 40), sleeves=(215, 180, 145), hands=True, belt=(90, 70, 50), beard=True)),
    'bolin': (False, dict(skin=(230, 200, 170), eyes=(80, 130, 90), hair=(40, 35, 35), style='short', shirt=(60, 100, 60), pants=(50, 60, 50),
                          boots=(60, 50, 40), sleeves=(60, 100, 60), belt=(90, 70, 50), collar=(200, 180, 120))),

    'tenzin': (False, dict(skin=(230, 200, 170), eyes=(70, 70, 80), hair=(60, 50, 45), style='bald', shirt=(200, 120, 50), pants=(200, 120, 50),
                           boots=(120, 80, 50), sleeves=(230, 200, 170), hands=True, belt=(150, 60, 40), collar=(150, 60, 40), arrows=(60, 100, 200),
                           beard=True, robe=True, sash=(150, 60, 40))),
    'zaheer': (False, dict(skin=(225, 195, 165), eyes=(60, 60, 60), hair=(40, 40, 40), style='bald', shirt=(150, 120, 70), pants=(80, 70, 60),
                           boots=(60, 50, 40), sleeves=(225, 195, 165), hands=True, belt=(60, 50, 40), beard=True)),
    'jinora': (True, dict(skin=(235, 205, 175), eyes=(80, 70, 60), hair=(70, 50, 40), style='bob', shirt=(220, 170, 80), pants=(220, 170, 80),
                          boots=(120, 80, 50), sleeves=(235, 205, 175), hands=True, belt=(160, 70, 50), collar=(160, 70, 50), arrows=(60, 100, 200), robe=True)),
    'gyatso': (False, dict(skin=(230, 200, 170), eyes=(70, 70, 80), hair=(220, 220, 220), style='bald', shirt=(210, 150, 60), pants=(210, 150, 60),
                           boots=(120, 80, 50), sleeves=(230, 200, 170), hands=True, belt=(150, 60, 40), collar=(150, 60, 40), arrows=(60, 100, 200),
                           mustache=True, robe=True, sash=(150, 60, 40))),
    'jae': (False, dict(skin=(200, 160, 120), eyes=(60, 50, 40), hair=(40, 35, 30), style='short', shirt=(215, 150, 60), pants=(120, 90, 60),
                        boots=(80, 60, 40), sleeves=(215, 150, 60), belt=(150, 70, 50), collar=(230, 200, 120), arrows=(70, 110, 210))),
    'kartik': (False, dict(skin=(170, 125, 90), eyes=(50, 40, 35), hair=(30, 25, 25), style='topknot', shirt=(200, 130, 50), pants=(100, 80, 60),
                           boots=(70, 50, 40), sleeves=(170, 125, 90), hands=True, belt=(150, 60, 40), collar=(230, 200, 120), arrows=(70, 110, 210))),

    'katara': (True, dict(skin=(190, 140, 100), eyes=(70, 130, 200), hair=(70, 45, 30), style='loops', shirt=(60, 110, 170), pants=(50, 90, 150),
                          boots=(110, 85, 60), sleeves=(60, 110, 170), belt=(230, 230, 240), collar=(230, 230, 240), trim=(230, 230, 240))),
    'amon': (False, dict(skin=(210, 180, 150), eyes=(40, 40, 40), hair=(40, 40, 45), style='short', shirt=(45, 50, 60), pants=(35, 40, 50),
                         boots=(30, 30, 35), sleeves=(45, 50, 60), belt=(90, 90, 100), collar=(90, 90, 100), mask=((235, 225, 210), (190, 40, 40)), hood=(40, 40, 45))),
    'ming_hua': (True, dict(skin=(200, 165, 130), eyes=(60, 90, 130), hair=(30, 30, 35), style='long', shirt=(70, 80, 100), pants=(45, 50, 65),
                            boots=(40, 40, 50), sleeves=(70, 80, 100), hands=False, belt=(100, 110, 130), noarms=True)),
    'pakku': (False, dict(skin=(215, 185, 155), eyes=(80, 90, 110), hair=(225, 225, 230), style='topknot', shirt=(60, 110, 170), pants=(50, 90, 150),
                          boots=(100, 80, 60), sleeves=(60, 110, 170), belt=(230, 230, 240), collar=(230, 230, 240), trim=(230, 230, 240), beard=True,
                          lotus=True, robe=True)),

    'zuko': (False, dict(skin=(235, 205, 175), eyes=(190, 150, 60), hair=(35, 30, 30), style='short', shirt=(140, 30, 30), pants=(70, 30, 30),
                         boots=(40, 30, 30), sleeves=(140, 30, 30), belt=(200, 160, 60), collar=(200, 160, 60), trim=(200, 160, 60), scar=True)),
    'iroh': (False, dict(skin=(230, 200, 170), eyes=(190, 150, 60), hair=(220, 220, 220), style='topknot', shirt=(60, 110, 170), pants=(50, 90, 150),
                         boots=(60, 50, 40), sleeves=(60, 110, 170), belt=(230, 230, 240), collar=(230, 230, 240), trim=(230, 230, 240), beard=True,
                         bigbeard=True, lotus=True, robe=True)),
    'azula': (True, dict(skin=(235, 210, 180), eyes=(190, 150, 60), hair=(35, 30, 30), style='fringe', shirt=(150, 30, 30), pants=(80, 30, 30),
                         boots=(40, 30, 30), sleeves=(150, 30, 30), belt=(200, 160, 60), collar=(200, 160, 60), trim=(200, 160, 60), topknot=True)),
    'jeong_jeong': (False, dict(skin=(225, 195, 165), eyes=(80, 70, 60), hair=(225, 225, 230), style='wild', shirt=(60, 110, 170), pants=(50, 90, 150),
                                boots=(60, 50, 40), sleeves=(60, 110, 170), belt=(230, 230, 240), collar=(230, 230, 240), trim=(230, 230, 240),
                                beard=True, lotus=True, robe=True, scar=False)),
    'ozai': (False, dict(skin=(235, 205, 175), eyes=(190, 150, 60), hair=(35, 30, 30), style='long', shirt=(120, 20, 20), pants=(60, 20, 20),
                         boots=(40, 30, 30), sleeves=(120, 20, 20), belt=(200, 160, 60), collar=(200, 160, 60), trim=(200, 160, 60), beard=True,
                         crown=(230, 190, 70), topknot=True)),
}


def paint_look(name, slim, s):
    p = Painter(name)
    paint_head(p, s['skin'], s['eyes'])
    if s.get('mask'):
        paint_mask(p, s['mask'][0], s['mask'][1])
        paint_hair(p, s['hood'], 'long')
    else:
        paint_hair(p, s['hair'], s['style'])
    if s.get('topknot'):
        paint_hair(p, s['hair'], 'topknot')
    if s.get('arrows'):
        paint_arrows(p, s['arrows'])
    if s.get('scar'):
        paint_scar(p)
    if s.get('beard'):
        paint_beard(p, s['hair'], big=s.get('bigbeard', False))
    if s.get('mustache'):
        paint_mustache(p, s['hair'])
    if s.get('headband'):
        paint_headband(p, s['headband'], s.get('headband_row', 2))
    if s.get('crown'):
        paint_crown(p, s['crown'])
    paint_body(p, s['shirt'], s['pants'], s['boots'], s['skin'], sleeves=s.get('sleeves'), hands=s.get('hands', True),
               belt=s.get('belt'), trim=s.get('trim'), collar=s.get('collar'), slim=slim, robe=s.get('robe', False), sash=s.get('sash'))
    if s.get('pads'):
        paint_armor_pads(p, s['pads'])
    if s.get('lotus'):
        # The White Lotus tile on the chest.
        fx, fy = BODY['front'][0], BODY['front'][1]
        p.fill((fx + 3, fy + 4, 2, 2), (245, 245, 245), 0.0)
        p.dot(fx + 3, fy + 4, (200, 60, 60))
    if s.get('noarms'):
        # Ming-Hua has no arms: the sleeves are water, drawn as pale blue.
        for arm_cube in (arm(40, 16, slim), arm(32, 48, slim)):
            p.faces(arm_cube, (150, 200, 240), grain=0.1)
    p.save(os.path.join(SUIT, name + '.png'))


def paint_shell(name, base, crack, eyes_open=True):
    """A full-body shell (earth or metal) on the base layer, with transparent eyes."""
    p = Painter(name)
    for c in (HEAD, BODY, R_LEG, L_LEG, arm(40, 16, False), arm(32, 48, False)):
        p.faces(c, base, grain=0.12, sides=0.9, back=0.85)
        # Cracks and plates: scattered darker pixels and a few lines.
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
    if eyes_open:
        fx, fy = HEAD['front'][0], HEAD['front'][1]
        for ex in (1, 2, 5, 6):
            p.px[fx + ex, fy + 4] = (0, 0, 0, 0)
    p.save(os.path.join(ENTITY, name + '.png'))


def paint_items():
    os.makedirs(ITEM, exist_ok=True)
    scrolls = {'earth_scroll': (90, 140, 70), 'air_scroll': (220, 170, 60), 'water_scroll': (50, 120, 200), 'fire_scroll': (200, 60, 30)}
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
    paint_items()
    print('wrote', len(LOOKS), 'skins, 2 shells, 7 item icons')


if __name__ == '__main__':
    main()
