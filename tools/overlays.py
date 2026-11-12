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

The 1.21.4 and 1.21.5 overlays also apply on 1.21.11 (data format 94.1, resource format 75.0): the recipes, item model
definitions and spawn eggs are still in those formats. On top of them, for 1.21.11 only:
* data: noise settings with a preliminary surface level; biomes with their sky, fog, water fog, particles, sounds and music as environment attributes; dimension types in
  1.21.11's form (the skybox, the bed and respawn anchor rules and the raids as attributes, a fixed time as a timeline of
  the day's values at that time: data/enlightened/timeline/); tags with the blocks and items 1.21.9 renamed (chain is
  iron_chain).

From 1.21.9 the overlay entries carry min_format and max_format as well as the old formats range: 1.21.11 reads the
same entries for data (last old format 81) and for resources (last old format 64), and an entry that reaches past one
of those needs the new fields, equal to the old range.
"""
import copy
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
DATA_11 = "overlay_1_21_11_data"
# what a dimension's special effects did on 1.21.1 (client.cosmic.CosmicEffects), as timeline values that override the
# day's: the cosmic plane's haze keeps its purple at night, and it has no clouds
SKY_11 = {"enlightened:cosmic": {"minecraft:visual/fog_color": "#210d3d", "minecraft:visual/cloud_color": "#00000000"}}
# ids 1.21.9 and later renamed, as tags name them
RENAMED_11 = {"minecraft:chain": "minecraft:iron_chain"}


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


def hexcolour(value):
    return value if isinstance(value, str) else "#%06x" % (value & 0xFFFFFF)


def biome_11(obj):
    """A biome in 1.21.4's form (carvers as a list, music as a weighted list) in 1.21.11's: its sky, fog, water fog,
    particles, sounds and music are environment attributes; the colours left in its effects are written in hex."""
    obj = copy.deepcopy(obj)
    effects = obj.get("effects", {})
    attributes = obj.get("attributes", {})
    for key, attribute in (("sky_color", "sky_color"), ("fog_color", "fog_color"), ("water_fog_color", "water_fog_color")):
        if key in effects:
            attributes["minecraft:visual/" + attribute] = hexcolour(effects.pop(key))
    for key in ("water_color", "grass_color", "foliage_color", "dry_foliage_color"):
        if key in effects:
            effects[key] = hexcolour(effects[key])
    particle = effects.pop("particle", None)
    if particle:
        attributes["minecraft:visual/ambient_particles"] = [{"particle": particle["options"], "probability": particle["probability"]}]
    sounds = {}
    if "ambient_sound" in effects:
        sounds["loop"] = effects.pop("ambient_sound")
    if "mood_sound" in effects:
        sounds["mood"] = effects.pop("mood_sound")
    if "additions_sound" in effects:
        sounds["additions"] = effects.pop("additions_sound")
    if sounds:
        attributes["minecraft:audio/ambient_sounds"] = sounds
    music = effects.pop("music", None)
    if isinstance(music, list):
        music = music[0]["data"] if music else None
    if music:
        music = {k: v for k, v in music.items() if k != "replace_current_music" or v}
        attributes["minecraft:audio/background_music"] = {"default": music}
    effects.pop("music_volume", None)
    obj["effects"] = effects
    if attributes:
        obj["attributes"] = attributes
    return obj


def colour_channels(value):
    if isinstance(value, str):
        h = value[1:]
        v = int(h, 16)
        return [(v >> 24) & 255 if len(h) == 8 else None, (v >> 16) & 255, (v >> 8) & 255, v & 255]
    v = value & 0xFFFFFFFF
    return [(v >> 24) & 255, (v >> 16) & 255, (v >> 8) & 255, v & 255]


def interpolate(a, b, f):
    if isinstance(a, bool) or isinstance(b, bool):
        return a
    if isinstance(a, str) or (isinstance(a, int) and abs(a) > 0xFFFFFF) or (isinstance(a, int) and a < 0):
        ca, cb = colour_channels(a), colour_channels(b)
        out = [None if x is None else round(x + (y - x) * f) for x, y in zip(ca, cb)]
        if isinstance(a, str):
            return ("#%02x" % out[0] if out[0] is not None else "#") + "%02x%02x%02x" % tuple(out[1:])
        v = (out[0] << 24) | (out[1] << 16) | (out[2] << 8) | out[3]
        return v - (1 << 32) if v >= 1 << 31 else v
    return a + (b - a) * f


def track_at(track, ticks, period):
    """A timeline track's value at a time of day (the keyframes repeat every period; two keyframes on one tick jump)."""
    frames = track["keyframes"]
    before = [k for k in frames if k["ticks"] <= ticks]
    prev = before[-1] if before else dict(frames[-1], ticks=frames[-1]["ticks"] - period)
    after = [k for k in frames if k["ticks"] > ticks]
    nxt = after[0] if after else dict(frames[0], ticks=frames[0]["ticks"] + period)
    if track.get("ease") == "constant" or nxt["ticks"] == prev["ticks"]:
        return prev["value"]
    return interpolate(prev["value"], nxt["value"], (ticks - prev["ticks"]) / (nxt["ticks"] - prev["ticks"]))


