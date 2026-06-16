# Salt's Inventory Update

Salt's Inventory Update is a multi-version, multi-loader Minecraft mod project. It uses one Gradle project, shared common code, and a loader module per supported Minecraft version.

## Layout

```text
versions/
  common/              Shared Java used by every version and loader
  1.20.1/
    common/            Code/resources shared by Fabric and Forge on 1.20.1
    fabric/            Fabric entrypoint and fabric.mod.json
    forge/             Forge entrypoint and mods.toml
  1.21.1/
    common/            Code/resources shared by Fabric and NeoForge on 1.21.1
    fabric/
    neoforge/
  1.21.11/
    common/
    fabric/
    neoforge/
  26.1.2/
    common/
    fabric/
    neoforge/
```

The version and loader matrix is declared in [settings.gradle.kts](settings.gradle.kts). Dependency versions and mod metadata live in [build.gradle.kts](build.gradle.kts).

## Common Code Pattern

- Put loader-free Java in `versions/common`.
- Put Minecraft-version-specific shared code in `versions/<mc-version>/common`.
- Put Fabric, Forge, or NeoForge hooks in `versions/<mc-version>/<loader>`.

This keeps version API differences contained while still letting you share most of your mod logic.

## Useful Commands

```powershell
.\gradlew.bat projects
.\gradlew.bat tasks
.\gradlew.bat build
```

For publishing, uncomment the `publishing` block in [build.gradle.kts](build.gradle.kts), add the CurseForge and Modrinth project IDs, then set `CURSEFORGE_TOKEN` and `MODRINTH_TOKEN` in your environment.
