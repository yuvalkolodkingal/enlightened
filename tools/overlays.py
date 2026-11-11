#!/usr/bin/env python3
"""The resources of the Minecraft versions after 1.21.1 (docs/MULTIVERSION.md), as pack overlays: folders that
pack.mcmeta switches on by pack format, on top of the 1.21.1 resources the jar holds. Run from the repository root;
it rewrites src/main/resources/pack.mcmeta and the overlay folders.

1.21.4 (data format 57-61, resource format 42-46):
* data: recipes with ingredients written as ids ("minecraft:stick", "#tag"); biomes with carvers as a list and
  their music as a weighted list;
* assets: an item model definition (assets/<ns>/items/<item>.json) for every item, the spawn eggs' colours and the
  glider's dye as their tints.

The 1.21.4 overlays also apply on 1.21.5 (data format 71, resource format 55), whose formats they still are; the
ranges reach over 1.21.5's. On top of them, for 1.21.5 only:
* data: the trim recipe names its pattern (a trim pattern no longer names its template item);
* assets: the spawn eggs have a texture each (1.21.5 removed the template egg they were tinted from), drawn here in
  their two colours, and an item model for it.
"""
import json
import os
import re
import shutil

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src/main/resources")
NS = "enlightened"
DATA_4 = "overlay_1_21_4_data"
ASSETS_4 = "overlay_1_21_4_assets"
DATA_5 = "overlay_1_21_5_data"
ASSETS_5 = "overlay_1_21_5_assets"


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def ingredient(value):
    """1.21.1's {"item": x} / {"tag": t} (or a list of them) as 1.21.2's "x" / "#t"."""
    if isinstance(value, list):
        return [ingredient(v) for v in value]
    if isinstance(value, dict):
        if "item" in value:
            return value["item"]
        if "tag" in value:
            return "#" + value["tag"]
    return value


def recipe(obj):
    obj = dict(obj)
    if "key" in obj:
        obj["key"] = {k: ingredient(v) for k, v in obj["key"].items()}
    for field in ("ingredient", "ingredients", "base", "addition", "template"):
        if field in obj:
            obj[field] = ingredient(obj[field])
    return obj


def colours():
    """The spawn eggs' two colours, from ModItems.java: egg("name", ModEntities.X, 0xBASE, 0xSPOTS)."""
    src = open(os.path.join(ROOT, "src/main/java/com/kolodkingal/enlightened/registry/ModItems.java"), encoding="utf-8").read()
    out = {}
    for name, a, b in re.findall(r'egg\("(\w+)", ModEntities\.\w+, (0x[0-9A-Fa-f]+), (0x[0-9A-Fa-f]+)\)', src):
        out[name] = (int(a, 16), int(b, 16))
    return out


def glider_default():
    src = open(os.path.join(ROOT, "src/main/java/com/kolodkingal/enlightened/item/GliderItem.java"), encoding="utf-8").read()
    return int(re.search(r"DEFAULT_COLOR = (0x[0-9A-Fa-f]+)", src).group(1), 16)


def signed(rgb):
    v = 0xFF000000 | rgb
    return v - (1 << 32)