def game_json_11(path):
    """A file of 1.21.11's own data (its day timeline, its overworld noise), from the game's resources in the build."""
    import glob
    import zipfile
    for jar in glob.glob(os.path.join(ROOT, "versions/1.21.11/build/moddev/artifacts/*client-extra*.jar")):
        with zipfile.ZipFile(jar) as z:
            return json.loads(z.read(path))
    raise SystemExit("run ./gradlew :v1_21_11:compileJava first: 1.21.11's " + path + " is read from its resources")


def day_timeline():
    return game_json_11("data/minecraft/timeline/day.json")


def noise_settings_11(obj, overworld):
    """Noise settings in 1.21.11's form: the router's initial density without jaggedness became the preliminary surface
    level, found from the top as the game's overworld does (its upper bound is the overworld's, as the router's terrain)."""
    obj = copy.deepcopy(obj)
    router = obj["noise_router"]
    initial = router.pop("initial_density_without_jaggedness", None)
    if initial is not None and "preliminary_surface_level" not in router:
        top = overworld["noise_router"]["preliminary_surface_level"]
        router["preliminary_surface_level"] = {
            "type": "minecraft:find_top_surface",
            "density": {"type": "minecraft:add", "argument1": top["density"]["argument1"], "argument2": initial},
            "upper_bound": top["upper_bound"],
            "lower_bound": obj["noise"]["min_y"],
            "cell_height": obj["noise"]["size_vertical"] * 4,
        }
    return obj


def dimension_type_11(obj, name, day):
    """A dimension type in 1.21.11's form, and the timeline that holds its fixed time (or None)."""
    out = {k: obj[k] for k in ("has_skylight", "has_ceiling", "coordinate_scale", "min_y", "height", "logical_height",
                               "infiniburn", "ambient_light", "monster_spawn_light_level", "monster_spawn_block_light_limit")}
    out["skybox"] = {"minecraft:the_end": "end", "minecraft:the_nether": "none"}.get(obj.get("effects"), "overworld")
    attributes = {}
    if not obj.get("bed_works", True):
        attributes["minecraft:gameplay/bed_rule"] = {"can_set_spawn": "never", "can_sleep": "never", "explodes": True}
    attributes["minecraft:gameplay/respawn_anchor_works"] = bool(obj.get("respawn_anchor_works", False))
    if not obj.get("has_raids", True):
        attributes["minecraft:gameplay/can_start_raid"] = False
    if obj.get("piglin_safe", False):
        attributes["minecraft:gameplay/piglins_zombify"] = False
    if obj.get("ultrawarm", False):
        attributes["minecraft:gameplay/water_evaporates"] = True
        attributes["minecraft:gameplay/fast_lava"] = True
    timeline = None
    if "fixed_time" in obj:
        # the day's values at that time, for ever (1.21.1's fixed_time)
        out["has_fixed_time"] = True
        period = day.get("period_ticks", 24000)
        tracks = {}
        for key, track in day["tracks"].items():
            entry = {"keyframes": [{"ticks": 0, "value": track_at(track, obj["fixed_time"] % period, period)}]}
            if "modifier" in track:
                entry["modifier"] = track["modifier"]
            tracks[key] = entry
        for key, value in SKY_11.get(obj.get("effects"), {}).items():
            tracks[key] = {"keyframes": [{"ticks": 0, "value": value}]}
        timeline = {"period_ticks": period, "tracks": tracks}
        out["timelines"] = [f"{NS}:fixed_{name}"]
    out["attributes"] = attributes
    return out, timeline


