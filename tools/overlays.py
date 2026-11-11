#!/usr/bin/env python3
"""The resources of the Minecraft versions after 1.21.1 (docs/MULTIVERSION.md), as pack overlays: folders that
pack.mcmeta switches on by pack format, on top of the 1.21.1 resources the jar holds. Run from the repository root;
it rewrites src/main/resources/pack.mcmeta and the overlay folders.

1.21.4 (data format 57-61, resource format 42-46):
* data: recipes with ingredients written as ids ("minecraft:stick", "#tag"); biomes with carvers as a list and
  their music as a weighted list;
* assets: an item model definition (assets/<ns>/items/<item>.json) for every item, the spawn eggs' colours and the
  glider's dye as their tints.
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


def main():
    for d in (DATA_4, ASSETS_4):
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

    write(os.path.join(RES, "pack.mcmeta"), {
        "overlays": {"entries": [
            {"formats": {"min_inclusive": 57, "max_inclusive": 61}, "directory": DATA_4},
            {"formats": {"min_inclusive": 42, "max_inclusive": 46}, "directory": ASSETS_4},
        ]}
    })
    print("overlays written; spawn eggs:", sorted(eggs))


if __name__ == "__main__":
    main()
