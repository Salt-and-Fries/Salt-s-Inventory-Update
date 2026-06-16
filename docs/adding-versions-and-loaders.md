# Adding Versions And Loaders

## Add A Minecraft Version

1. Add a new `version("<minecraft-version>")` block in [../settings.gradle.kts](../settings.gradle.kts).
2. Add the matching dependency versions in [../build.gradle.kts](../build.gradle.kts).
3. Copy the closest `versions/<old-version>` folder to `versions/<new-version>`.
4. Update `VersionInfo.MINECRAFT_VERSION`.
5. Update `pack.mcmeta` to the correct pack format for that Minecraft version.
6. Fix imports where Minecraft, Forge, Fabric, or NeoForge APIs changed.

## Add A Loader To An Existing Version

1. Add `fabric()`, `forge()`, or `neoforge()` in that version's settings block.
2. Add the loader block and loader version in [../build.gradle.kts](../build.gradle.kts).
3. Add a loader folder under `versions/<minecraft-version>/<loader>`.
4. Add the loader entrypoint class and metadata file.

## Where To Put Dependencies

- Use `sharedCommon { dependencies { ... } }` for libraries shared by every target.
- Use `version("<mc>") { common { dependencies { ... } } }` for version-specific shared dependencies.
- Use `fabric { dependencies { ... } }`, `forge { ... }`, or `neoforge { ... }` for loader-specific dependencies.

