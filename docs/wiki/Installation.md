# Installation

[עברית](Installation-HE)

1. Install **NeoForge 21.1** for **Minecraft 1.21.1** (built against NeoForge 21.1.256; any 21.1 from there on works), or, since 1.0.29, **NeoForge 21.4** for **Minecraft 1.21.4** (built against 21.4.161), or, since 1.0.31, **NeoForge 21.5** for **Minecraft 1.21.5** (built against 21.5.101), or, since 1.0.32, **NeoForge 21.11** for **Minecraft 1.21.11** (built against 21.11.48). It is the same jar for all four: it finds out which Minecraft it is in and starts the right copy of itself.
2. Put `enlightened-<version>.jar` (from the [Releases](../../releases) page, or [built from source](Building-from-Source)) in the `mods` folder.
3. For multiplayer, put the same jar on the server too: a client and a server with different versions of the mod refuse each other.

Nothing else is needed. The mod has no dependencies beyond NeoForge.

The server's options live in `world/serverconfig/enlightened-server.toml` once a world has been opened: see
[Server Configuration](Server-Configuration).
