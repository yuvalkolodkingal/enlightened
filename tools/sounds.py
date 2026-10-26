#!/usr/bin/env python3
"""Synthesises every sound of Enlightened from scratch (no samples): numpy oscillators, noise, simple filters and a
little reverb, written as WAV and encoded to Ogg Vorbis with ffmpeg.

    python3 tools/sounds.py          # writes src/main/resources/assets/enlightened/sounds/sfx/*.ogg

Every recipe is a few lines below; the result is deterministic.
"""
import math
import os
import subprocess
import sys
import wave

import numpy as np

SR = 44100
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'enlightened', 'sounds', 'sfx')
rng = np.random.default_rng(7)


def t(dur):
    return np.arange(int(SR * dur)) / SR


def sine(freq, dur, phase=0.0):
    return np.sin(2 * np.pi * freq * t(dur) + phase)


def sweep(f0, f1, dur, curve=1.0):
    tt = t(dur)
    f = f0 + (f1 - f0) * (tt / dur) ** curve
    phase = 2 * np.pi * np.cumsum(f) / SR
    return np.sin(phase)


def noise(dur):
    return rng.uniform(-1, 1, int(SR * dur))


def env(x, a=0.01, d=0.1, s=0.6, r=0.2):
    n = len(x)
    out = np.ones(n)
    na, nd, nr = int(a * SR), int(d * SR), int(r * SR)
    na = min(na, n)
    out[:na] = np.linspace(0, 1, na)
    nd = min(nd, n - na)
    out[na:na + nd] = np.linspace(1, s, nd)
    out[na + nd:] = s
    nr = min(nr, n)
    if nr > 0:
        out[n - nr:] *= np.linspace(1, 0, nr)
    return x * out


def lowpass(x, cutoff):
    rc = 1.0 / (2 * np.pi * cutoff)
    dt = 1.0 / SR
    alpha = dt / (rc + dt)
    y = np.zeros_like(x)
    acc = 0.0
    for i in range(len(x)):
        acc += alpha * (x[i] - acc)
        y[i] = acc
    return y


def highpass(x, cutoff):
    return x - lowpass(x, cutoff)


def reverb(x, decay=0.3, delays=(0.029, 0.037, 0.053, 0.071)):
    out = x.copy()
    for d in delays:
        n = int(d * SR)
        buf = np.zeros(len(x) + n)
        buf[n:] = x * decay
        out = out + buf[:len(x)]
    return out


def fade(x, ms=8):
    n = min(len(x), int(SR * ms / 1000))
    x[:n] *= np.linspace(0, 1, n)
    x[-n:] *= np.linspace(1, 0, n)
    return x


def norm(x, peak=0.9):
    m = np.max(np.abs(x)) or 1.0
    return x / m * peak


def mix(*parts):
    n = max(len(p) for p in parts)
    out = np.zeros(n)
    for p in parts:
        out[:len(p)] += p
    return out


def write(name, x):
    os.makedirs(OUT, exist_ok=True)
    x = fade(norm(x))
    wav = os.path.join(OUT, name + '.wav')
    with wave.open(wav, 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((x * 32767).astype(np.int16).tobytes())
    ogg = os.path.join(OUT, name + '.ogg')
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav, '-c:a', 'libvorbis', '-q:a', '4', ogg], check=True)
    os.remove(wav)


# ---- recipes ----

def rumble(dur, f0=60, f1=30):
    return env(lowpass(noise(dur), 180) * 1.5 + sweep(f0, f1, dur) * 0.6, 0.02, 0.2, 0.5, dur * 0.5)


def whoosh(dur, c0=400, c1=3000, rev=True):
    n = noise(dur)
    tt = t(dur)
    cut = c0 + (c1 - c0) * np.sin(np.pi * tt / dur)
    out = np.zeros_like(n)
    acc = 0.0
    for i in range(len(n)):
        alpha = 1.0 / (SR / (2 * np.pi * cut[i]) + 1.0)
        acc += alpha * (n[i] - acc)
        out[i] = acc
    out = highpass(out, 150)
    out = env(out, 0.05, 0.1, 0.8, dur * 0.4)
    return reverb(out, 0.2) if rev else out


def crackle(dur, density=0.02):
    x = np.zeros(int(SR * dur))
    for i in range(len(x)):
        if rng.random() < density:
            x[i] = rng.uniform(-1, 1)
    return lowpass(x, 5000)


def chime(freqs, dur, decay=0.5):
    out = np.zeros(int(SR * dur))
    for i, f in enumerate(freqs):
        s = sine(f, dur) * np.exp(-t(dur) / (decay * (1 + i * 0.3)))
        out += s / (i + 1)
    return out


RECIPES = {}


def recipe(name):
    def deco(fn):
        RECIPES[name] = fn
        return fn
    return deco


