# Building from Source

[עברית](Building-from-Source-HE)

Java 21 and Git; the first build downloads NeoForge and Minecraft (a few minutes).

```
git clone https://github.com/yuvalkolodkingal/enlightened.git
cd enlightened
./gradlew build              # the jar: build/libs/enlightened-<version>.jar
./gradlew runGameTestServer  # the GameTests; the server exits with an error if one fails
./gradlew runClient          # a development client
./gradlew runServer          # a development server
```

The skins, the armour shells and the item icons are drawn by `python3 tools/skins.py` (Pillow), the 37 sounds by
`python3 tools/sounds.py` (numpy and ffmpeg): both write straight into `src/main/resources`, and both are
deterministic, so running them again changes nothing unless a palette or a recipe changed.

GitHub Actions builds the mod on every push, runs the GameTests and publishes the jar as a release
(`.github/workflows/build.yml`), and publishes this wiki from `docs/wiki` (`wiki.yml`).

The full design (every number, how it is built) is in [docs/DESIGN.md](../blob/main/docs/DESIGN.md).