def rename_ids(value):
    if isinstance(value, str):
        return RENAMED_11.get(value, value)
    if isinstance(value, list):
        return [rename_ids(v) for v in value]
    if isinstance(value, dict):
        return {k: rename_ids(v) for k, v in value.items()}
    return value


def main():
    for d in (DATA_4, ASSETS_4, DATA_5, ASSETS_5, DATA_11):
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

    # 1.21.11: biomes and dimension types in its form, tags with its renamed ids
    for name in sorted(os.listdir(os.path.join(data, "worldgen", "biome"))):
        obj = json.load(open(os.path.join(RES, DATA_4, "data", NS, "worldgen", "biome", name), encoding="utf-8"))
        write(os.path.join(RES, DATA_11, "data", NS, "worldgen", "biome", name), biome_11(obj))
    overworld = game_json_11("data/minecraft/worldgen/noise_settings/overworld.json")
    for name in sorted(os.listdir(os.path.join(data, "worldgen", "noise_settings"))):
        obj = json.load(open(os.path.join(data, "worldgen", "noise_settings", name), encoding="utf-8"))
        write(os.path.join(RES, DATA_11, "data", NS, "worldgen", "noise_settings", name), noise_settings_11(obj, overworld))
    day = day_timeline()
    for name in sorted(os.listdir(os.path.join(data, "dimension_type"))):
        obj = json.load(open(os.path.join(data, "dimension_type", name), encoding="utf-8"))
        dim, timeline = dimension_type_11(obj, name[:-5], day)
        write(os.path.join(RES, DATA_11, "data", NS, "dimension_type", name), dim)
        if timeline:
            write(os.path.join(RES, DATA_11, "data", NS, "timeline", "fixed_" + name), timeline)
    for ns in sorted(os.listdir(os.path.join(RES, "data"))):
        tags = os.path.join(RES, "data", ns, "tags")
        for root, _, files in os.walk(tags):
            for name in sorted(files):
                path = os.path.join(root, name)
                obj = json.load(open(path, encoding="utf-8"))
                if rename_ids(obj) != obj:
                    write(os.path.join(RES, DATA_11, os.path.relpath(path, RES)), rename_ids(obj))

    # (later entries win; each range reaches over the formats of the versions it serves: 1.21.4 is data 57-61 and
    # resources 42-46, 1.21.5 data 71 and resources 55, 1.21.11 data 94.1 and resources 75.0; min_format and
    # max_format repeat the range for 1.21.9 and later)
    def entry(low, high, directory):
        return {"formats": {"min_inclusive": low, "max_inclusive": high}, "min_format": low, "max_format": high, "directory": directory}

    write(os.path.join(RES, "pack.mcmeta"), {
        "overlays": {"entries": [
            entry(57, 94, DATA_4),
            entry(42, 75, ASSETS_4),
            entry(62, 94, DATA_5),
            entry(47, 75, ASSETS_5),
            entry(94, 94, DATA_11),
        ]}
    })
    print("overlays written; spawn eggs:", sorted(eggs))


if __name__ == "__main__":
    main()