@recipe('awaken')
def _():
    return mix(chime([220, 330, 440, 660, 880], 2.5, 1.0), whoosh(2.0, 200, 2500) * 0.5, reverb(rumble(1.5, 80, 40) * 0.6, 0.4))


@recipe('learn')
def _():
    return mix(chime([523, 659, 784], 1.2, 0.4), whoosh(0.6, 800, 4000) * 0.3)


@recipe('point')
def _():
    return chime([880, 1174, 1760], 0.8, 0.25)


@recipe('scroll')
def _():
    return env(highpass(lowpass(noise(0.5), 3000), 400), 0.01, 0.1, 0.5, 0.3)


@recipe('rock_throw')
def _():
    return mix(rumble(0.5, 90, 50), whoosh(0.5, 300, 1500, False) * 0.6, crackle(0.3, 0.05) * 0.4)


@recipe('rock_hit')
def _():
    return mix(env(lowpass(noise(0.3), 800), 0.001, 0.05, 0.3, 0.2) * 1.2, crackle(0.25, 0.1), rumble(0.3, 70, 40) * 0.5)


@recipe('earth_rise')
def _():
    return mix(rumble(1.2, 50, 90), crackle(1.0, 0.03) * 0.6, env(lowpass(noise(1.2), 400), 0.1, 0.3, 0.6, 0.5))


@recipe('earth_armor')
def _():
    return mix(rumble(0.8, 70, 50), env(lowpass(noise(0.8), 600), 0.01, 0.2, 0.4, 0.4), crackle(0.8, 0.06) * 0.5)


@recipe('sand_hiss')
def _():
    return env(highpass(lowpass(noise(0.9), 6000), 1500), 0.05, 0.2, 0.6, 0.5)


@recipe('lava_flow')
def _():
    return mix(env(lowpass(noise(1.5), 300), 0.1, 0.3, 0.7, 0.6) * 1.2, crackle(1.5, 0.01) * 0.8, rumble(1.5, 45, 35) * 0.4)


@recipe('metal_clang')
def _():
    return mix(chime([1200, 1810, 2600, 3900], 0.9, 0.18), env(highpass(noise(0.1), 3000), 0.001, 0.03, 0.2, 0.05) * 0.5)


@recipe('metal_wreck')
def _():
    return mix(chime([600, 905, 1400], 0.8, 0.2), env(lowpass(noise(0.5), 1500), 0.001, 0.1, 0.4, 0.3), crackle(0.5, 0.08) * 0.6)


@recipe('seismic')
def _():
    return mix(env(sine(55, 0.6), 0.01, 0.2, 0.3, 0.3), env(sine(110, 0.3), 0.01, 0.1, 0.2, 0.15) * 0.4)


@recipe('air_blast')
def _():
    return whoosh(0.7, 300, 5000)


@recipe('air_shield')
def _():
    return mix(whoosh(1.0, 500, 2500) * 0.7, env(sine(330, 1.0) + sine(495, 1.0) * 0.5, 0.1, 0.3, 0.5, 0.5) * 0.3)


@recipe('air_shield_break')
def _():
    return mix(env(noise(0.4), 0.001, 0.1, 0.3, 0.3), whoosh(0.8, 2000, 300), chime([440, 660], 0.6, 0.15) * 0.4)


@recipe('air_blade')
def _():
    return highpass(whoosh(0.35, 2000, 7000, False), 1000)


@recipe('air_jump')
def _():
    return mix(whoosh(0.5, 300, 4000, False), env(sweep(200, 800, 0.4), 0.01, 0.1, 0.4, 0.2) * 0.3)


@recipe('air_scooter')
def _():
    return mix(env(lowpass(noise(0.6), 1200), 0.05, 0.2, 0.6, 0.3), env(sweep(150, 400, 0.6), 0.05, 0.2, 0.5, 0.3) * 0.3)


@recipe('tornado')
def _():
    tt = t(1.6)
    wob = lowpass(noise(1.6), 900) * (0.6 + 0.4 * np.sin(2 * np.pi * 6 * tt))
    return mix(env(wob, 0.1, 0.4, 0.7, 0.6) * 1.2, env(sweep(80, 160, 1.6), 0.1, 0.3, 0.5, 0.6) * 0.3)


@recipe('projection')
def _():
    return mix(chime([392, 587, 784, 1175], 2.0, 0.9), reverb(whoosh(1.5, 3000, 300) * 0.4, 0.5))


@recipe('suffocate')
def _():
    return mix(env(highpass(lowpass(noise(1.0), 2500), 800), 0.3, 0.3, 0.6, 0.4), env(sweep(400, 150, 1.0), 0.2, 0.3, 0.4, 0.4) * 0.3)


@recipe('water_whip')
def _():
    return mix(whoosh(0.4, 800, 4000, False), env(lowpass(noise(0.3), 2500), 0.001, 0.08, 0.3, 0.2), crackle(0.2, 0.2) * 0.3)


