# SharedInv Reforged

Unofficial port of SharedInv for Forge and NeoForge. Original mod by Redstoned and MIT licensed.

Players in the same group share one inventory, hotbar, armor and offhand slot. Nobody needs the mod on the client. State is saved to `sharedinv.nbt` in the world folder.

This first release targets Minecraft 1.21.1 only. There are two jars:

- `sharedinv_reforged-1.21.1-forge-1.0.0.jar` for Forge 1.21.1
- `sharedinv_reforged-1.21.1-neoforge-1.0.0.jar` for NeoForge 1.21.1

Use the jar for your loader. Do not put both in the same mods folder.

## Commands

All commands need permission level 2 and start with `/sharedinv`.

- `help` links to the original mod page
- `group add <name>`
- `group remove <name>`
- `group list [name]`
- `group join <name> [player]`
- `group leave <player>`
- `group default set <name>`
- `group default get`
- `group default clear`

## Building

You need Java 21. The Gradle wrapper is pinned to Gradle 8.11 because ForgeGradle does not support Gradle 9.

```shell
./gradlew buildAll
```

The jars end up in `forge/build/libs` and `neoforge/build/libs`.

Shared code lives in `common/src`. Forge and NeoForge glue lives in their own folders. Both modules compile the shared sources with Mojang mappings.

## Credits

Original mod by Redstoned: https://github.com/red-stoned/sharedinv and https://modrinth.com/mod/sharedinv. Credit to Galiano and Kroojel. Port by T3ddz.
