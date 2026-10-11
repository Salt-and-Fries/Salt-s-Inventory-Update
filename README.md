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
  26.2/
    common/
    fabric/
    neoforge/
  26.3/
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

For Minecraft 26.3 NeoForge, set `CI=true` before invoking Gradle so ModDevGradle uses its supported binary patch pipeline; the published 26.3 NeoForm source currently fails recompilation at `HolderSet.emptyNamed`. In PowerShell, run `$env:CI = 'true'; .\gradlew.bat :26.3:neoforge:build` for that target, or `$env:CI = 'true'; .\gradlew.bat build` for the full release.

After a full build, the twelve verified Fabric/Forge/NeoForge jars and `SHA256SUMS.txt` are collected in `build/release/0.1.4`. The versioned directory is synchronized from scratch so stale artifacts cannot enter a release.

For publishing, uncomment the `publishing` block in [build.gradle.kts](build.gradle.kts), add the CurseForge and Modrinth project IDs, then set `CURSEFORGE_TOKEN` and `MODRINTH_TOKEN` in your environment.

## Public API for 0.1.4

The [API guide](docs/markdown/desktop_window_api.md) covers optional providers, menu and client-only windows, native UI reuse, shared real-inventory companions, original-surface management controls and session-aware actions. [Compilable examples](api-examples/README.md) include a standalone nine-slot companion consumer. The public API is version 5 and desktop wire protocol is version 4; previous public API entrypoints were removed.

`./gradlew verifyApiExamples` compiles the examples against all twelve loader/version targets. `build` also verifies these examples and checks that every release jar contains the redesigned API.