@recipe('water_gather')
def _():
    return mix(env(lowpass(noise(0.6), 1800), 0.05, 0.2, 0.5, 0.3), chime([660, 990], 0.5, 0.2) * 0.3)


@recipe('heal')
def _():
    return mix(chime([523, 784, 1046, 1568], 1.5, 0.6), env(lowpass(noise(1.0), 1500), 0.2, 0.3, 0.4, 0.5) * 0.3)


@recipe('octopus')
def _():
    return mix(env(lowpass(noise(0.9), 1200), 0.05, 0.3, 0.6, 0.4), env(sweep(120, 240, 0.9), 0.05, 0.2, 0.4, 0.4) * 0.3)


@recipe('spout')
def _():
    return mix(env(lowpass(noise(1.2), 2000), 0.1, 0.3, 0.8, 0.4) * 1.2, env(sweep(100, 300, 1.2), 0.1, 0.3, 0.6, 0.4) * 0.3)


@recipe('ice_crack')
def _():
    return mix(crackle(0.5, 0.15), chime([1500, 2300, 3100], 0.6, 0.12) * 0.6, env(highpass(noise(0.2), 2000), 0.001, 0.05, 0.2, 0.15))


@recipe('ice_knife')
def _():
    return mix(highpass(whoosh(0.3, 3000, 8000, False), 1500), chime([2600, 3900], 0.3, 0.08) * 0.5)


@recipe('blood_grip')
def _():
    return mix(env(sweep(90, 45, 1.2), 0.05, 0.3, 0.6, 0.5), env(lowpass(noise(1.2), 250), 0.05, 0.3, 0.5, 0.5) * 0.8, env(sine(180, 1.2) * (0.5 + 0.5 * np.sin(2 * np.pi * 7 * t(1.2))), 0.1, 0.3, 0.5, 0.5) * 0.3)


@recipe('fire_blast')
def _():
    return mix(whoosh(0.6, 400, 2500, False), crackle(0.6, 0.04) * 0.6, env(lowpass(noise(0.3), 700), 0.001, 0.1, 0.3, 0.2))


@recipe('fire_stream')
def _():
    return mix(env(lowpass(noise(1.0), 900), 0.05, 0.2, 0.8, 0.3), crackle(1.0, 0.03) * 0.7)


@recipe('fire_whip')
def _():
    return mix(env(highpass(noise(0.12), 1500), 0.001, 0.03, 0.2, 0.08) * 1.2, whoosh(0.4, 600, 3500, False) * 0.6, crackle(0.3, 0.05) * 0.4)


@recipe('fire_ring')
def _():
    return mix(whoosh(0.9, 300, 3000), crackle(0.9, 0.05) * 0.7, rumble(0.6, 80, 50) * 0.4)


@recipe('lightning_charge')
def _():
    tt = t(1.0)
    buzz = sine(110, 1.0) * sine(1100, 1.0) * (0.3 + 0.7 * tt)
    return mix(env(buzz, 0.05, 0.2, 0.9, 0.1), env(highpass(noise(1.0), 4000), 0.1, 0.2, 0.6, 0.1) * (0.2 + 0.8 * tt) * 0.5)


@recipe('lightning_strike')
def _():
    return mix(env(noise(0.15), 0.0005, 0.05, 0.2, 0.1) * 1.5, reverb(rumble(1.5, 120, 40), 0.5), crackle(0.4, 0.2) * 0.6)


@recipe('lightning_redirect')
def _():
    return mix(env(sine(220, 0.6) * sine(2200, 0.6), 0.01, 0.2, 0.5, 0.3), env(highpass(noise(0.5), 3000), 0.01, 0.1, 0.4, 0.3) * 0.5, chime([880, 1320], 0.6, 0.2) * 0.4)


@recipe('lemur_chirp')
def _():
    tt = t(0.35)
    chirp = sine(1800 + 900 * np.sin(2 * np.pi * 9 * tt), 0.35) * (0.6 + 0.4 * np.sin(2 * np.pi * 18 * tt))
    return mix(env(chirp, 0.01, 0.1, 0.5, 0.15), env(sweep(2400, 1400, 0.2), 0.01, 0.05, 0.4, 0.1) * 0.4)


@recipe('lemur_hurt')
def _():
    return mix(env(sweep(2200, 900, 0.3) * (0.7 + 0.3 * sine(40, 0.3)), 0.005, 0.1, 0.5, 0.15), env(highpass(noise(0.25), 3000), 0.005, 0.05, 0.3, 0.15) * 0.3)


def main():
    names = sys.argv[1:] or list(RECIPES)
    for name in names:
        write(name, RECIPES[name]())
        print('wrote', name)
    print(len(names), 'sounds')


if __name__ == '__main__':
    main()