def egg_texture(path, base, spots):
    """A 16x16 spawn egg in two colours: an egg outlined in a darker base, lit at the upper left, with spots."""
    from PIL import Image

    def shade(rgb, f):
        return tuple(max(0, min(255, int(c * f))) for c in ((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255)) + (255,)

    inside = set()
    for y in range(16):
        ny = (y + 0.5 - 8.5) / 7.0
        if abs(ny) >= 1.0:
            continue
        w = 5.0 * (1.0 - ny * ny) ** 0.5 * (1.0 + 0.15 * ny)
        for x in range(16):
            if abs(x + 0.5 - 8.0) <= w:
                inside.add((x, y))
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for (x, y) in inside:
        edge = any((x + dx, y + dy) not in inside for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if edge:
            colour = shade(base, 0.55)
        elif x <= 6 and y <= 6 and (x, y) != (6, 6):
            colour = shade(base, 1.18)
        elif x >= 10 or y >= 13:
            colour = shade(base, 0.85)
        else:
            colour = shade(base, 1.0)
        img.putpixel((x, y), colour)
    for (x, y) in ((8, 4), (9, 4), (5, 7), (6, 7), (10, 7), (8, 9), (8, 10), (9, 10), (5, 11), (11, 11), (7, 13), (8, 13)):
        if (x, y) in inside and all((x + dx, y + dy) in inside for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            img.putpixel((x, y), shade(spots, 1.0))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def main():
    for d in (DATA_4, ASSETS_4, DATA_5, ASSETS_5):
        shutil.rmtree(os.path.join(RES, d), ignore_errors=True)

    data = os.path.join(RES, "data", NS)
    for name in sorted(os.listdir(os.path.join(data, "recipe"))):
        obj = json.load(open(os.path.join(data, "recipe", name), encoding="utf-8"))
        write(os.path.join(RES, DATA_4, "data", NS, "recipe", name), recipe(obj))
    for name in sorted(os.listdir(os.path.join(data, "worldgen", "biome"))):
        obj = json.load(open(os.path.join(data, "worldgen", "biome", name), encoding="utf-8"))
        carvers = obj.get("carvers")
        if isinstance(carvers, dict):
            obj["carvers"] = [c for step in carvers.values() for c in (step if isinstance(step, list) else [step])]
        music = obj.get("effects", {}).get("music")
        if isinstance(music, dict):
            obj["effects"]["music"] = [{"data": music, "weight": 1}]
        write(os.path.join(RES, DATA_4, "data", NS, "worldgen", "biome", name), obj)

    eggs = colours()
    models = os.path.join(RES, "assets", NS, "models", "item")
    for name in sorted(os.listdir(models)):
        item = name[:-5]
        model = {"type": "minecraft:model", "model": f"{NS}:item/{item}"}
        if item in eggs:
            model = {"type": "minecraft:model", "model": "minecraft:item/template_spawn_egg",
                     "tints": [{"type": "minecraft:constant", "value": signed(eggs[item][0])},
                               {"type": "minecraft:constant", "value": signed(eggs[item][1])}]}
        elif item == "air_glider":
            model["tints"] = [{"type": "minecraft:constant", "value": -1}, {"type": "minecraft:dye", "default": signed(glider_default())}]
        write(os.path.join(RES, ASSETS_4, "assets", NS, "items", name), {"model": model})

    # 1.21.5: the trim recipe names its pattern; the eggs get a texture of their own
    for name in sorted(os.listdir(os.path.join(data, "recipe"))):
        obj = json.load(open(os.path.join(RES, DATA_4, "data", NS, "recipe", name), encoding="utf-8"))
        if obj.get("type") == "minecraft:smithing_trim":
            template = obj["template"]
            obj["pattern"] = template.replace("_armor_trim_smithing_template", "")
            write(os.path.join(RES, DATA_5, "data", NS, "recipe", name), obj)
    for item, (base, spots) in sorted(eggs.items()):
        egg_texture(os.path.join(RES, ASSETS_5, "assets", NS, "textures", "item", item + ".png"), base, spots)
        write(os.path.join(RES, ASSETS_5, "assets", NS, "models", "item", item + ".json"),
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{item}"}})
        write(os.path.join(RES, ASSETS_5, "assets", NS, "items", item + ".json"),
              {"model": {"type": "minecraft:model", "model": f"{NS}:item/{item}"}})

    # (later entries win; each range reaches over the formats of the versions it serves: 1.21.4 is data 57-61 and
    # resources 42-46, 1.21.5 data 71 and resources 55)
    write(os.path.join(RES, "pack.mcmeta"), {
        "overlays": {"entries": [
            {"formats": {"min_inclusive": 57, "max_inclusive": 71}, "directory": DATA_4},
            {"formats": {"min_inclusive": 42, "max_inclusive": 55}, "directory": ASSETS_4},
            {"formats": {"min_inclusive": 62, "max_inclusive": 71}, "directory": DATA_5},
            {"formats": {"min_inclusive": 47, "max_inclusive": 55}, "directory": ASSETS_5},
        ]}
    })
    print("overlays written; spawn eggs:", sorted(eggs))


if __name__ == "__main__":
    main()
